package com.sivacrackers.expense;

import com.sivacrackers.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Kind of shop expense (rent, electricity, …). Deactivated instead of deleted once used. */
@Entity
@Table(name = "expense_categories")
public class ExpenseCategory extends BaseEntity {

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true;

    protected ExpenseCategory() {
        // for JPA
    }

    public ExpenseCategory(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
