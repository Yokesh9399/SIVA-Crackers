package com.sivacrackers.catalog.brand;

import com.sivacrackers.catalog.brand.BrandDtos.BrandSummary;
import com.sivacrackers.catalog.product.ProductRepository;
import com.sivacrackers.common.exception.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BrandService {

    private final BrandRepository brands;
    private final ProductRepository products;

    public BrandService(BrandRepository brands, ProductRepository products) {
        this.brands = brands;
        this.products = products;
    }

    /** Active brands in display order, each listed once, with its number of visible products. */
    public List<BrandSummary> list() {
        Map<Long, Long> counts = counts();
        return brands.findAllByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .map(b -> toDto(b, counts))
                .toList();
    }

    /** One active brand; 404 for unknown or inactive. */
    public BrandSummary detail(String slug) {
        Brand b = brands.findBySlug(slug).filter(Brand::isActive)
                .orElseThrow(() -> new NotFoundException("Brand not found"));
        return toDto(b, counts());
    }

    private static BrandSummary toDto(Brand b, Map<Long, Long> counts) {
        return new BrandSummary(b.getId(), b.getName(), b.getSlug(), b.getDescription(), b.getLogoUrl(),
                b.getDisplayOrder(), b.isActive(), b.isDemo(), counts.getOrDefault(b.getId(), 0L));
    }

    private Map<Long, Long> counts() {
        return products.countVisibleByBrand().stream().collect(Collectors.toMap(
                ProductRepository.CountByParent::getParentId, ProductRepository.CountByParent::getProductCount));
    }
}
