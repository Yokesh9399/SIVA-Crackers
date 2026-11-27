package com.sivacrackers.expense;

import com.sivacrackers.audit.AuditService;
import com.sivacrackers.catalog.product.ProductSpecifications;
import com.sivacrackers.common.ShopTime;
import com.sivacrackers.common.dto.PageResponse;
import com.sivacrackers.common.exception.ConflictException;
import com.sivacrackers.common.exception.InvalidRequestException;
import com.sivacrackers.common.exception.NotFoundException;
import com.sivacrackers.expense.ExpenseDtos.CategoryDto;
import com.sivacrackers.expense.ExpenseDtos.CategoryRequest;
import com.sivacrackers.expense.ExpenseDtos.ExpenseDto;
import com.sivacrackers.expense.ExpenseDtos.ExpensePage;
import com.sivacrackers.expense.ExpenseDtos.ExpenseRequest;
import com.sivacrackers.expense.ExpenseDtos.MonthTotal;
import com.sivacrackers.expense.ExpenseDtos.MonthlySummary;
import com.sivacrackers.security.CurrentUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Shop expenses. Every create, change and cancellation is written to the activity log with the amounts involved. */
@Service
public class ExpenseService {

    public record Filter(LocalDate from, LocalDate to, Long categoryId, Expense.Status status, String search) {
    }

    public enum ExpenseSort {
        DATE_DESC("date,desc", Sort.by(Sort.Order.desc("expenseDate"), Sort.Order.desc("id"))),
        DATE_ASC("date,asc", Sort.by(Sort.Order.asc("expenseDate"), Sort.Order.asc("id"))),
        AMOUNT_DESC("amount,desc", Sort.by(Sort.Order.desc("amount"), Sort.Order.desc("id"))),
        AMOUNT_ASC("amount,asc", Sort.by(Sort.Order.asc("amount"), Sort.Order.asc("id")));

        private final String param;
        private final Sort sort;

        ExpenseSort(String param, Sort sort) {
            this.param = param;
            this.sort = sort;
        }

        public static ExpenseSort of(String value) {
            if (value == null || value.isBlank()) {
                return DATE_DESC;
            }
            for (ExpenseSort s : values()) {
                if (s.param.equals(value.trim())) {
                    return s;
                }
            }
            throw new InvalidRequestException("sort", "must be one of date,desc, date,asc, amount,desc, amount,asc");
        }
    }

    private final ExpenseRepository expenses;
    private final ExpenseCategoryRepository categories;
    private final AuditService audit;
    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public ExpenseService(ExpenseRepository expenses, ExpenseCategoryRepository categories, AuditService audit, EntityManager em,
                          JdbcTemplate jdbc, Clock clock) {
        this.expenses = expenses;
        this.categories = categories;
        this.audit = audit;
        this.em = em;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ categories

    @Transactional(readOnly = true)
    public List<CategoryDto> categories() {
        return categories.findAllByOrderByDisplayOrderAscNameAsc().stream().map(CategoryDto::of).toList();
    }

    @Transactional
    public CategoryDto createCategory(CategoryRequest req) {
        String name = req.name().trim();
        if (categories.existsByNameIgnoreCase(name)) {
            throw new ConflictException("name", "is already used by another expense category");
        }
        ExpenseCategory c = new ExpenseCategory(name);
        c.setDisplayOrder(req.displayOrder());
        c.setActive(req.active());
        ExpenseCategory saved = saveCategory(c);
        audit.record("EXPENSE_CATEGORY_CREATED", "EXPENSE_CATEGORY", saved.getId(), name);
        return CategoryDto.of(saved);
    }

    @Transactional
    public CategoryDto updateCategory(Long id, CategoryRequest req) {
        ExpenseCategory c = categories.findById(id).orElseThrow(() -> new NotFoundException("Expense category not found."));
        String name = req.name().trim();
        if (categories.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("name", "is already used by another expense category");
        }
        c.setName(name);
        c.setDisplayOrder(req.displayOrder());
        c.setActive(req.active());
        ExpenseCategory saved = saveCategory(c);
        audit.record("EXPENSE_CATEGORY_UPDATED", "EXPENSE_CATEGORY", id, name + (req.active() ? "" : " (inactive)"));
        return CategoryDto.of(saved);
    }

    // ------------------------------------------------------------------ expenses

    @Transactional(readOnly = true)
    public ExpensePage list(Filter f, ExpenseSort sort, int page, int size) {
        var result = expenses.findAll(spec(f), PageRequest.of(page, size, sort.sort));
        // Totals over all pages, ACTIVE only (cancelled expenses never count).
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Object[]> q = cb.createQuery(Object[].class);
        Root<Expense> root = q.from(Expense.class);
        Filter activeOnly = new Filter(f.from(), f.to(), f.categoryId(),
                f.status() == Expense.Status.CANCELLED ? Expense.Status.CANCELLED : Expense.Status.ACTIVE, f.search());
        Predicate where = spec(activeOnly).toPredicate(root, q, cb);
        q.multiselect(cb.count(root), cb.coalesce(cb.sum(root.<BigDecimal>get("amount")), BigDecimal.ZERO)).where(where);
        Object[] totals = em.createQuery(q).getSingleResult();
        boolean onlyCancelled = f.status() == Expense.Status.CANCELLED;
        return new ExpensePage(PageResponse.of(result, ExpenseDto::of), onlyCancelled ? 0 : (Long) totals[0],
                onlyCancelled ? BigDecimal.ZERO : (BigDecimal) totals[1]);
    }

    @Transactional(readOnly = true)
    public ExpenseDto get(Long id) {
        return ExpenseDto.of(find(id));
    }

    @Transactional
    public ExpenseDto create(ExpenseRequest req) {
        Expense e = new Expense(CurrentUser.idOrNull(), CurrentUser.nameOrNull());
        apply(e, req, true);
        Expense saved = expenses.saveAndFlush(e);
        audit.record("EXPENSE_CREATED", "EXPENSE", saved.getId(), describe(saved));
        return ExpenseDto.of(saved);
    }

    @Transactional
    public ExpenseDto update(Long id, ExpenseRequest req) {
        Expense e = find(id);
        if (e.getStatus() == Expense.Status.CANCELLED) {
            throw new ConflictException("A cancelled expense cannot be edited.");
        }
        String before = describe(e);
        apply(e, req, e.getCategory().getId().equals(req.categoryId()));
        Expense saved = expenses.saveAndFlush(e);
        audit.record("EXPENSE_UPDATED", "EXPENSE", id, "before: " + before + " | after: " + describe(saved));
        return ExpenseDto.of(saved);
    }

    @Transactional
    public ExpenseDto cancel(Long id, String reason) {
        Expense e = find(id);
        if (e.getStatus() == Expense.Status.CANCELLED) {
            throw new ConflictException("This expense is already cancelled.");
        }
        e.cancel(reason.trim(), clock.instant());
        Expense saved = expenses.saveAndFlush(e);
        audit.record("EXPENSE_CANCELLED", "EXPENSE", id, describe(saved) + " | reason: " + reason.trim());
        return ExpenseDto.of(saved);
    }

    /** ACTIVE expense totals per month of one year (Indian calendar dates as entered). */
    @Transactional(readOnly = true)
    public MonthlySummary monthly(int year) {
        if (year < 2000 || year > 2100) {
            throw new InvalidRequestException("year", "must be between 2000 and 2100");
        }
        Map<Integer, Object[]> rows = jdbc.query("""
                        SELECT MONTH(expense_date) AS m, COUNT(*) AS c, SUM(amount) AS t
                        FROM expenses WHERE status = 'ACTIVE' AND YEAR(expense_date) = ?
                        GROUP BY MONTH(expense_date)""",
                (rs, n) -> new Object[] {rs.getInt("m"), rs.getLong("c"), rs.getBigDecimal("t")}, year)
                .stream().collect(Collectors.toMap(r -> (Integer) r[0], r -> r));
        List<MonthTotal> months = IntStream.rangeClosed(1, 12).mapToObj(m -> {
            Object[] r = rows.get(m);
            return r == null ? new MonthTotal(m, 0, BigDecimal.ZERO) : new MonthTotal(m, (Long) r[1], (BigDecimal) r[2]);
        }).toList();
        return new MonthlySummary(year, months, months.stream().map(MonthTotal::total).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** This calendar month's ACTIVE total (dashboard). */
    @Transactional(readOnly = true)
    public BigDecimal thisMonthTotal() {
        LocalDate today = ShopTime.today(clock);
        return expenses.activeTotalBetween(today.withDayOfMonth(1), today);
    }

    // ------------------------------------------------------------------ helpers

    public static Specification<Expense> spec(Filter f) {
        return (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (f.from() != null) {
                where.add(cb.greaterThanOrEqualTo(root.get("expenseDate"), f.from()));
            }
            if (f.to() != null) {
                where.add(cb.lessThanOrEqualTo(root.get("expenseDate"), f.to()));
            }
            if (f.categoryId() != null) {
                where.add(cb.equal(root.get("category").get("id"), f.categoryId()));
            }
            if (f.status() != null) {
                where.add(cb.equal(root.get("status"), f.status()));
            }
            if (f.search() != null && !f.search().isBlank()) {
                String p = "%" + ProductSpecifications.escapeLike(f.search().trim()) + "%";
                where.add(cb.or(cb.like(root.get("description"), p, '\\'), cb.like(root.get("paidTo"), p, '\\'),
                        cb.like(root.get("reference"), p, '\\')));
            }
            if (!Long.class.equals(query.getResultType()) && !Object[].class.equals(query.getResultType())) {
                root.fetch("category");
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
    }

    private void apply(Expense e, ExpenseRequest req, boolean sameCategory) {
        ExpenseCategory c = categories.findById(req.categoryId())
                .orElseThrow(() -> new InvalidRequestException("categoryId", "is not a valid expense category"));
        if (!c.isActive() && !sameCategory) {
            throw new InvalidRequestException("categoryId", "is inactive");
        }
        LocalDate today = ShopTime.today(clock);
        if (req.expenseDate().isAfter(today)) {
            throw new InvalidRequestException("expenseDate", "cannot be in the future");
        }
        if (req.expenseDate().isBefore(today.minusYears(10))) {
            throw new InvalidRequestException("expenseDate", "is too far in the past");
        }
        e.setCategory(c);
        e.setExpenseDate(req.expenseDate());
        e.setAmount(req.amount());
        e.setDescription(req.description().trim());
        e.setPaidTo(blankToNull(req.paidTo()));
        e.setReference(blankToNull(req.reference()));
        e.setNotes(blankToNull(req.notes()));
    }

    private ExpenseCategory saveCategory(ExpenseCategory c) {
        try {
            return categories.saveAndFlush(c);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("This expense category name is already used.");
        }
    }

    private Expense find(Long id) {
        return expenses.findWithCategoryById(id).orElseThrow(() -> new NotFoundException("Expense not found."));
    }

    private static String describe(Expense e) {
        return e.getExpenseDate() + " " + e.getCategory().getName() + " ₹" + e.getAmount().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + " – " + e.getDescription();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
