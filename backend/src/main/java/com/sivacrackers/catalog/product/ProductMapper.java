package com.sivacrackers.catalog.product;

import com.sivacrackers.catalog.brand.Brand;
import com.sivacrackers.catalog.category.Category;
import com.sivacrackers.catalog.product.dto.ProductDtos.CategoryWithDescription;
import com.sivacrackers.catalog.product.dto.ProductDtos.ImageDto;
import com.sivacrackers.catalog.product.dto.ProductDtos.ProductDetail;
import com.sivacrackers.catalog.product.dto.ProductDtos.ProductSummary;
import com.sivacrackers.common.dto.RefDto;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/** Entity → public DTO. Call inside a read transaction (images and safety notes are loaded lazily). */
public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductSummary toSummary(Product p) {
        return new ProductSummary(
                p.getId(), p.getName(), p.getSlug(), p.getSku(), p.getDescription(), p.getPackSize(), p.getUnit(),
                p.getMrp(), p.getPrice(), p.getStockStatus(), p.isFeatured(), addedOn(p), images(p), p.isDemo(),
                ref(p.getCategory()), ref(p.getBrand()));
    }

    public static ProductDetail toDetail(Product p) {
        Category c = p.getCategory();
        return new ProductDetail(
                p.getId(), p.getName(), p.getSlug(), p.getSku(), p.getDescription(), p.getPackSize(), p.getUnit(),
                p.getMrp(), p.getPrice(), p.getStockStatus(), p.isFeatured(), addedOn(p), images(p), p.isDemo(),
                new CategoryWithDescription(c.getId(), c.getName(), c.getSlug(), c.getDescription()),
                ref(p.getBrand()),
                List.copyOf(c.getSafetyNotes()));
    }

    public static RefDto ref(Category c) {
        return new RefDto(c.getId(), c.getName(), c.getSlug());
    }

    public static RefDto ref(Brand b) {
        return new RefDto(b.getId(), b.getName(), b.getSlug());
    }

    private static List<ImageDto> images(Product p) {
        return p.getImages().stream().map(i -> new ImageDto(i.getUrl(), i.getAltText())).toList();
    }

    /** Date the product was added (the website shows dates, not times). */
    private static LocalDate addedOn(Product p) {
        return p.getCreatedAt() == null ? null : LocalDate.ofInstant(p.getCreatedAt(), ZoneOffset.UTC);
    }
}
