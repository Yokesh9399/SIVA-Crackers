package com.sivacrackers.catalog.category;

import java.util.List;

/** Public category JSON (matches the Phase 1–3 frontend; productCount = visible products). */
public final class CategoryDtos {

    private CategoryDtos() {
    }

    public record CategorySummary(Long id, String name, String slug, String description, String imageUrl,
                                  int displayOrder, boolean active, boolean isDemo, long productCount) {
    }

    /** One category with its safety information (GET /api/public/categories/{slug}). */
    public record CategoryDetail(Long id, String name, String slug, String description, String imageUrl,
                                 int displayOrder, boolean active, boolean isDemo, long productCount,
                                 List<String> safetyNotes) {
    }
}
