package com.sivacrackers.catalog.brand;

import com.sivacrackers.catalog.brand.BrandDtos.BrandSummary;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public brands. Products of a brand: GET /api/public/products?brands={slug}. */
@RestController
@RequestMapping("/api/public/brands")
public class PublicBrandController {

    private final BrandService service;

    public PublicBrandController(BrandService service) {
        this.service = service;
    }

    @GetMapping
    public List<BrandSummary> list() {
        return service.list();
    }

    @GetMapping("/{slug}")
    public BrandSummary detail(
            @PathVariable @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                    message = "must contain only lower-case letters, digits and hyphens") String slug) {
        return service.detail(slug);
    }
}
