package com.sivacrackers.expense;

import com.sivacrackers.common.dto.PageResponse;
import com.sivacrackers.common.dto.RefDto;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Expense API bodies ({@code /api/admin/expenses}, {@code /api/admin/expense-categories}). */
public final class ExpenseDtos {

    private ExpenseDtos() {
    }

    public record CategoryRequest(
            @NotBlank(message = "is required") @Size(max = 60, message = "must be at most 60 characters") String name,
            @NotNull(message = "is required") @Min(value = 0, message = "must not be negative") @Max(value = 9999, message = "is too large") Integer displayOrder,
            @NotNull(message = "is required") Boolean active) {
    }

    public record CategoryDto(Long id, String name, int displayOrder, boolean active) {

        static CategoryDto of(ExpenseCategory c) {
            return new CategoryDto(c.getId(), c.getName(), c.getDisplayOrder(), c.isActive());
        }
    }

    public record ExpenseRequest(
            @NotNull(message = "is required") Long categoryId,
            @NotNull(message = "is required") LocalDate expenseDate,
            @NotNull(message = "is required") @DecimalMin(value = "0.01", message = "must be more than 0")
            @DecimalMax(value = "9999999999.99", message = "is too large")
            @Digits(integer = 10, fraction = 2, message = "must be an amount with at most 2 decimal places") BigDecimal amount,
            @NotBlank(message = "is required") @Size(max = 200, message = "must be at most 200 characters") String description,
            @Size(max = 120, message = "must be at most 120 characters") String paidTo,
            @Size(max = 60, message = "must be at most 60 characters") String reference,
            @Size(max = 500, message = "must be at most 500 characters") String notes) {
    }

    public record CancelRequest(
            @NotBlank(message = "is required") @Size(max = 300, message = "must be at most 300 characters") String reason) {
    }

    public record ExpenseDto(Long id, RefDto category, LocalDate expenseDate, BigDecimal amount, String description, String paidTo,
                             String reference, String notes, Expense.Status status, String cancelReason, Instant cancelledAt,
                             String recordedByName, Instant createdAt, Instant updatedAt) {

        public static ExpenseDto of(Expense e) {
            return new ExpenseDto(e.getId(), new RefDto(e.getCategory().getId(), e.getCategory().getName(), null), e.getExpenseDate(),
                    e.getAmount(), e.getDescription(), e.getPaidTo(), e.getReference(), e.getNotes(), e.getStatus(),
                    e.getCancelReason(), e.getCancelledAt(), e.getRecordedByName(), e.getCreatedAt(), e.getUpdatedAt());
        }
    }

    /** A page of expenses plus totals of the ACTIVE expenses matching the same filters (all pages). */
    public record ExpensePage(PageResponse<ExpenseDto> page, long activeCount, BigDecimal activeTotal) {
    }

    public record MonthTotal(int month, long count, BigDecimal total) {
    }

    public record MonthlySummary(int year, List<MonthTotal> months, BigDecimal yearTotal) {
    }
}
