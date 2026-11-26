package com.sivacrackers.catalog.product.admin;

import com.sivacrackers.catalog.product.ProductUnit;
import com.sivacrackers.catalog.product.StockStatus;
import com.sivacrackers.common.dto.RefDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Request / response bodies of the OWNER's product management ({@code /api/admin/products}). */
public final class AdminProductDtos {

    private AdminProductDtos() {
    }

    static final String SLUG = "^$|^[a-z0-9]+(-[a-z0-9]+)*$";
    static final String SLUG_MESSAGE = "may contain only lower-case letters, numbers and single hyphens";
    static final String SKU = "^[A-Za-z0-9][A-Za-z0-9._/-]*$";

    public record ImageInput(
            @NotBlank(message = "is required") @Size(max = 500, message = "is too long") String url,
            @Size(max = 200, message = "must be at most 200 characters") String alt) {
    }

    /** Create or fully update a product. {@code slug} may be left empty: it is then made from the name. */
    public record ProductRequest(
            @NotBlank(message = "is required") @Size(max = 150, message = "must be at most 150 characters") String name,
            @Size(max = 170, message = "must be at most 170 characters") @Pattern(regexp = SLUG, message = SLUG_MESSAGE) String slug,
            @NotBlank(message = "is required") @Size(max = 40, message = "must be at most 40 characters")
            @Pattern(regexp = SKU, message = "may contain only letters, numbers and . _ / -") String sku,
            @NotNull(message = "is required") Long categoryId,
            @NotNull(message = "is required") Long brandId,
            @Size(max = 2000, message = "must be at most 2000 characters") String description,
            @NotBlank(message = "is required") @Size(max = 50, message = "must be at most 50 characters") String packSize,
            @NotNull(message = "is required") ProductUnit unit,
            @NotNull(message = "is required") @DecimalMin(value = "0.01", message = "must be more than 0")
            @Digits(integer = 8, fraction = 2, message = "must be a price with at most 2 decimal places") BigDecimal mrp,
            @NotNull(message = "is required") @DecimalMin(value = "0.01", message = "must be more than 0")
            @Digits(integer = 8, fraction = 2, message = "must be a price with at most 2 decimal places") BigDecimal price,
            @NotNull(message = "is required") @Min(value = 0, message = "must not be negative")
            @Max(value = 1_000_000, message = "is too large") Integer stockQuantity,
            @NotNull(message = "is required") @Min(value = 0, message = "must not be negative")
            @Max(value = 100_000, message = "is too large") Integer reorderLevel,
            @NotNull(message = "is required") Boolean featured,
            @NotNull(message = "is required") Boolean visibleOnWebsite,
            @NotNull(message = "is required") Boolean active,
            @NotNull(message = "is required") @Size(max = 6, message = "at most 6 images") List<@Valid ImageInput> images) {
    }

    /** Quick changes from the product list; only the fields that are sent are changed. */
    public record QuickUpdateRequest(
            Boolean visibleOnWebsite,
            Boolean active,
            Boolean featured,
            @Min(value = 0, message = "must not be negative") @Max(value = 1_000_000, message = "is too large") Integer stockQuantity) {
    }

    public record ImageDto(String url, String alt) {
    }

    /** Full product for the admin view and edit form. */
    public record AdminProduct(
            Long id, String name, String slug, String sku, RefDto category, RefDto brand, String description,
            String packSize, ProductUnit unit, BigDecimal mrp, BigDecimal price, int stockQuantity, int reorderLevel,
            StockStatus stockStatus, boolean featured, boolean visibleOnWebsite, boolean active, boolean isDemo,
            boolean publiclyVisible, List<ImageDto> images, Instant createdAt, Instant updatedAt) {
    }

    /** One row of the admin product list. */
    public record AdminProductRow(
            Long id, String name, String slug, String sku, RefDto category, RefDto brand, BigDecimal mrp, BigDecimal price,
            int stockQuantity, StockStatus stockStatus, boolean featured, boolean visibleOnWebsite, boolean active,
            boolean isDemo, boolean publiclyVisible, String imageUrl, Instant createdAt) {
    }
}
