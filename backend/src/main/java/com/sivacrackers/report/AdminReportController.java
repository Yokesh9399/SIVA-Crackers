package com.sivacrackers.report;

import com.sivacrackers.common.Params;
import com.sivacrackers.common.ShopTime;
import com.sivacrackers.common.exception.InvalidRequestException;
import com.sivacrackers.expense.ExpenseDtos.ExpenseDto;
import com.sivacrackers.expense.ExpenseService;
import com.sivacrackers.inventory.InventoryDtos.MovementDto;
import com.sivacrackers.inventory.InventoryDtos.StockRow;
import com.sivacrackers.inventory.InventoryService;
import com.sivacrackers.inventory.MovementReason;
import com.sivacrackers.inventory.MovementType;
import com.sivacrackers.inventory.StockMovementSpecifications;
import com.sivacrackers.report.ReportService.ExpenseCategoryRow;
import com.sivacrackers.report.ReportService.GroupBy;
import com.sivacrackers.report.ReportService.SummaryRow;
import com.sivacrackers.report.ReportService.SupplierRow;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Business reports – OWNER and MANAGER. Each report returns JSON, or a CSV download with {@code ?format=csv}.
 * Date filters are whole days in Indian time; the default period is the last 30 days.
 */
@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
public class AdminReportController {

    /** CSV exports stop here to protect the server; narrow the dates for more. */
    static final int CSV_MAX_ROWS = 10_000;

    private final ReportService reports;
    private final InventoryService inventory;
    private final ExpenseService expenses;
    private final Clock clock;

    public AdminReportController(ReportService reports, InventoryService inventory, ExpenseService expenses, Clock clock) {
        this.reports = reports;
        this.inventory = inventory;
        this.expenses = expenses;
        this.clock = clock;
    }

    @GetMapping("/summary")
    public ResponseEntity<?> summary(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                     @RequestParam(required = false) String groupBy,
                                     @RequestParam(required = false) String format) {
        GroupBy g = Params.enumOrNull(GroupBy.class, "groupBy", groupBy);
        var s = reports.summary(ShopTime.Range.of(from, to, clock), g == null ? GroupBy.DAY : g);
        if (!csv(format)) {
            return ResponseEntity.ok(s);
        }
        Csv c = new Csv().row("Period", "Stock in (units)", "Stock out (units)", "Adjustments (net units)", "Stock movements",
                "Purchase value recorded (INR)", "Expenses (count)", "Expenses (INR)");
        List<SummaryRow> rows = new ArrayList<>(s.rows());
        rows.add(s.totals());
        rows.forEach(r -> c.row(r.period(), r.stockInUnits(), r.stockOutUnits(), r.adjustmentUnits(), r.movements(),
                r.purchaseValue(), r.expenseCount(), r.expenseTotal()));
        return c.download("summary-" + s.groupBy().name().toLowerCase() + "-" + s.from() + "-to-" + s.to() + ".csv");
    }

    @GetMapping("/stock-movements")
    public ResponseEntity<?> movements(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                       @RequestParam(required = false) String type,
                                       @RequestParam(required = false) String reason,
                                       @RequestParam(required = false) Long supplierId,
                                       @RequestParam(required = false) Long productId,
                                       @RequestParam(required = false) String search,
                                       @RequestParam(required = false) Integer page,
                                       @RequestParam(required = false) Integer size,
                                       @RequestParam(required = false) String format) {
        ShopTime.Range range = ShopTime.Range.of(from, to, clock);
        var spec = StockMovementSpecifications.matching(new StockMovementSpecifications.Filter(range,
                Params.enumOrNull(MovementType.class, "type", type), Params.enumOrNull(MovementReason.class, "reason", reason),
                productId, supplierId, Params.search(search)));
        if (!csv(format)) {
            return ResponseEntity.ok(inventory.movements(spec, Params.page(page), Params.size(size, List.of(25, 50, 100), 25)));
        }
        var all = inventory.movements(spec, 0, CSV_MAX_ROWS);
        Csv c = new Csv().row("Date & time", "Product", "Code", "Type", "Reason", "Before", "Change", "After", "Supplier",
                "Unit cost (INR)", "Note", "By");
        for (MovementDto m : all.content()) {
            c.row(m.createdAt(), m.productName(), m.productSku(), m.type(), m.reasonLabel(), m.previousQuantity(), m.quantityChange(),
                    m.newQuantity(), m.supplier() == null ? null : m.supplier().name(), m.unitCost(), m.note(), m.performedByName());
        }
        return c.download("stock-movements-" + range.from() + "-to-" + range.to() + ".csv");
    }

    @GetMapping("/low-stock")
    public ResponseEntity<?> lowStock(@RequestParam(required = false) String format) {
        var alerts = inventory.alerts();
        if (!csv(format)) {
            return ResponseEntity.ok(alerts);
        }
        Csv c = new Csv().row("Status", "Product", "Code", "Category", "Brand", "In stock", "Few-left level");
        for (StockRow r : alerts.outOfStock()) {
            c.row("Out of stock", r.name(), r.sku(), r.category().name(), r.brand().name(), r.stockQuantity(), r.reorderLevel());
        }
        for (StockRow r : alerts.lowStock()) {
            c.row("Few left", r.name(), r.sku(), r.category().name(), r.brand().name(), r.stockQuantity(), r.reorderLevel());
        }
        return c.download("low-stock-" + ShopTime.today(clock) + ".csv");
    }

    @GetMapping("/suppliers")
    public ResponseEntity<?> suppliers(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                       @RequestParam(required = false) String format) {
        ShopTime.Range range = ShopTime.Range.of(from, to, clock);
        List<SupplierRow> rows = reports.suppliers(range);
        if (!csv(format)) {
            return ResponseEntity.ok(rows);
        }
        Csv c = new Csv().row("Supplier", "Status", "Deliveries", "Units received", "Purchase value recorded (INR)", "Last delivery");
        rows.forEach(r -> c.row(r.name(), r.active() ? "Active" : "Inactive", r.deliveries(), r.unitsReceived(), r.purchaseValue(),
                r.lastDeliveryAt()));
        return c.download("suppliers-" + range.from() + "-to-" + range.to() + ".csv");
    }

    @GetMapping("/expenses")
    public ResponseEntity<?> expenseReport(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                           @RequestParam(required = false) String format) {
        ShopTime.Range range = ShopTime.Range.of(from, to, clock);
        var report = reports.expenses(range);
        if (!csv(format)) {
            return ResponseEntity.ok(report);
        }
        Csv c = new Csv().row("Category", "Expenses (count)", "Total (INR)");
        for (ExpenseCategoryRow r : report.byCategory()) {
            c.row(r.category(), r.count(), r.total());
        }
        c.row("Total", report.count(), report.total()).row();
        c.row("Date", "Category", "Description", "Paid to", "Reference", "Amount (INR)", "Status", "Recorded by");
        var list = expenses.list(new ExpenseService.Filter(range.from(), range.to(), null, null, null),
                ExpenseService.ExpenseSort.DATE_ASC, 0, CSV_MAX_ROWS);
        for (ExpenseDto e : list.page().content()) {
            c.row(e.expenseDate(), e.category().name(), e.description(), e.paidTo(), e.reference(), e.amount(), e.status(),
                    e.recordedByName());
        }
        return c.download("expenses-" + range.from() + "-to-" + range.to() + ".csv");
    }

    private static boolean csv(String format) {
        if (format == null || format.isBlank() || format.equalsIgnoreCase("json")) {
            return false;
        }
        if (format.equalsIgnoreCase("csv")) {
            return true;
        }
        throw new InvalidRequestException("format", "must be json or csv");
    }
}
