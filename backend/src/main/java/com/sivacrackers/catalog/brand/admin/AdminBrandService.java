package com.sivacrackers.catalog.brand.admin;

import com.sivacrackers.catalog.brand.Brand;
import com.sivacrackers.catalog.brand.BrandRepository;
import com.sivacrackers.catalog.brand.admin.AdminBrandDtos.AdminBrand;
import com.sivacrackers.catalog.brand.admin.AdminBrandDtos.BrandRequest;
import com.sivacrackers.catalog.product.ProductRepository;
import com.sivacrackers.catalog.product.ProductRepository.CountByParent;
import com.sivacrackers.common.Slugs;
import com.sivacrackers.common.exception.ConflictException;
import com.sivacrackers.common.exception.InvalidRequestException;
import com.sivacrackers.common.exception.NotFoundException;
import com.sivacrackers.media.ImageCleanup;
import com.sivacrackers.media.ImageFolder;
import com.sivacrackers.media.ImageStorage;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Brand management. A brand that still has products cannot be deleted – it can be made inactive instead. */
@Service
public class AdminBrandService {

    private final BrandRepository brands;
    private final ProductRepository products;
    private final ImageCleanup imageCleanup;

    public AdminBrandService(BrandRepository brands, ProductRepository products, ImageCleanup imageCleanup) {
        this.brands = brands;
        this.products = products;
        this.imageCleanup = imageCleanup;
    }

    @Transactional(readOnly = true)
    public List<AdminBrand> list(String search) {
        Map<Long, Long> all = counts(products.countAllByBrand());
        Map<Long, Long> visible = counts(products.countVisibleByBrand());
        String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return brands.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .filter(b -> q.isEmpty() || b.getName().toLowerCase(Locale.ROOT).contains(q) || b.getSlug().contains(q))
                .map(b -> toAdmin(b, all.getOrDefault(b.getId(), 0L), visible.getOrDefault(b.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminBrand get(Long id) {
        return toAdmin(find(id), products.countByBrandId(id), visibleCount(id));
    }

    @Transactional
    public AdminBrand create(BrandRequest req) {
        String name = req.name().trim();
        String slug = slugFor(req.slug(), name);
        if (brands.existsByNameIgnoreCase(name)) {
            throw new ConflictException("name", "is already used by another brand");
        }
        if (brands.existsBySlug(slug)) {
            throw new ConflictException("slug", "is already used by another brand");
        }
        Brand b = new Brand(name, slug);
        apply(b, req);
        return toAdmin(save(b), 0, 0);
    }

    @Transactional
    public AdminBrand update(Long id, BrandRequest req) {
        Brand b = find(id);
        String name = req.name().trim();
        String slug = slugFor(req.slug(), name);
        if (brands.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("name", "is already used by another brand");
        }
        if (brands.existsBySlugAndIdNot(slug, id)) {
            throw new ConflictException("slug", "is already used by another brand");
        }
        String oldLogo = b.getLogoUrl();
        b.setName(name);
        b.setSlug(slug);
        apply(b, req);
        Brand saved = save(b);
        imageCleanup.deleteIfUnusedAfterCommit(Collections.singletonList(oldLogo));
        return toAdmin(saved, products.countByBrandId(id), visibleCount(id));
    }

    @Transactional
    public void delete(Long id) {
        Brand b = find(id);
        long used = products.countByBrandId(id);
        if (used > 0) {
            throw new ConflictException("“" + b.getName() + "” still has " + used + (used == 1 ? " product" : " products")
                    + ". Move them to another brand or delete them first – or mark the brand inactive to hide it.");
        }
        String logo = b.getLogoUrl();
        brands.delete(b);
        brands.flush();
        imageCleanup.deleteIfUnusedAfterCommit(Collections.singletonList(logo));
    }

    private void apply(Brand b, BrandRequest req) {
        String logo = blankToNull(req.logoUrl());
        if (logo != null && !ImageStorage.isUploadUrl(logo, ImageFolder.BRANDS)) {
            throw new InvalidRequestException("logoUrl", "must be a picture uploaded through the admin");
        }
        b.setDescription(blankToNull(req.description()));
        b.setLogoUrl(logo);
        b.setDisplayOrder(req.displayOrder());
        b.setActive(req.active());
    }

    private Brand save(Brand b) {
        try {
            return brands.saveAndFlush(b);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("This brand name is already used. Please choose another.");
        }
    }

    private String slugFor(String requested, String name) {
        String slug = requested == null || requested.isBlank() ? Slugs.from(name, 100) : requested.trim();
        if (slug.isEmpty()) {
            throw new InvalidRequestException("name", "must contain letters or numbers");
        }
        return slug;
    }

    private Brand find(Long id) {
        return brands.findById(id).orElseThrow(() -> new NotFoundException("Brand not found."));
    }

    private long visibleCount(Long id) {
        return counts(products.countVisibleByBrand()).getOrDefault(id, 0L);
    }

    private static Map<Long, Long> counts(List<CountByParent> rows) {
        return rows.stream().collect(Collectors.toMap(CountByParent::getParentId, CountByParent::getProductCount));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static AdminBrand toAdmin(Brand b, long productCount, long visibleCount) {
        return new AdminBrand(b.getId(), b.getName(), b.getSlug(), b.getDescription(), b.getLogoUrl(), b.getDisplayOrder(),
                b.isActive(), b.isDemo(), productCount, visibleCount, b.getCreatedAt(), b.getUpdatedAt());
    }
}
