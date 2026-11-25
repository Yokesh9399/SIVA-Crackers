package com.sivacrackers.catalog.category;

import com.sivacrackers.catalog.category.CategoryDtos.CategoryDetail;
import com.sivacrackers.catalog.category.CategoryDtos.CategorySummary;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public categories. Products of a category: GET /api/public/products?categories={slug}.
 */
@RestController
@RequestMapping("/api/public/categories")
public class PublicCategoryController {

    private final CategoryService service;

    public PublicCategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategorySummary> list() {
        return service.list();
    }

    /** One category with its safety information. */
    @GetMapping("/{slug}")
    public CategoryDetail detail(
            @PathVariable @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                    message = "must contain only lower-case letters, digits and hyphens") String slug) {
        return service.detail(slug);
    }
}
