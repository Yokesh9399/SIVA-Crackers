package com.sivacrackers.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** One line of a bill. Product details are copied at billing time and never change afterwards. */
@Entity
@Table(name = "bill_items")
public class BillItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id", nullable = false, updatable = false)
    private Bill bill;

    @Column(name = "line_no", nullable = false, updatable = false)
    private int lineNo;

    @Column(name = "product_id", updatable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 150, updatable = false)
    private String productName;

    @Column(name = "product_sku", nullable = false, length = 40, updatable = false)
    private String productSku;

    @Column(name = "pack_size", nullable = false, length = 50, updatable = false)
    private String packSize;

    @Column(nullable = false, length = 10, updatable = false)
    private String unit;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2, updatable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false, updatable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal lineTotal;

    protected BillItem() {
        // for JPA
    }

    BillItem(Bill bill, int lineNo, Long productId, String productName, String productSku, String packSize, String unit,
             BigDecimal unitPrice, int quantity) {
        this.bill = bill;
        this.lineNo = lineNo;
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.packSize = packSize;
        this.unit = unit;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() { return id; }
    public int getLineNo() { return lineNo; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductSku() { return productSku; }
    public String getPackSize() { return packSize; }
    public String getUnit() { return unit; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getLineTotal() { return lineTotal; }
}
