package com.sivacrackers.catalog.brand.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** OWNER brand management ({@code /api/admin/brands}). */
public final class AdminBrandDtos {

    private AdminBrandDtos() {
    }

    public record BrandRequest(
            @NotBlank(message = "is required") @Size(max = 80, message = "must be at most 80 characters") String name,
            @Size(max = 100, message = "must be at most 100 characters")
            @Pattern(regexp = "^$|^[a-z0-9]+(-[a-z0-9]+)*$", message = "may contain only lower-case letters, numbers and single hyphens") String slug,
            @Size(max = 500, message = "must be at most 500 characters") String description,
            @Size(max = 500, message = "is too long") String logoUrl,
            @NotNull(message = "is required") @Min(value = 0, message = "must not be negative") @Max(value = 9999, message = "is too large") Integer displayOrder,
            @NotNull(message = "is required") Boolean active) {
    }

    public record AdminBrand(
            Long id, String name, String slug, String description, String logoUrl, int displayOrder, boolean active,
            boolean isDemo, long productCount, long visibleProductCount, Instant createdAt, Instant updatedAt) {
    }
}
