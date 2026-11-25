package com.sivacrackers.catalog.product;

import com.sivacrackers.catalog.product.dto.ProductDtos.PriceListGroup;
import com.sivacrackers.catalog.product.dto.ProductDtos.PriceRange;
import com.sivacrackers.catalog.product.dto.ProductDtos.ProductDetail;
import com.sivacrackers.catalog.product.dto.ProductDtos.ProductSummary;
import com.sivacrackers.common.dto.PageResponse;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, read-only product endpoints used by the website (no login).
 * Query parameters follow the frontend: comma-separated slugs, 0-based page, size 12/24/48,
 * sort = default | name,asc | name,desc | price,asc | price,desc | newest.
 */
@RestController
@RequestMapping("/api/public")
public class PublicProductController {

    static final String SLUG = "^[a-z0-9]+(-[a-z0-9]+)*$";
    static final String SLUG_MESSAGE = "must contain only lower-case letters, digits and hyphens";

    private final ProductService service;

    public PublicProductController(ProductService service) {
        this.service = service;
    }

    /** Catalogue with search, filters, sorting and pagination. */
    @GetMapping("/products")
    public PageResponse<ProductSummary> products(
            @RequestParam(required = false) @Size(max = 100, message = "must be at most 100 characters") String search,
            @RequestParam(required = false) @Size(max = 48, message = "at most 48 categories")
            List<@Pattern(regexp = SLUG, message = SLUG_MESSAGE) String> categories,
            @RequestParam(required = false) @Size(max = 48, message = "at most 48 brands")
            List<@Pattern(regexp = SLUG, message = SLUG_MESSAGE) String> brands,
            @RequestParam(required = false) @DecimalMin(value = "0", message = "must not be negative") BigDecimal minPrice,
            @RequestParam(required = false) @DecimalMin(value = "0", message = "must not be negative") BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) @Min(value = 0, message = "must not be negative") Integer page,
            @RequestParam(required = false) Integer size) {
        return service.search(ProductQuery.of(search, categories, brands, minPrice, maxPrice, inStock, sort, page, size));
    }

    @GetMapping("/products/featured")
    public List<ProductSummary> featured(
            @RequestParam(defaultValue = "8") @Min(1) @Max(12) int limit) {
        return service.featured(limit);
    }

    @GetMapping("/products/new-arrivals")
    public List<ProductSummary> newArrivals(
            @RequestParam(defaultValue = "8") @Min(1) @Max(12) int limit) {
        return service.newArrivals(limit);
    }

    /** Products for the browser-only Estimate List. Unknown or hidden slugs are skipped. */
    @GetMapping("/products/by-slugs")
    public List<ProductSummary> bySlugs(
            @RequestParam(defaultValue = "") @Size(max = 200, message = "at most 200 products")
            List<@Pattern(regexp = SLUG, message = SLUG_MESSAGE) String> slugs) {
        return service.bySlugs(slugs.stream().filter(s -> !s.isBlank()).toList());
    }

    @GetMapping("/products/{slug}")
    public ProductDetail detail(@PathVariable @Pattern(regexp = SLUG, message = SLUG_MESSAGE) String slug) {
        return service.detail(slug);
    }

    @GetMapping("/products/{slug}/related")
    public List<ProductSummary> related(
            @PathVariable @Pattern(regexp = SLUG, message = SLUG_MESSAGE) String slug,
            @RequestParam(defaultValue = "4") @Min(1) @Max(12) int limit) {
        return service.related(slug, limit);
    }

    /** Every visible product grouped by category (Price List page and its print layout). */
    @GetMapping("/price-list")
    public List<PriceListGroup> priceList() {
        return service.priceList();
    }

    @GetMapping("/price-range")
    public PriceRange priceRange() {
        return service.priceRange();
    }
}
