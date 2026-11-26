package com.sivacrackers.report;

import com.sivacrackers.common.ShopTime;
import com.sivacrackers.common.exception.InvalidRequestException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business reports built only from recorded data: stock movements, suppliers and expenses.
 * There are deliberately no sales, revenue or profit figures – no reliable sales data exists
 * (in-store billing is not approved). "Purchase value" is only what staff entered as unit cost
 * when receiving stock.
 */
@Service
public class ReportService {

    public enum GroupBy {
        DAY("%Y-%m-%d"), MONTH("%Y-%m"), YEAR("%Y");

        final String mysqlFormat;

        GroupBy(String mysqlFormat) {
            this.mysqlFormat = mysqlFormat;
        }
    }

    public record SummaryRow(String period, long stockInUnits, long stockOutUnits, long adjustmentUnits, long movements,
                             BigDecimal purchaseValue, long expenseCount, BigDecimal expenseTotal) {
    }

    public record Summary(String from, String to, GroupBy groupBy, List<SummaryRow> rows, SummaryRow totals) {
    }

    public record SupplierRow(Long supplierId, String name, boolean active, long deliveries, long unitsReceived,
                              BigDecimal purchaseValue, Instant lastDeliveryAt) {
    }

    public record ExpenseCategoryRow(Long categoryId, String category, long count, BigDecimal total) {
    }

    public record ExpenseReport(String from, String to, List<ExpenseCategoryRow> byCategory, long count, BigDecimal total) {
    }

    private final JdbcTemplate jdbc;

    public ReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Daily / monthly / yearly totals (Indian time) of stock movements and active expenses. */
    @Transactional(readOnly = true)
    public Summary summary(ShopTime.Range range, GroupBy groupBy) {
        if (groupBy == GroupBy.DAY && ChronoUnit.DAYS.between(range.from(), range.to()) > 366) {
            throw new InvalidRequestException("groupBy", "use MONTH or YEAR for periods longer than one year");
        }
        String fmt = groupBy.mysqlFormat;
        Map<String, SummaryRow> rows = new TreeMap<>();
        jdbc.query("""
                SELECT DATE_FORMAT(CONVERT_TZ(created_at, '+00:00', ?), ?) AS p,
                       SUM(CASE WHEN movement_type = 'STOCK_IN' THEN quantity_change ELSE 0 END) AS in_units,
                       SUM(CASE WHEN movement_type = 'STOCK_OUT' THEN -quantity_change ELSE 0 END) AS out_units,
                       SUM(CASE WHEN movement_type = 'ADJUSTMENT' THEN quantity_change ELSE 0 END) AS adj_units,
                       COUNT(*) AS n,
                       SUM(CASE WHEN movement_type = 'STOCK_IN' AND unit_cost IS NOT NULL THEN quantity_change * unit_cost ELSE 0 END) AS pv
                FROM stock_movements
                WHERE created_at >= ? AND created_at < ?
                GROUP BY p""",
                (ResultSet rs) -> {
                    String p = rs.getString("p");
                    rows.put(p, new SummaryRow(p, rs.getLong("in_units"), rs.getLong("out_units"), rs.getLong("adj_units"),
                            rs.getLong("n"), money(rs.getBigDecimal("pv")), 0, BigDecimal.ZERO.setScale(2)));
                },
                ShopTime.MYSQL_OFFSET, fmt, utc(range.startInstant()), utc(range.endInstantExclusive()));
        jdbc.query("""
                SELECT DATE_FORMAT(expense_date, ?) AS p, COUNT(*) AS n, SUM(amount) AS t
                FROM expenses
                WHERE status = 'ACTIVE' AND expense_date >= ? AND expense_date <= ?
                GROUP BY p""",
                (ResultSet rs) -> {
                    String p = rs.getString("p");
                    SummaryRow s = rows.get(p);
                    long n = rs.getLong("n");
                    BigDecimal t = money(rs.getBigDecimal("t"));
                    rows.put(p, s == null
                            ? new SummaryRow(p, 0, 0, 0, 0, BigDecimal.ZERO.setScale(2), n, t)
                            : new SummaryRow(p, s.stockInUnits(), s.stockOutUnits(), s.adjustmentUnits(), s.movements(),
                                    s.purchaseValue(), n, t));
                },
                fmt, range.from(), range.to());
        List<SummaryRow> list = new ArrayList<>(rows.values());
        SummaryRow totals = new SummaryRow("Total",
                list.stream().mapToLong(SummaryRow::stockInUnits).sum(),
                list.stream().mapToLong(SummaryRow::stockOutUnits).sum(),
                list.stream().mapToLong(SummaryRow::adjustmentUnits).sum(),
                list.stream().mapToLong(SummaryRow::movements).sum(),
                list.stream().map(SummaryRow::purchaseValue).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2),
                list.stream().mapToLong(SummaryRow::expenseCount).sum(),
                list.stream().map(SummaryRow::expenseTotal).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2));
        return new Summary(range.from().toString(), range.to().toString(), groupBy, list, totals);
    }

    /** Every supplier with the stock received from them in the period (including suppliers with none). */
    @Transactional(readOnly = true)
    public List<SupplierRow> suppliers(ShopTime.Range range) {
        return jdbc.query("""
                SELECT s.id, s.name, s.active,
                       COUNT(m.id) AS deliveries,
                       COALESCE(SUM(m.quantity_change), 0) AS units,
                       COALESCE(SUM(m.quantity_change * m.unit_cost), 0) AS pv,
                       MAX(m.created_at) AS last_at
                FROM suppliers s
                LEFT JOIN stock_movements m ON m.supplier_id = s.id AND m.movement_type = 'STOCK_IN'
                     AND m.created_at >= ? AND m.created_at < ?
                GROUP BY s.id, s.name, s.active
                ORDER BY units DESC, s.name ASC""",
                (rs, n) -> new SupplierRow(rs.getLong("id"), rs.getString("name"), rs.getBoolean("active"), rs.getLong("deliveries"),
                        rs.getLong("units"), money(rs.getBigDecimal("pv")), instant(rs, "last_at")),
                utc(range.startInstant()), utc(range.endInstantExclusive()));
    }

    /** Active expenses in the period, per category. */
    @Transactional(readOnly = true)
    public ExpenseReport expenses(ShopTime.Range range) {
        List<ExpenseCategoryRow> rows = jdbc.query("""
                SELECT c.id, c.name, COUNT(e.id) AS n, COALESCE(SUM(e.amount), 0) AS t
                FROM expense_categories c
                LEFT JOIN expenses e ON e.category_id = c.id AND e.status = 'ACTIVE'
                     AND e.expense_date >= ? AND e.expense_date <= ?
                GROUP BY c.id, c.name, c.display_order
                ORDER BY t DESC, c.display_order ASC""",
                (rs, n) -> new ExpenseCategoryRow(rs.getLong("id"), rs.getString("name"), rs.getLong("n"), money(rs.getBigDecimal("t"))),
                range.from(), range.to());
        return new ExpenseReport(range.from().toString(), range.to().toString(), rows,
                rows.stream().mapToLong(ExpenseCategoryRow::count).sum(),
                rows.stream().map(ExpenseCategoryRow::total).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2));
    }

    /** created_at columns hold UTC wall-clock time (Hibernate jdbc.time_zone = UTC). */
    private static LocalDateTime utc(Instant i) {
        return LocalDateTime.ofInstant(i, ZoneOffset.UTC);
    }

    private static Instant instant(ResultSet rs, String col) throws SQLException {
        LocalDateTime t = rs.getObject(col, LocalDateTime.class);
        return t == null ? null : t.toInstant(ZoneOffset.UTC);
    }

    private static BigDecimal money(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
