package com.sivacrackers.catalog.category;

import com.sivacrackers.catalog.category.CategoryDtos.CategoryDetail;
import com.sivacrackers.catalog.category.CategoryDtos.CategorySummary;
import com.sivacrackers.catalog.product.ProductRepository;
import com.sivacrackers.common.exception.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categories;
    private final ProductRepository products;

    public CategoryService(CategoryRepository categories, ProductRepository products) {
        this.categories = categories;
        this.products = products;
    }

    /** Active categories in display order, each with its number of visible products. */
    public List<CategorySummary> list() {
        Map<Long, Long> counts = counts();
        return categories.findAllByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .map(c -> new CategorySummary(c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.getImageUrl(),
                        c.getDisplayOrder(), c.isActive(), c.isDemo(), counts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    /** One active category with its safety notes; 404 for unknown or inactive. */
    public CategoryDetail detail(String slug) {
        Category c = categories.findBySlug(slug).filter(Category::isActive)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        return new CategoryDetail(c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.getImageUrl(),
                c.getDisplayOrder(), c.isActive(), c.isDemo(), counts().getOrDefault(c.getId(), 0L),
                List.copyOf(c.getSafetyNotes()));
    }

    private Map<Long, Long> counts() {
        return products.countVisibleByCategory().stream().collect(Collectors.toMap(
                ProductRepository.CountByParent::getParentId, ProductRepository.CountByParent::getProductCount));
    }
}
