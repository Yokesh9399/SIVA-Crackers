package com.sivacrackers.catalog.product;

import com.sivacrackers.catalog.category.Category;
import com.sivacrackers.catalog.category.CategoryRepository;
import com.sivacrackers.catalog.product.dto.ProductDtos.PriceListGroup;
import com.sivacrackers.catalog.product.dto.ProductDtos.PriceRange;
import com.sivacrackers.catalog.product.dto.ProductDtos.ProductDetail;
import com.sivacrackers.catalog.product.dto.ProductDtos.ProductSummary;
import com.sivacrackers.common.dto.PageResponse;
import com.sivacrackers.common.exception.NotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only public catalogue: listing, search, detail, related, Estimate List lookup and price list. */
@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository products;
    private final CategoryRepository categories;

    public ProductService(ProductRepository products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    public PageResponse<ProductSummary> search(ProductQuery q) {
        var page = products.findAll(ProductSpecifications.catalogue(q), PageRequest.of(q.page(), q.size(), q.sort().sort()));
        return PageResponse.of(page, ProductMapper::toSummary);
    }

    /** Featured products in the shop's product order (homepage). */
    public List<ProductSummary> featured(int limit) {
        return products.findAll(ProductSpecifications.visibleProducts(true), PageRequest.of(0, limit, Sort.by("id")))
                .map(ProductMapper::toSummary).getContent();
    }

    /** Most recently added products (homepage "New arrivals"). */
    public List<ProductSummary> newArrivals(int limit) {
        return products.findAll(ProductSpecifications.visibleProducts(false), PageRequest.of(0, limit, ProductSort.NEWEST.sort()))
                .map(ProductMapper::toSummary).getContent();
    }

    /** Detail page; 404 for unknown or hidden products (hidden products are indistinguishable from missing). */
    public ProductDetail detail(String slug) {
        return ProductMapper.toDetail(findVisible(slug));
    }

    /** Same category first, then the same brand from other categories (product order), up to limit. */
    public List<ProductSummary> related(String slug, int limit) {
        Product p = findVisible(slug);
        Long categoryId = p.getCategory().getId();
        Long brandId = p.getBrand().getId();
        List<Product> result = new ArrayList<>(products.findAll(
                ProductSpecifications.related(categoryId, brandId, p.getId(), true), PageRequest.of(0, limit, Sort.by("id")))
                .getContent());
        if (result.size() < limit) {
            result.addAll(products.findAll(
                    ProductSpecifications.related(categoryId, brandId, p.getId(), false),
                    PageRequest.of(0, limit - result.size(), Sort.by("id"))).getContent());
        }
        return result.stream().map(ProductMapper::toSummary).toList();
    }

    /** Estimate List lookup: visible products for the given slugs, in the order asked; unknown slugs skipped. */
    public List<ProductSummary> bySlugs(List<String> slugs) {
        if (slugs.isEmpty()) {
            return List.of();
        }
        Map<String, Product> found = products.findAllBySlugIn(slugs).stream()
                .filter(ProductService::isVisible)
                .collect(Collectors.toMap(Product::getSlug, Function.identity()));
        return slugs.stream().distinct().map(found::get).filter(java.util.Objects::nonNull)
                .map(ProductMapper::toSummary).toList();
    }

    /** Every visible product grouped by active category, in display order (Price List page). */
    public List<PriceListGroup> priceList() {
        List<Product> all = products.findAll(ProductSpecifications.visibleProducts(false), ProductSort.DEFAULT.sort());
        Map<Long, List<ProductSummary>> byCategory = new LinkedHashMap<>();
        for (Product p : all) {
            byCategory.computeIfAbsent(p.getCategory().getId(), k -> new ArrayList<>()).add(ProductMapper.toSummary(p));
        }
        List<PriceListGroup> groups = new ArrayList<>();
        for (Category c : categories.findAllByActiveTrueOrderByDisplayOrderAscIdAsc()) {
            List<ProductSummary> list = byCategory.get(c.getId());
            if (list != null && !list.isEmpty()) {
                groups.add(new PriceListGroup(ProductMapper.ref(c), list));
            }
        }
        return groups;
    }

    public PriceRange priceRange() {
        ProductRepository.PriceRangeView range = products.findVisiblePriceRange();
        return range == null ? new PriceRange(null, null) : new PriceRange(range.getMin(), range.getMax());
    }

    private Product findVisible(String slug) {
        return products.findBySlug(slug)
                .filter(ProductService::isVisible)
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    private static boolean isVisible(Product p) {
        return p.isPubliclyVisible() && p.getCategory().isActive() && p.getBrand().isActive();
    }
}
