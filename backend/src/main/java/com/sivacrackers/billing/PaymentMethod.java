package com.sivacrackers.billing;

import com.sivacrackers.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A way a customer paid at the shop counter (cash, UPI, card, …). No online payment is processed. */
@Entity
@Table(name = "payment_methods")
public class PaymentMethod extends BaseEntity {

    @Column(nullable = false, length = 20, updatable = false)
    private String code;

    @Column(nullable = false, length = 60)
    private String label;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true;

    protected PaymentMethod() {
        // for JPA
    }

    public PaymentMethod(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
