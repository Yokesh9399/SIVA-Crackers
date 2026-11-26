package com.sivacrackers.expense;

import com.sivacrackers.common.Params;
import com.sivacrackers.common.ShopTime;
import com.sivacrackers.expense.ExpenseDtos.CancelRequest;
import com.sivacrackers.expense.ExpenseDtos.CategoryDto;
import com.sivacrackers.expense.ExpenseDtos.CategoryRequest;
import com.sivacrackers.expense.ExpenseDtos.ExpenseDto;
import com.sivacrackers.expense.ExpenseDtos.ExpensePage;
import com.sivacrackers.expense.ExpenseDtos.ExpenseRequest;
import com.sivacrackers.expense.ExpenseDtos.MonthlySummary;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Shop expenses – OWNER and MANAGER. Expenses are cancelled with a reason, never deleted. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
public class AdminExpenseController {

    private final ExpenseService service;
    private final Clock clock;

    public AdminExpenseController(ExpenseService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping("/expense-categories")
    public List<CategoryDto> categories() {
        return service.categories();
    }

    @PostMapping("/expense-categories")
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createCategory(body));
    }

    @PutMapping("/expense-categories/{id}")
    public CategoryDto updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest body) {
        return service.updateCategory(id, body);
    }

    /** {@code ?from=&to=&categoryId=&status=ACTIVE|CANCELLED&search=&sort=date,desc|date,asc|amount,desc|amount,asc&page=&size=25|50|100} */
    @GetMapping("/expenses")
    public ExpensePage list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                            @RequestParam(required = false) Long categoryId,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) String search,
                            @RequestParam(required = false) String sort,
                            @RequestParam(required = false) Integer page,
                            @RequestParam(required = false) Integer size) {
        if (from != null || to != null) {
            ShopTime.Range.of(from != null ? from : to, to != null ? to : from, clock); // validates order / length
        }
        var filter = new ExpenseService.Filter(from, to, categoryId, Params.enumOrNull(Expense.Status.class, "status", status),
                Params.search(search));
        return service.list(filter, ExpenseService.ExpenseSort.of(sort), Params.page(page), Params.size(size, List.of(25, 50, 100), 25));
    }

    @GetMapping("/expenses/{id}")
    public ExpenseDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/expenses")
    public ResponseEntity<ExpenseDto> create(@Valid @RequestBody ExpenseRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(body));
    }

    @PutMapping("/expenses/{id}")
    public ExpenseDto update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest body) {
        return service.update(id, body);
    }

    @PostMapping("/expenses/{id}/cancel")
    public ExpenseDto cancel(@PathVariable Long id, @Valid @RequestBody CancelRequest body) {
        return service.cancel(id, body.reason());
    }

    @GetMapping("/expenses/summary/monthly")
    public MonthlySummary monthly(@RequestParam(required = false) Integer year) {
        return service.monthly(year != null ? year : ShopTime.today(clock).getYear());
    }
}
