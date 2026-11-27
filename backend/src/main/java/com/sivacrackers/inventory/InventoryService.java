package com.sivacrackers.inventory;

import com.sivacrackers.audit.AuditService;
import com.sivacrackers.catalog.product.Product;
import com.sivacrackers.catalog.product.ProductMapper;
import com.sivacrackers.catalog.product.ProductRepository;
import com.sivacrackers.catalog.product.admin.AdminProductQuery;
import com.sivacrackers.catalog.product.admin.AdminProductSpecifications;
import com.sivacrackers.common.dto.PageResponse;
import com.sivacrackers.common.exception.ConflictException;
import com.sivacrackers.common.exception.InvalidRequestException;
import com.sivacrackers.common.exception.NotFoundException;
import com.sivacrackers.inventory.InventoryDtos.Alerts;
import com.sivacrackers.inventory.InventoryDtos.MovementDto;
import com.sivacrackers.inventory.InventoryDtos.MovementRequest;
import com.sivacrackers.inventory.InventoryDtos.StockRow;
import com.sivacrackers.security.CurrentUser;
import com.sivacrackers.supplier.Supplier;
import com.sivacrackers.supplier.SupplierRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every change to a product's stock goes through here and is recorded as a {@link StockMovement}.
 * The product row is locked for the duration of the change and stock can never go below zero
 * (also enforced by the database).
 */
@Service
public class InventoryService {

    private final ProductRepository products;
    private final StockMovementRepository movements;
    private final SupplierRepository suppliers;
    private final AuditService audit;
    private final Clock clock;

    public InventoryService(ProductRepository products, StockMovementRepository movements, SupplierRepository suppliers,
                            AuditService audit, Clock clock) {
        this.products = products;
        this.movements = movements;
        this.suppliers = suppliers;
        this.audit = audit;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ reading

    /** Current stock of every product (any status), with the admin product filters. */
    @Transactional(readOnly = true)
    public PageResponse<StockRow> stock(AdminProductQuery q) {
        var page = products.findAll(AdminProductSpecifications.matching(q), PageRequest.of(q.page(), q.size(), q.sort().sort()));
        var ids = page.getContent().stream().map(Product::getId).toList();
        java.util.Map<Long, StockMovement> latest = ids.isEmpty() ? java.util.Map.of()
                : movements.findLatestForProducts(ids).stream()
                        .collect(java.util.stream.Collectors.toMap(StockMovement::getProductId, m -> m));
        return PageResponse.of(page, p -> row(p, latest.get(p.getId())));
    }

    @Transactional(readOnly = true)
    public StockRow stockOf(Long productId) {
        return rowWithLast(products.findWithParentsById(productId).orElseThrow(() -> new NotFoundException("Product not found.")));
    }

    /** One stock row including when stock last changed and who changed it. */
    private StockRow rowWithLast(Product p) {
        return row(p, movements.findLatestForProducts(List.of(p.getId())).stream().findFirst().orElse(null));
    }

    @Transactional(readOnly = true)
    public Alerts alerts() {
        List<StockRow> low = products.findActiveLowStock().stream().map(InventoryService::row).toList();
        List<StockRow> out = products.findActiveOutOfStock().stream().map(InventoryService::row).toList();
        return new Alerts(low.size(), out.size(), low, out);
    }

    @Transactional(readOnly = true)
    public PageResponse<MovementDto> movements(Specification<StockMovement> filter, int page, int size) {
        var result = movements.findAll(filter, PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return PageResponse.of(result, MovementDto::of);
    }

    // ------------------------------------------------------------------ changing stock

    /** Stock in, stock out or adjustment requested by a staff member. */
    @Transactional
    public MovementDto record(MovementRequest req) {
        Product p = products.findForUpdateById(req.productId())
                .orElseThrow(() -> new InvalidRequestException("productId", "is not a valid product"));
        MovementReason reason = req.reason();
        if (reason.isAutomatic()) {
            throw new InvalidRequestException("reason", "is set automatically (product form or bills)");
        }
        int qty = req.quantity();
        int previous = p.getStockQuantity();
        int change = switch (reason.type()) {
            case STOCK_IN -> {
                if (qty <= 0) {
                    throw new InvalidRequestException("quantity", "must be at least 1");
                }
                yield qty;
            }
            case STOCK_OUT -> {
                if (qty <= 0) {
                    throw new InvalidRequestException("quantity", "must be at least 1");
                }
                if (qty > previous) {
                    throw new ConflictException("quantity", "is more than the " + previous + " in stock (stock cannot go below 0)");
                }
                yield -qty;
            }
            case ADJUSTMENT -> {
                if (qty == previous) {
                    throw new InvalidRequestException("quantity", "is the same as the current stock – nothing to adjust");
                }
                yield qty - previous;
            }
        };
        Supplier supplier = null;
        if (req.supplierId() != null) {
            if (reason.type() != MovementType.STOCK_IN && reason != MovementReason.RETURNED_TO_SUPPLIER) {
                throw new InvalidRequestException("supplierId", "can only be given for stock received or returned to a supplier");
            }
            supplier = suppliers.findById(req.supplierId())
                    .orElseThrow(() -> new InvalidRequestException("supplierId", "is not a valid supplier"));
            if (!supplier.isActive() && reason.type() == MovementType.STOCK_IN) {
                throw new InvalidRequestException("supplierId", "is inactive");
            }
        }
        if (req.unitCost() != null && reason != MovementReason.PURCHASE) {
            throw new InvalidRequestException("unitCost", "can only be given for stock received from a supplier");
        }
        return MovementDto.of(apply(p, reason, previous, change, supplier, req.unitCost(), blankToNull(req.note())));
    }

    /**
     * Called by the product form (same transaction) when the stock quantity typed there differs from the
     * current one, so even those changes appear in the history.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordFormChange(Product p, int previous, int newQuantity, boolean newProduct) {
        if (newQuantity == previous) {
            return;
        }
        apply(p, newProduct ? MovementReason.INITIAL_STOCK : MovementReason.PRODUCT_EDIT, previous, newQuantity - previous,
                null, null, null);
    }

    /**
     * Stock leaving with a bill (negative {@code change}) or coming back when a bill is voided (positive).
     * The caller has already locked the product row and checked availability.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordBill(Product p, int change, Long billId, String note) {
        MovementReason reason = change < 0 ? MovementReason.BILL_SALE : MovementReason.BILL_VOID;
        int previous = p.getStockQuantity();
        int next = previous + change;
        if (next < 0) {
            throw new ConflictException("items", "not enough stock of " + p.getName());
        }
        p.setStockQuantity(next);
        products.saveAndFlush(p);
        movements.saveAndFlush(new StockMovement(p.getId(), p.getName(), p.getSku(), reason, previous, change, null, null, note,
                CurrentUser.idOrNull(), CurrentUser.nameOrNull(), clock.instant(), billId));
    }

    /** Locks a product row before the product form changes its stock. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(Long productId) {
        products.findForUpdateById(productId);
    }

    /** Changes the "few left" level (low-stock threshold) of one product. */
    @Transactional
    public StockRow setThreshold(Long productId, int reorderLevel) {
        Product p = products.findForUpdateById(productId).orElseThrow(() -> new NotFoundException("Product not found."));
        int before = p.getReorderLevel();
        if (before != reorderLevel) {
            p.setReorderLevel(reorderLevel);
            products.saveAndFlush(p);
            audit.record("LOW_STOCK_LEVEL_CHANGED", "PRODUCT", p.getId(), p.getName() + ": " + before + " → " + reorderLevel);
        }
        return rowWithLast(products.findWithParentsById(productId).orElseThrow());
    }

    private StockMovement apply(Product p, MovementReason reason, int previous, int change, Supplier supplier,
                                java.math.BigDecimal unitCost, String note) {
        int next = previous + change;
        if (next < 0) {
            throw new ConflictException("quantity", "would make the stock negative");
        }
        p.setStockQuantity(next);
        products.saveAndFlush(p);
        return movements.saveAndFlush(new StockMovement(p.getId(), p.getName(), p.getSku(), reason, previous, change, supplier,
                unitCost, note, CurrentUser.idOrNull(), CurrentUser.nameOrNull(), clock.instant()));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static StockRow row(Product p) {
        return row(p, null);
    }

    static StockRow row(Product p, StockMovement last) {
        String image = p.getImages().isEmpty() ? null : p.getImages().getFirst().getUrl();
        return new StockRow(p.getId(), p.getName(), p.getSku(), ProductMapper.ref(p.getCategory()), ProductMapper.ref(p.getBrand()),
                p.getStockQuantity(), p.getReorderLevel(), p.getStockStatus(), p.isActive(), p.isVisibleOnWebsite(), image,
                p.getPrice(), p.getPackSize(), p.getUnit().name(), last == null ? null : last.getCreatedAt(),
                last == null ? null : last.getPerformedByName());
    }
}
