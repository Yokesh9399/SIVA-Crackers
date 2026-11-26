package com.sivacrackers.billing;

import com.sivacrackers.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * An in-store bill made at the shop counter by staff. Once saved it is final: its lines and amounts never
 * change. It can only be voided (with a reason), which returns the items to stock. The payment is only
 * recorded – nothing is charged online.
 */
@Entity
@Table(name = "bills")
public class Bill extends BaseEntity {

    public enum Status { FINALIZED, VOIDED }

    public enum PaymentStatus { PAID, PENDING }

    @Column(name = "bill_number", nullable = false, length = 20, updatable = false)
    private String billNumber;

    @Column(name = "request_key", nullable = false, length = 64, updatable = false)
    private String requestKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status = Status.FINALIZED;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_name", length = 100, updatable = false)
    private String customerName;

    @Column(name = "customer_phone", length = 15, updatable = false)
    private String customerPhone;

    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal total;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 10)
    private PaymentStatus paymentStatus;

    @Column(name = "amount_received", precision = 12, scale = 2)
    private BigDecimal amountReceived;

    @Column(length = 300, updatable = false)
    private String notes;

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @Column(name = "created_by_name", length = 100, updatable = false)
    private String createdByName;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "voided_by_name", length = 100)
    private String voidedByName;

    @Column(name = "void_reason", length = 300)
    private String voidReason;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.PERSIST)
    @OrderBy("lineNo ASC")
    private List<BillItem> items = new ArrayList<>();

    protected Bill() {
        // for JPA
    }

    Bill(String billNumber, String requestKey, Long customerId, String customerName, String customerPhone,
         BigDecimal subtotal, BigDecimal discountAmount, PaymentMethod paymentMethod, PaymentStatus paymentStatus,
         BigDecimal amountReceived, String notes, Long createdBy, String createdByName) {
        this.billNumber = billNumber;
        this.requestKey = requestKey;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.total = subtotal.subtract(discountAmount);
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.amountReceived = amountReceived;
        this.notes = notes;
        this.createdBy = createdBy;
        this.createdByName = createdByName;
    }

    void addItem(BillItem item) {
        items.add(item);
    }

    void markPaid(PaymentMethod method, BigDecimal received) {
        this.paymentStatus = PaymentStatus.PAID;
        this.paymentMethod = method;
        this.amountReceived = received;
    }

    void voidBill(String reason, String byName, Instant at) {
        this.status = Status.VOIDED;
        this.voidReason = reason;
        this.voidedByName = byName;
        this.voidedAt = at;
    }

    public String getBillNumber() { return billNumber; }
    public String getRequestKey() { return requestKey; }
    public Status getStatus() { return status; }
    public Long getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getTotal() { return total; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public BigDecimal getAmountReceived() { return amountReceived; }
    public String getNotes() { return notes; }
    public Long getCreatedBy() { return createdBy; }
    public String getCreatedByName() { return createdByName; }
    public Instant getVoidedAt() { return voidedAt; }
    public String getVoidedByName() { return voidedByName; }
    public String getVoidReason() { return voidReason; }
    public List<BillItem> getItems() { return items; }
}
