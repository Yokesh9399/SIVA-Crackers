package com.sivacrackers.inventory;

import com.sivacrackers.catalog.product.admin.AdminProductQuery;
import com.sivacrackers.common.Params;
import com.sivacrackers.common.ShopTime;
import com.sivacrackers.common.dto.PageResponse;
import com.sivacrackers.inventory.InventoryDtos.Alerts;
import com.sivacrackers.inventory.InventoryDtos.MovementDto;
import com.sivacrackers.inventory.InventoryDtos.MovementRequest;
import com.sivacrackers.inventory.InventoryDtos.ReasonDto;
import com.sivacrackers.inventory.InventoryDtos.StockRow;
import com.sivacrackers.inventory.InventoryDtos.ThresholdRequest;
import com.sivacrackers.inventory.StockMovementSpecifications.Filter;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inventory. All staff (OWNER, MANAGER, CASHIER) can view stock and its history;
 * only OWNER and MANAGER can change stock or the low-stock level.
 */
@RestController
@RequestMapping("/api/admin/inventory")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER')")
public class AdminInventoryController {

    private static final List<Integer> MOVEMENT_PAGE_SIZES = List.of(25, 50, 100);

    private final InventoryService inventory;
    private final Clock clock;

    public AdminInventoryController(InventoryService inventory, Clock clock) {
        this.inventory = inventory;
        this.clock = clock;
    }

    /** Current stock; same filters as the admin product list. */
    @GetMapping
    public PageResponse<StockRow> stock(@RequestParam(required = false) String search,
                                        @RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) Long brandId,
                                        @RequestParam(required = false) String availability,
                                        @RequestParam(required = false) String visibility,
                                        @RequestParam(required = false) String sort,
                                        @RequestParam(required = false) Integer page,
                                        @RequestParam(required = false) Integer size) {
        return inventory.stock(AdminProductQuery.of(Params.search(search), categoryId, brandId, availability, visibility, sort,
                page, size));
    }

    @GetMapping("/alerts")
    public Alerts alerts() {
        return inventory.alerts();
    }

    @GetMapping("/reasons")
    public List<ReasonDto> reasons() {
        return Arrays.stream(MovementReason.values())
                .filter(r -> !r.isAutomatic())
                .map(r -> new ReasonDto(r, r.type(), r.label())).toList();
    }

    @GetMapping("/products/{productId}")
    public StockRow product(@PathVariable Long productId) {
        return inventory.stockOf(productId);
    }

    /** {@code ?from=yyyy-MM-dd&to=&type=&reason=&productId=&supplierId=&search=&page=&size=25|50|100} */
    @GetMapping("/movements")
    public PageResponse<MovementDto> movements(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        ShopTime.Range range = from == null && to == null ? null : ShopTime.Range.of(from, to, clock);
        Filter filter = new Filter(range, Params.enumOrNull(MovementType.class, "type", type),
                Params.enumOrNull(MovementReason.class, "reason", reason), productId, supplierId, Params.search(search));
        return inventory.movements(StockMovementSpecifications.matching(filter), Params.page(page),
                Params.size(size, MOVEMENT_PAGE_SIZES, 25));
    }

    @PostMapping("/movements")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<MovementDto> record(@Valid @RequestBody MovementRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventory.record(body));
    }

    @PatchMapping("/products/{productId}/threshold")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public StockRow threshold(@PathVariable Long productId, @Valid @RequestBody ThresholdRequest body) {
        return inventory.setThreshold(productId, body.reorderLevel());
    }
}
