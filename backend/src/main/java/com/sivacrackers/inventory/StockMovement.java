package com.sivacrackers.inventory;

import com.sivacrackers.supplier.Supplier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * One change to a product's stock, with the quantity before and after. Written once, never edited:
 * mistakes are corrected with a new movement, so the history always adds up.
 * The product's name and code are copied so the history survives if the product is deleted later.
 */
@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", updatable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 150, updatable = false)
    private String productName;

    @Column(name = "product_sku", nullable = false, length = 40, updatable = false)
    private String productSku;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 12, updatable = false)
    private MovementType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24, updatable = false)
    private MovementReason reason;

    @Column(name = "previous_quantity", nullable = false, updatable = false)
    private int previousQuantity;

    @Column(name = "quantity_change", nullable = false, updatable = false)
    private int quantityChange;

    @Column(name = "new_quantity", nullable = false, updatable = false)
    private int newQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", updatable = false)
    private Supplier supplier;

    @Column(name = "unit_cost", precision = 10, scale = 2, updatable = false)
    private BigDecimal unitCost;

    @Column(length = 500, updatable = false)
    private String note;

    @Column(name = "performed_by", updatable = false)
    private Long performedBy;

    @Column(name = "performed_by_name", length = 100, updatable = false)
    private String performedByName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StockMovement() {
        // for JPA
    }

    StockMovement(Long productId, String productName, String productSku, MovementReason reason, int previousQuantity,
                  int quantityChange, Supplier supplier, BigDecimal unitCost, String note, Long performedBy,
                  String performedByName, Instant createdAt) {
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.type = reason.type();
        this.reason = reason;
        this.previousQuantity = previousQuantity;
        this.quantityChange = quantityChange;
        this.newQuantity = previousQuantity + quantityChange;
        this.supplier = supplier;
        this.unitCost = unitCost;
        this.note = note;
        this.performedBy = performedBy;
        this.performedByName = performedByName;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductSku() { return productSku; }
    public MovementType getType() { return type; }
    public MovementReason getReason() { return reason; }
    public int getPreviousQuantity() { return previousQuantity; }
    public int getQuantityChange() { return quantityChange; }
    public int getNewQuantity() { return newQuantity; }
    public Supplier getSupplier() { return supplier; }
    public BigDecimal getUnitCost() { return unitCost; }
    public String getNote() { return note; }
    public Long getPerformedBy() { return performedBy; }
    public String getPerformedByName() { return performedByName; }
    public Instant getCreatedAt() { return createdAt; }
}
