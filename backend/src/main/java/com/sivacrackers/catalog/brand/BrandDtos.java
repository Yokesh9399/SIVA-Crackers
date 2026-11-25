package com.sivacrackers.catalog.brand;

/** Public brand JSON (matches the Phase 1–3 frontend; productCount = visible products). */
public final class BrandDtos {

    private BrandDtos() {
    }

    public record BrandSummary(Long id, String name, String slug, String description, String logoUrl,
                               int displayOrder, boolean active, boolean isDemo, long productCount) {
    }
}
