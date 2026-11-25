package com.sivacrackers.catalog.product;

import com.sivacrackers.catalog.brand.Brand;
import com.sivacrackers.catalog.category.Category;
import com.sivacrackers.common.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A catalogue product. Prices are indicative (shown on the website, confirmed at the shop);
 * the database guarantees 0 ≤ price ≤ MRP and stock ≥ 0.
 */
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 170)
    private String slug;

    /** Product code shown to visitors. */
    @Column(nullable = false, length = 40)
    private String sku;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(length = 2000)
    private String description;

    @Column(name = "pack_size", nullable = false, length = 50)
    private String packSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ProductUnit unit;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal mrp;

    /** Indicative selling price. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "reorder_level", nullable = false)
    private int reorderLevel = 10;

    @Column(nullable = false)
    private boolean featured;

    @Column(name = "visible_on_website", nullable = false)
    private boolean visibleOnWebsite = true;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "is_demo", nullable = false)
    private boolean demo;

    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "display_order")
    private List<ProductImage> images = new ArrayList<>();

    protected Product() {
        // for JPA
    }

    public Product(String name, String slug, String sku, Category category, Brand brand,
                   String packSize, ProductUnit unit, BigDecimal mrp, BigDecimal price) {
        this.name = name;
        this.slug = slug;
        this.sku = sku;
        this.category = category;
        this.brand = brand;
        this.packSize = packSize;
        this.unit = unit;
        this.mrp = mrp;
        this.price = price;
    }

    /** Calculated, never stored. */
    public StockStatus getStockStatus() {
        return StockStatus.of(stockQuantity, reorderLevel);
    }

    /** Shown on the public website only when both active and visible. */
    public boolean isPubliclyVisible() {
        return active && visibleOnWebsite;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Brand getBrand() { return brand; }
    public void setBrand(Brand brand) { this.brand = brand; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPackSize() { return packSize; }
    public void setPackSize(String packSize) { this.packSize = packSize; }
    public ProductUnit getUnit() { return unit; }
    public void setUnit(ProductUnit unit) { this.unit = unit; }
    public BigDecimal getMrp() { return mrp; }
    public void setMrp(BigDecimal mrp) { this.mrp = mrp; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public boolean isVisibleOnWebsite() { return visibleOnWebsite; }
    public void setVisibleOnWebsite(boolean visibleOnWebsite) { this.visibleOnWebsite = visibleOnWebsite; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isDemo() { return demo; }
    public void setDemo(boolean demo) { this.demo = demo; }
    public List<ProductImage> getImages() { return images; }
}
