package com.sivacrackers.expense;

import com.sivacrackers.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** One shop expense. Mistakes are fixed by editing or cancelling (with a reason) – never by deleting. */
@Entity
@Table(name = "expenses")
public class Expense extends BaseEntity {

    public enum Status { ACTIVE, CANCELLED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ExpenseCategory category;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(name = "paid_to", length = 120)
    private String paidTo;

    @Column(length = 60)
    private String reference;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status = Status.ACTIVE;

    @Column(name = "cancel_reason", length = 300)
    private String cancelReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "recorded_by", updatable = false)
    private Long recordedBy;

    @Column(name = "recorded_by_name", length = 100, updatable = false)
    private String recordedByName;

    protected Expense() {
        // for JPA
    }

    public Expense(Long recordedBy, String recordedByName) {
        this.recordedBy = recordedBy;
        this.recordedByName = recordedByName;
    }

    public void cancel(String reason, Instant at) {
        this.status = Status.CANCELLED;
        this.cancelReason = reason;
        this.cancelledAt = at;
    }

    public ExpenseCategory getCategory() { return category; }
    public void setCategory(ExpenseCategory category) { this.category = category; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPaidTo() { return paidTo; }
    public void setPaidTo(String paidTo) { this.paidTo = paidTo; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Status getStatus() { return status; }
    public String getCancelReason() { return cancelReason; }
    public Instant getCancelledAt() { return cancelledAt; }
    public Long getRecordedBy() { return recordedBy; }
    public String getRecordedByName() { return recordedByName; }
}
