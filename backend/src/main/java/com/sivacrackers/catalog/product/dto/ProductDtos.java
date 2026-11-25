package com.sivacrackers.catalog.product.dto;

import com.sivacrackers.catalog.product.ProductUnit;
import com.sivacrackers.catalog.product.StockStatus;
import com.sivacrackers.common.dto.RefDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Public product JSON. Field names and nesting match the Phase 1–3 frontend exactly.
 * Internal fields (stock quantity, reorder level, visibility flags, update time) are never exposed.
 */
public final class ProductDtos {

    private ProductDtos() {
    }

    /** One product image: {@code { url, alt }}. */
    public record ImageDto(String url, String alt) {
    }

    /** Product in lists, cards, related products and the Estimate List. */
    public record ProductSummary(
            Long id,
            String name,
            String slug,
            String sku,
            String description,
            String packSize,
            ProductUnit unit,
            BigDecimal mrp,
            BigDecimal price,
            StockStatus stockStatus,
            boolean featured,
            LocalDate createdAt,
            List<ImageDto> images,
            boolean isDemo,
            RefDto category,
            RefDto brand) {
    }

    /** Category reference on the detail page also carries its description. */
    public record CategoryWithDescription(Long id, String name, String slug, String description) {
    }

    /** Product detail page: the summary fields + category description + category safety notes. */
    public record ProductDetail(
            Long id,
            String name,
            String slug,
            String sku,
            String description,
            String packSize,
            ProductUnit unit,
            BigDecimal mrp,
            BigDecimal price,
            StockStatus stockStatus,
            boolean featured,
            LocalDate createdAt,
            List<ImageDto> images,
            boolean isDemo,
            CategoryWithDescription category,
            RefDto brand,
            List<String> safetyNotes) {
    }

    /** Lowest / highest indicative price of visible products (both null if there are none). */
    public record PriceRange(BigDecimal min, BigDecimal max) {
    }

    /** One category section of the Price List. */
    public record PriceListGroup(RefDto category, List<ProductSummary> products) {
    }
}
