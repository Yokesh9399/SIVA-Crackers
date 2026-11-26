package com.sivacrackers.billing;

import com.sivacrackers.audit.AuditService;
import com.sivacrackers.billing.BillingDtos.BillDto;
import com.sivacrackers.billing.BillingDtos.BillPage;
import com.sivacrackers.billing.BillingDtos.BillRequest;
import com.sivacrackers.billing.BillingDtos.BillRow;
import com.sivacrackers.billing.BillingDtos.DiscountType;
import com.sivacrackers.billing.BillingDtos.LineRequest;
import com.sivacrackers.billing.BillingDtos.PaymentMethodDto;
import com.sivacrackers.billing.BillingDtos.PaymentMethodRequest;
import com.sivacrackers.catalog.product.Product;
import com.sivacrackers.catalog.product.ProductRepository;
import com.sivacrackers.catalog.product.ProductSpecifications;
import com.sivacrackers.common.ShopTime;
import com.sivacrackers.common.dto.PageResponse;
import com.sivacrackers.common.exception.ConflictException;
import com.sivacrackers.common.exception.InvalidRequestException;
import com.sivacrackers.common.exception.NotFoundException;
import com.sivacrackers.customer.ShopCustomer;
import com.sivacrackers.customer.ShopCustomerRepository;
import com.sivacrackers.inventory.InventoryService;
import com.sivacrackers.security.CurrentUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Internal in-store billing. A bill is finalized in one transaction: product rows are locked (always in the
 * same order, so two counters cannot deadlock), stock is checked, the bill and its lines are saved, a stock
 * movement is written for every line and the stock goes down – all or nothing. The same {@code requestKey}
 * can never bill twice.
 */
@Service
public class BillingService {

    /** Largest discount allowed on a bill (decided 26 Nov 2026). */
    public static final BigDecimal MAX_DISCOUNT_RATE = new BigDecimal("0.20");

    public record Filter(ShopTime.Range range, String search, String paymentMethod, Bill.PaymentStatus paymentStatus,
                         Bill.Status status, Long customerId) {
    }

    private final BillRepository bills;
    private final PaymentMethodRepository methods;
    private final ProductRepository products;
    private final ShopCustomerRepository customers;
    private final InventoryService inventory;
    private final BillNumberGenerator numbers;
    private final AuditService audit;
    private final EntityManager em;
    private final Clock clock;

    public BillingService(BillRepository bills, PaymentMethodRepository methods, ProductRepository products,
                          ShopCustomerRepository customers, InventoryService inventory, BillNumberGenerator numbers,
                          AuditService audit, EntityManager em, Clock clock) {
        this.bills = bills;
        this.methods = methods;
        this.products = products;
        this.customers = customers;
        this.inventory = inventory;
        this.numbers = numbers;
        this.audit = audit;
        this.em = em;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ bills

    @Transactional
    public BillDto create(BillRequest req) {
        var existing = bills.findByRequestKey(req.requestKey());
        if (existing.isPresent()) {
            return BillDto.of(existing.get(), true); // a repeated submit: nothing is billed or deducted again
        }

        // Merge repeated products and lock their rows in id order.
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (LineRequest l : req.items()) {
            quantities.merge(l.productId(), l.quantity(), Integer::sum);
        }
        List<Long> lockOrder = quantities.keySet().stream().sorted().toList();
        Map<Long, Product> locked = new LinkedHashMap<>();
        for (Long id : lockOrder) {
            Product p = products.findForUpdateById(id)
                    .orElseThrow(() -> new InvalidRequestException("items", "contains a product that does not exist"));
            if (!p.isActive()) {
                throw new InvalidRequestException("items", p.getName() + " is inactive and cannot be billed");
            }
            int wanted = quantities.get(id);
            if (wanted > p.getStockQuantity()) {
                throw new ConflictException("items", "only " + p.getStockQuantity() + " of " + p.getName() + " in stock");
            }
            locked.put(id, p);
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> e : quantities.entrySet()) {
            subtotal = subtotal.add(locked.get(e.getKey()).getPrice().multiply(BigDecimal.valueOf(e.getValue())));
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        if (subtotal.signum() <= 0) {
            throw new InvalidRequestException("items", "the bill total must be more than 0");
        }
        BigDecimal discount = discount(req.discountType(), req.discountValue(), subtotal);
        BigDecimal total = subtotal.subtract(discount);

        PaymentMethod method = activeMethod(req.paymentMethod(), "paymentMethod");
        if (req.amountReceived() != null && req.paymentStatus() == Bill.PaymentStatus.PAID && req.amountReceived().compareTo(total) < 0) {
            throw new InvalidRequestException("amountReceived", "is less than the bill total – record the bill as PENDING instead");
        }
        ShopCustomer customer = null;
        if (req.customerId() != null) {
            customer = customers.findById(req.customerId())
                    .orElseThrow(() -> new InvalidRequestException("customerId", "is not a saved customer"));
        }

        int year = ShopTime.today(clock).getYear();
        Bill bill = new Bill(numbers.next(year), req.requestKey(), customer == null ? null : customer.getId(),
                customer == null ? null : customer.getFullName(), customer == null ? null : customer.getPhone(), subtotal, discount,
                method, req.paymentStatus(), req.amountReceived(), blankToNull(req.notes()), CurrentUser.idOrNull(), CurrentUser.nameOrNull());
        int line = 1;
        for (Map.Entry<Long, Integer> e : quantities.entrySet()) {
            Product p = locked.get(e.getKey());
            bill.addItem(new BillItem(bill, line++, p.getId(), p.getName(), p.getSku(), p.getPackSize(), p.getUnit().name(),
                    p.getPrice(), e.getValue()));
        }
        Bill saved;
        try {
            saved = bills.saveAndFlush(bill);
        } catch (DataIntegrityViolationException ex) {
            // The same requestKey was saved by a simultaneous submit; this transaction is rolled back, nothing deducted twice.
            throw new ConflictException("This bill was already saved. Open Bill History to see it.");
        }
        for (Map.Entry<Long, Integer> e : quantities.entrySet()) {
            inventory.recordBill(locked.get(e.getKey()), -e.getValue(), saved.getId(), "Bill " + saved.getBillNumber());
        }
        audit.record("BILL_CREATED", "BILL", saved.getId(), saved.getBillNumber() + " ₹" + saved.getTotal().toPlainString()
                + " " + method.getCode() + " " + saved.getPaymentStatus());
        return BillDto.of(saved, false);
    }

    /** Voids a finalized bill: items go back to stock, the bill stays in the history marked VOIDED. */
    @Transactional
    public BillDto voidBill(Long id, String reason) {
        Bill bill = bills.findForUpdateById(id).orElseThrow(() -> new NotFoundException("Bill not found."));
        if (bill.getStatus() == Bill.Status.VOIDED) {
            throw new ConflictException("This bill is already voided.");
        }
        List<BillItem> items = new ArrayList<>(bill.getItems());
        items.sort((a, b) -> Long.compare(a.getProductId() == null ? 0 : a.getProductId(), b.getProductId() == null ? 0 : b.getProductId()));
        for (BillItem item : items) {
            if (item.getProductId() == null) {
                continue; // product deleted since; nothing to return to stock
            }
            products.findForUpdateById(item.getProductId()).ifPresent(p ->
                    inventory.recordBill(p, item.getQuantity(), bill.getId(), "Void of bill " + bill.getBillNumber()));
        }
        bill.voidBill(reason.trim(), CurrentUser.nameOrNull(), clock.instant());
        Bill saved = bills.saveAndFlush(bill);
        audit.record("BILL_VOIDED", "BILL", id, saved.getBillNumber() + " ₹" + saved.getTotal().toPlainString() + " | reason: " + reason.trim());
        return BillDto.of(bills.findWithItemsById(id).orElseThrow(), false);
    }

    /** A PENDING bill paid later at the shop. Amounts and lines never change. */
    @Transactional
    public BillDto markPaid(Long id, String paymentMethod, BigDecimal amountReceived) {
        Bill bill = bills.findForUpdateById(id).orElseThrow(() -> new NotFoundException("Bill not found."));
        if (bill.getStatus() == Bill.Status.VOIDED) {
            throw new ConflictException("A voided bill cannot be paid.");
        }
        if (bill.getPaymentStatus() == Bill.PaymentStatus.PAID) {
            throw new ConflictException("This bill is already paid.");
        }
        if (amountReceived != null && amountReceived.compareTo(bill.getTotal()) < 0) {
            throw new InvalidRequestException("amountReceived", "is less than the bill total");
        }
        PaymentMethod method = activeMethod(paymentMethod, "paymentMethod");
        bill.markPaid(method, amountReceived);
        bills.saveAndFlush(bill);
        audit.record("BILL_PAID", "BILL", id, bill.getBillNumber() + " " + method.getCode());
        return BillDto.of(bills.findWithItemsById(id).orElseThrow(), false);
    }

    @Transactional(readOnly = true)
    public BillDto get(Long id) {
        return BillDto.of(bills.findWithItemsById(id).orElseThrow(() -> new NotFoundException("Bill not found.")), false);
    }

    @Transactional(readOnly = true)
    public Bill entity(Long id) {
        Bill b = bills.findWithItemsById(id).orElseThrow(() -> new NotFoundException("Bill not found."));
        b.getPaymentMethod().getLabel(); // initialise for use outside the transaction (PDF)
        return b;
    }

    @Transactional(readOnly = true)
    public BillPage list(Filter f, int page, int size) {
        var result = bills.findAll(spec(f), PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Object[]> q = cb.createQuery(Object[].class);
        Root<Bill> root = q.from(Bill.class);
        Predicate where = spec(f).toPredicate(root, q, cb);
        Expression<BigDecimal> total = root.get("total");
        Predicate finalized = cb.equal(root.get("status"), Bill.Status.FINALIZED);
        Predicate pending = cb.and(finalized, cb.equal(root.get("paymentStatus"), Bill.PaymentStatus.PENDING));
        q.multiselect(
                cb.sum(cb.<Long>selectCase().when(finalized, 1L).otherwise(0L)),
                cb.sum(cb.<BigDecimal>selectCase().when(finalized, total).otherwise(BigDecimal.ZERO)),
                cb.sum(cb.<Long>selectCase().when(finalized, 0L).otherwise(1L)),
                cb.sum(cb.<BigDecimal>selectCase().when(pending, total).otherwise(BigDecimal.ZERO))).where(where);
        Object[] t = em.createQuery(q).getSingleResult();
        return new BillPage(PageResponse.of(result, BillingService::row), t[0] == null ? 0 : ((Number) t[0]).longValue(),
                money((BigDecimal) t[1]), t[2] == null ? 0 : ((Number) t[2]).longValue(), money((BigDecimal) t[3]));
    }

    // ------------------------------------------------------------------ payment methods

    @Transactional(readOnly = true)
    public List<PaymentMethodDto> paymentMethods() {
        return methods.findAllByOrderByDisplayOrderAscIdAsc().stream().map(PaymentMethodDto::of).toList();
    }

    @Transactional
    public PaymentMethodDto createMethod(PaymentMethodRequest req) {
        if (methods.existsByCode(req.code())) {
            throw new ConflictException("code", "is already used");
        }
        if (methods.existsByLabelIgnoreCase(req.label().trim())) {
            throw new ConflictException("label", "is already used");
        }
        PaymentMethod m = new PaymentMethod(req.code(), req.label().trim());
        m.setDisplayOrder(req.displayOrder());
        m.setActive(req.active());
        PaymentMethod saved = methods.saveAndFlush(m);
        audit.record("PAYMENT_METHOD_CREATED", "PAYMENT_METHOD", saved.getId(), saved.getLabel());
        return PaymentMethodDto.of(saved);
    }

    @Transactional
    public PaymentMethodDto updateMethod(Long id, PaymentMethodRequest req) {
        PaymentMethod m = methods.findById(id).orElseThrow(() -> new NotFoundException("Payment method not found."));
        if (!m.getCode().equals(req.code())) {
            throw new InvalidRequestException("code", "cannot be changed once created");
        }
        if (methods.existsByLabelIgnoreCaseAndIdNot(req.label().trim(), id)) {
            throw new ConflictException("label", "is already used");
        }
        m.setLabel(req.label().trim());
        m.setDisplayOrder(req.displayOrder());
        m.setActive(req.active());
        PaymentMethod saved = methods.saveAndFlush(m);
        audit.record("PAYMENT_METHOD_UPDATED", "PAYMENT_METHOD", id, saved.getLabel() + (saved.isActive() ? "" : " (inactive)"));
        return PaymentMethodDto.of(saved);
    }

    // ------------------------------------------------------------------ helpers

    static BigDecimal discount(DiscountType type, BigDecimal value, BigDecimal subtotal) {
        if (type == null || value == null || value.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal amount = type == DiscountType.PERCENT
                ? subtotal.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
        BigDecimal max = subtotal.multiply(MAX_DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP);
        if (type == DiscountType.PERCENT && value.compareTo(new BigDecimal("20")) > 0 || amount.compareTo(max) > 0) {
            throw new InvalidRequestException("discountValue", "can be at most 20% of the subtotal (₹" + max.toPlainString() + ")");
        }
        return amount;
    }

    private PaymentMethod activeMethod(String code, String field) {
        PaymentMethod m = methods.findByCode(code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new InvalidRequestException(field, "is not a known payment method"));
        if (!m.isActive()) {
            throw new InvalidRequestException(field, "is switched off");
        }
        return m;
    }

    public static Specification<Bill> spec(Filter f) {
        return (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (f.range() != null) {
                where.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.range().startInstant()));
                where.add(cb.lessThan(root.get("createdAt"), f.range().endInstantExclusive()));
            }
            if (f.search() != null && !f.search().isBlank()) {
                String p = "%" + ProductSpecifications.escapeLike(f.search().trim()) + "%";
                where.add(cb.or(cb.like(root.get("billNumber"), p, '\\'), cb.like(root.get("customerName"), p, '\\'),
                        cb.like(root.get("customerPhone"), p, '\\')));
            }
            if (f.paymentMethod() != null && !f.paymentMethod().isBlank()) {
                where.add(cb.equal(root.get("paymentMethod").get("code"), f.paymentMethod().trim().toUpperCase(Locale.ROOT)));
            }
            if (f.paymentStatus() != null) {
                where.add(cb.equal(root.get("paymentStatus"), f.paymentStatus()));
            }
            if (f.status() != null) {
                where.add(cb.equal(root.get("status"), f.status()));
            }
            if (f.customerId() != null) {
                where.add(cb.equal(root.get("customerId"), f.customerId()));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
    }

    public static BillRow row(Bill b) {
        return new BillRow(b.getId(), b.getBillNumber(), b.getStatus(), b.getCustomerName(), b.getCustomerPhone(), b.getItems().size(),
                b.getTotal(), b.getPaymentMethod().getLabel(), b.getPaymentStatus(), b.getCreatedByName(), b.getCreatedAt());
    }

    private static BigDecimal money(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
