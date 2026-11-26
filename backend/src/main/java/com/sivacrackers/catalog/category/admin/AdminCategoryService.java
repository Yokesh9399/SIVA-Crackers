package com.sivacrackers.catalog.category.admin;

import com.sivacrackers.catalog.category.Category;
import com.sivacrackers.catalog.category.CategoryRepository;
import com.sivacrackers.catalog.category.admin.AdminCategoryDtos.AdminCategory;
import com.sivacrackers.catalog.category.admin.AdminCategoryDtos.CategoryRequest;
import com.sivacrackers.catalog.product.ProductRepository;
import com.sivacrackers.catalog.product.ProductRepository.CountByParent;
import com.sivacrackers.common.Slugs;
import com.sivacrackers.common.exception.ConflictException;
import com.sivacrackers.common.exception.InvalidRequestException;
import com.sivacrackers.common.exception.NotFoundException;
import com.sivacrackers.audit.AuditService;
import com.sivacrackers.media.ImageCleanup;
import com.sivacrackers.media.ImageFolder;
import com.sivacrackers.media.ImageStorage;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Category management. A category that still has products cannot be deleted – it can be made inactive instead. */
@Service
public class AdminCategoryService {

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final ImageCleanup imageCleanup;
    private final AuditService audit;

    public AdminCategoryService(CategoryRepository categories, ProductRepository products, ImageCleanup imageCleanup,
                                AuditService audit) {
        this.categories = categories;
        this.products = products;
        this.imageCleanup = imageCleanup;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<AdminCategory> list(String search) {
        Map<Long, Long> all = counts(products.countAllByCategory());
        Map<Long, Long> visible = counts(products.countVisibleByCategory());
        String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return categories.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .filter(c -> q.isEmpty() || c.getName().toLowerCase(Locale.ROOT).contains(q) || c.getSlug().contains(q))
                .map(c -> toAdmin(c, all.getOrDefault(c.getId(), 0L), visible.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminCategory get(Long id) {
        Category c = find(id);
        return toAdmin(c, products.countByCategoryId(id), visibleCount(id));
    }

    @Transactional
    public AdminCategory create(CategoryRequest req) {
        String name = req.name().trim();
        String slug = slugFor(req.slug(), name);
        if (categories.existsByNameIgnoreCase(name)) {
            throw new ConflictException("name", "is already used by another category");
        }
        if (categories.existsBySlug(slug)) {
            throw new ConflictException("slug", "is already used by another category");
        }
        Category c = new Category(name, slug);
        apply(c, req);
        Category saved = save(c);
        return toAdmin(saved, 0, 0);
    }

    @Transactional
    public AdminCategory update(Long id, CategoryRequest req) {
        Category c = find(id);
        String name = req.name().trim();
        String slug = slugFor(req.slug(), name);
        if (categories.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("name", "is already used by another category");
        }
        if (categories.existsBySlugAndIdNot(slug, id)) {
            throw new ConflictException("slug", "is already used by another category");
        }
        String oldImage = c.getImageUrl();
        c.setName(name);
        c.setSlug(slug);
        apply(c, req);
        Category saved = save(c);
        imageCleanup.deleteIfUnusedAfterCommit(java.util.Collections.singletonList(oldImage));
        return toAdmin(saved, products.countByCategoryId(id), visibleCount(id));
    }

    @Transactional
    public void delete(Long id) {
        Category c = find(id);
        long used = products.countByCategoryId(id);
        if (used > 0) {
            throw new ConflictException("“" + c.getName() + "” still has " + used + (used == 1 ? " product" : " products")
                    + ". Move them to another category or delete them first – or mark the category inactive to hide it.");
        }
        String image = c.getImageUrl();
        categories.delete(c);
        categories.flush();
        audit.record("CATEGORY_DELETED", "CATEGORY", id, c.getName());
        imageCleanup.deleteIfUnusedAfterCommit(java.util.Collections.singletonList(image));
    }

    private void apply(Category c, CategoryRequest req) {
        String image = blankToNull(req.imageUrl());
        if (image != null && !ImageStorage.isUploadUrl(image, ImageFolder.CATEGORIES)) {
            throw new InvalidRequestException("imageUrl", "must be a picture uploaded through the admin");
        }
        c.setDescription(blankToNull(req.description()));
        c.setImageUrl(image);
        c.setDisplayOrder(req.displayOrder());
        c.setActive(req.active());
        c.replaceSafetyNotes(req.safetyNotes().stream().map(String::trim).toList());
    }

    private Category save(Category c) {
        try {
            return categories.saveAndFlush(c);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("This category name is already used. Please choose another.");
        }
    }

    private String slugFor(String requested, String name) {
        String slug = requested == null || requested.isBlank() ? Slugs.from(name, 100) : requested.trim();
        if (slug.isEmpty()) {
            throw new InvalidRequestException("name", "must contain letters or numbers");
        }
        return slug;
    }

    private Category find(Long id) {
        return categories.findWithNotesById(id).orElseThrow(() -> new NotFoundException("Category not found."));
    }

    private long visibleCount(Long id) {
        return counts(products.countVisibleByCategory()).getOrDefault(id, 0L);
    }

    private static Map<Long, Long> counts(List<CountByParent> rows) {
        return rows.stream().collect(Collectors.toMap(CountByParent::getParentId, CountByParent::getProductCount));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static AdminCategory toAdmin(Category c, long productCount, long visibleCount) {
        return new AdminCategory(c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.getImageUrl(),
                c.getDisplayOrder(), c.isActive(), c.isDemo(), List.copyOf(c.getSafetyNotes()), productCount, visibleCount,
                c.getCreatedAt(), c.getUpdatedAt());
    }
}
