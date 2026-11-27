package com.sivacrackers.report;

import com.sivacrackers.billing.BillingDtos.BillRow;
import com.sivacrackers.billing.BillingService;
import com.sivacrackers.billing.PdfDocuments;
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
import com.sivacrackers.report.ReportService.PaymentMethodTotal;
import com.sivacrackers.report.ReportService.ProductSalesRow;
import com.sivacrackers.report.ReportService.SummaryRow;
import com.sivacrackers.report.ReportService.SupplierRow;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Business reports – OWNER and MANAGER. Each report returns JSON, or a download with {@code ?format=csv} or
 * {@code ?format=pdf}. Date filters are whole days in Indian time; the default period is the last 30 days.
 * Revenue comes only from finalized in-store bills; profit is never calculated (no reliable cost per sale).
 */
@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
public class AdminReportController {

    /** Exports stop here to protect the server; narrow the dates for more. */
    static final int EXPORT_MAX_ROWS = 10_000;

    private enum Format { JSON, CSV, PDF }

    /** One export: title, column names and rows, written as CSV or PDF. */
    private record Table(String title, String period, String fileName, String[] headers, List<Object[]> rows) {
    }

    private final ReportService reports;
    private final InventoryService inventory;
    private final ExpenseService expenses;
    private final BillingService billing;
    private final PdfDocuments pdf;
    private final Clock clock;

    public AdminReportController(ReportService reports, InventoryService inventory, ExpenseService expenses,
                                 BillingService billing, PdfDocuments pdf, Clock clock) {
        this.reports = reports;
        this.inventory = inventory;
        this.expenses = expenses;
        this.billing = billing;
        this.pdf = pdf;
        this.clock = clock;
    }

    @GetMapping("/summary")
    public ResponseEntity<?> summary(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                     @RequestParam(required = false) String groupBy,
                                     @RequestParam(required = false) String format) {
        Format f = format(format);
        GroupBy g = Params.enumOrNull(GroupBy.class, "groupBy", groupBy);
        var s = reports.summary(ShopTime.Range.of(from, to, clock), g == null ? GroupBy.DAY : g);
        if (f == Format.JSON) {
            return ResponseEntity.ok(s);
        }
        List<Object[]> rows = new ArrayList<>();
        List<SummaryRow> all = new ArrayList<>(s.rows());
        all.add(s.totals());
        for (SummaryRow r : all) {
            rows.add(new Object[] {r.period(), r.bills(), r.revenue(), r.discounts(), r.expenseCount(), r.expenseTotal(),
                    r.stockInUnits(), r.stockOutUnits(), r.adjustmentUnits(), r.purchaseValue()});
        }
        return export(f, new Table("Business summary (" + s.groupBy().name().toLowerCase(Locale.ROOT) + ")", period(s.from(), s.to())
                + " · Revenue = finalized bills only. Profit is not calculated (no reliable cost per sale).",
                "summary-" + s.groupBy().name().toLowerCase(Locale.ROOT) + "-" + s.from() + "-to-" + s.to(),
                new String[] {"Period", "Bills", "Revenue (INR)", "Discounts (INR)", "Expenses (count)", "Expenses (INR)",
                        "Stock in", "Stock out", "Adjust (net)", "Purchase value recorded (INR)"}, rows));
    }

    @GetMapping("/billing")
    public ResponseEntity<?> billingReport(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                           @RequestParam(required = false) String format) {
        Format f = format(format);
        ShopTime.Range range = ShopTime.Range.of(from, to, clock);
        var report = reports.billing(range);
        if (f == Format.JSON) {
            return ResponseEntity.ok(report);
        }
        List<Object[]> rows = new ArrayList<>();
        var bills = billing.list(new BillingService.Filter(range, null, null, null, null, null), 0, EXPORT_MAX_ROWS);
        for (BillRow b : bills.page().content()) {
            rows.add(new Object[] {b.createdAt(), b.billNumber(), b.status(), b.customerName(), b.itemCount(), b.total(),
                    b.paymentMethod(), b.paymentStatus(), b.createdByName()});
        }
        StringBuilder byMethod = new StringBuilder();
        for (PaymentMethodTotal m : report.byPaymentMethod()) {
            byMethod.append(" · ").append(m.label()).append(": ").append(m.bills()).append(" / Rs. ").append(m.total().toPlainString());
        }
        return export(f, new Table("In-store billing", period(report.from(), report.to()) + " · Revenue Rs. "
                + report.revenue().toPlainString() + " from " + report.finalizedBills() + " bills · voided " + report.voidedBills()
                + " · pending Rs. " + report.pendingTotal().toPlainString() + byMethod,
                "billing-" + report.from() + "-to-" + report.to(),
                new String[] {"Date & time", "Bill no.", "Status", "Customer", "Items", "Total (INR)", "Payment", "Payment status", "Billed by"},
                rows));
    }

    @GetMapping("/product-sales")
    public ResponseEntity<?> productSales(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                          @RequestParam(required = false) String search,
                                          @RequestParam(required = false) String sort,
                                          @RequestParam(required = false) String format) {
        Format f = format(format);
        if (sort != null && !sort.isBlank() && !sort.equals("revenue") && !sort.equals("units")) {
            throw new InvalidRequestException("sort", "must be revenue or units");
        }
        ShopTime.Range range = ShopTime.Range.of(from, to, clock);
        List<ProductSalesRow> rows = reports.productSales(range, Params.search(search), sort);
        if (f == Format.JSON) {
            return ResponseEntity.ok(rows);
        }
        List<Object[]> out = new ArrayList<>();
        for (ProductSalesRow r : rows) {
            out.add(new Object[] {r.productName(), r.productSku(), r.bills(), r.unitsSold(), r.revenue()});
        }
        return export(f, new Table("Product sales", period(range.from().toString(), range.to().toString()) + " · finalized bills only",
                "product-sales-" + range.from() + "-to-" + range.to(),
                new String[] {"Product", "Code", "Bills", "Units sold", "Sales before bill discount (INR)"}, out));
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
        Format f = format(format);
        ShopTime.Range range = ShopTime.Range.of(from, to, clock);
        var spec = StockMovementSpecifications.matching(new StockMovementSpecifications.Filter(range,
                Params.enumOrNull(MovementType.class, "type", type), Params.enumOrNull(MovementReason.class, "reason", reason),
                productId, supplierId, Params.search(search)));
        if (f == Format.JSON) {
            return ResponseEntity.ok(inventory.movements(spec, Params.page(page), Params.size(size, List.of(25, 50, 100), 25)));
        }
        List<Object[]> rows = new ArrayList<>();
        for (MovementDto m : inventory.movements(spec, 0, EXPORT_MAX_ROWS).content()) {
            rows.add(new Object[] {m.createdAt(), m.productName(), m.productSku(), m.type(), m.reasonLabel(), m.previousQuantity(),
                    m.quantityChange(), m.newQuantity(), m.supplier() == null ? null : m.supplier().name(), m.unitCost(), m.note(),
                    m.performedByName()});
        }
        return export(f, new Table("Stock movements", period(range.from().toString(), range.to().toString()),
                "stock-movements-" + range.from() + "-to-" + range.to(),
                new String[] {"Date & time", "Product", "Code", "Type", "Reason", "Before", "Change", "After", "Supplier",
                        "Unit cost (INR)", "Note", "By"}, rows));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<?> lowStock(@RequestParam(required = false) String format) {
        Format f = format(format);
        var alerts = inventory.alerts();
        if (f == Format.JSON) {
            return ResponseEntity.ok(alerts);
        }
        List<Object[]> rows = new ArrayList<>();
        for (StockRow r : alerts.outOfStock()) {
            rows.add(new Object[] {"Out of stock", r.name(), r.sku(), r.category().name(), r.brand().name(), r.stockQuantity(), r.reorderLevel()});
        }
        for (StockRow r : alerts.lowStock()) {
            rows.add(new Object[] {"Few left", r.name(), r.sku(), r.category().name(), r.brand().name(), r.stockQuantity(), r.reorderLevel()});
        }
        return export(f, new Table("Low stock", "As of " + ShopTime.today(clock), "low-stock-" + ShopTime.today(clock),
                new String[] {"Status", "Product", "Code", "Category", "Brand", "In stock", "Few-left level"}, rows));
    }

    @GetMapping("/suppliers")
    public ResponseEntity<?> suppliers(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                       @RequestParam(required = false) String format) {
        Format f = format(format);
        ShopTime.Range range = ShopTime.Range.of(from, to, clock);
        List<SupplierRow> rows = reports.suppliers(range);
        if (f == Format.JSON) {
            return ResponseEntity.ok(rows);
        }
        List<Object[]> out = new ArrayList<>();
        for (SupplierRow r : rows) {
            out.add(new Object[] {r.name(), r.active() ? "Active" : "Inactive", r.deliveries(), r.unitsReceived(), r.purchaseValue(),
                    r.lastDeliveryAt()});
        }
        return export(f, new Table("Suppliers", period(range.from().toString(), range.to().toString()),
                "suppliers-" + range.from() + "-to-" + range.to(),
                new String[] {"Supplier", "Status", "Deliveries", "Units received", "Purchase value recorded (INR)", "Last delivery"}, out));
    }

    @GetMapping("/expenses")
    public ResponseEntity<?> expenseReport(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                           @RequestParam(required = false) String format) {
        Format f = format(format);
        ShopTime.Range range = ShopTime.Range.of(from, to, clock);
        var report = reports.expenses(range);
        if (f == Format.JSON) {
            return ResponseEntity.ok(report);
        }
        if (f == Format.PDF) {
            List<Object[]> rows = new ArrayList<>();
            for (ExpenseCategoryRow r : report.byCategory()) {
                rows.add(new Object[] {r.category(), r.count(), r.total()});
            }
            rows.add(new Object[] {"Total", report.count(), report.total()});
            return export(f, new Table("Expenses by category", period(report.from(), report.to()) + " · cancelled expenses excluded",
                    "expenses-" + report.from() + "-to-" + report.to(), new String[] {"Category", "Expenses", "Total (INR)"}, rows));
        }
        Csv c = new Csv().row("Category", "Expenses (count)", "Total (INR)");
        for (ExpenseCategoryRow r : report.byCategory()) {
            c.row(r.category(), r.count(), r.total());
        }
        c.row("Total", report.count(), report.total()).row();
        c.row("Date", "Category", "Description", "Paid to", "Reference", "Amount (INR)", "Status", "Recorded by");
        var list = expenses.list(new ExpenseService.Filter(range.from(), range.to(), null, null, null),
                ExpenseService.ExpenseSort.DATE_ASC, 0, EXPORT_MAX_ROWS);
        for (ExpenseDto e : list.page().content()) {
            c.row(e.expenseDate(), e.category().name(), e.description(), e.paidTo(), e.reference(), e.amount(), e.status(),
                    e.recordedByName());
        }
        return c.download("expenses-" + range.from() + "-to-" + range.to() + ".csv");
    }

    // ------------------------------------------------------------------ helpers

    private ResponseEntity<byte[]> export(Format f, Table t) {
        if (f == Format.CSV) {
            Csv c = new Csv().row((Object[]) t.headers());
            t.rows().forEach(c::row);
            return c.download(t.fileName() + ".csv");
        }
        byte[] body = pdf.table(t.title(), t.period(), t.headers(), t.rows());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(t.fileName() + ".pdf").build().toString())
                .body(body);
    }

    private static String period(String from, String to) {
        return from + " to " + to + " (Indian dates)";
    }

    private static Format format(String format) {
        if (format == null || format.isBlank()) {
            return Format.JSON;
        }
        return switch (format.trim().toLowerCase(Locale.ROOT)) {
            case "json" -> Format.JSON;
            case "csv" -> Format.CSV;
            case "pdf" -> Format.PDF;
            default -> throw new InvalidRequestException("format", "must be json, csv or pdf");
        };
    }
}
