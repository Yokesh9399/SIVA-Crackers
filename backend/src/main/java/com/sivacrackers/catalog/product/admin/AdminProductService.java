package com.sivacrackers.catalog.product.admin;

import com.sivacrackers.catalog.brand.Brand;
import com.sivacrackers.catalog.brand.BrandRepository;
import com.sivacrackers.catalog.category.Category;
import com.sivacrackers.catalog.category.CategoryRepository;
import com.sivacrackers.catalog.product.Product;
import com.sivacrackers.catalog.product.ProductImage;
import com.sivacrackers.catalog.product.ProductMapper;
import com.sivacrackers.catalog.product.ProductRepository;
import com.sivacrackers.catalog.product.admin.AdminProductDtos.AdminProduct;
import com.sivacrackers.catalog.product.admin.AdminProductDtos.AdminProductRow;
import com.sivacrackers.catalog.product.admin.AdminProductDtos.ImageDto;
import com.sivacrackers.catalog.product.admin.AdminProductDtos.ImageInput;
import com.sivacrackers.catalog.product.admin.AdminProductDtos.ProductRequest;
import com.sivacrackers.catalog.product.admin.AdminProductDtos.QuickUpdateRequest;
import com.sivacrackers.common.Slugs;
import com.sivacrackers.common.dto.PageResponse;
import com.sivacrackers.common.exception.ConflictException;
import com.sivacrackers.common.exception.InvalidRequestException;
import com.sivacrackers.common.exception.NotFoundException;
import com.sivacrackers.audit.AuditService;
import com.sivacrackers.inventory.InventoryService;
import com.sivacrackers.media.ImageCleanup;
import com.sivacrackers.media.ImageFolder;
import com.sivacrackers.media.ImageStorage;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The OWNER's product management. New products are never demo data; edits keep an item's demo label. */
@Service
public class AdminProductService {

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final ImageCleanup imageCleanup;
    private final InventoryService inventory;
    private final AuditService audit;

    public AdminProductService(ProductRepository products, CategoryRepository categories, BrandRepository brands,
                               ImageCleanup imageCleanup, InventoryService inventory, AuditService audit) {
        this.products = products;
        this.categories = categories;
        this.brands = brands;
        this.imageCleanup = imageCleanup;
        this.inventory = inventory;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminProductRow> list(AdminProductQuery q) {
        var page = products.findAll(AdminProductSpecifications.matching(q), PageRequest.of(q.page(), q.size(), q.sort().sort()));
        return PageResponse.of(page, AdminProductService::toRow);
    }

    @Transactional(readOnly = true)
    public AdminProduct get(Long id) {
        return toAdmin(find(id));
    }

    @Transactional
    public AdminProduct create(ProductRequest req) {
        String name = req.name().trim();
        String slug = slugFor(req.slug(), name);
        String sku = req.sku().trim();
        if (products.existsBySlug(slug)) {
            throw new ConflictException("slug", "is already used by another product");
        }
        if (products.existsBySku(sku)) {
            throw new ConflictException("sku", "is already used by another product");
        }
        Product p = new Product(name, slug, sku, category(req.categoryId()), brand(req.brandId()),
                req.packSize().trim(), req.unit(), req.mrp(), req.price());
        apply(p, req);
        p.setStockQuantity(0);
        Product saved = save(p);
        inventory.recordFormChange(saved, 0, req.stockQuantity(), true);
        audit.record("PRODUCT_CREATED", "PRODUCT", saved.getId(), saved.getName() + " (" + saved.getSku() + ") ₹" + saved.getPrice().toPlainString());
        return toAdmin(saved);
    }

    @Transactional
    public AdminProduct update(Long id, ProductRequest req) {
        inventory.lock(id);
        Product p = find(id);
        int previousStock = p.getStockQuantity();
        String name = req.name().trim();
        String slug = slugFor(req.slug(), name);
        String sku = req.sku().trim();
        if (products.existsBySlugAndIdNot(slug, id)) {
            throw new ConflictException("slug", "is already used by another product");
        }
        if (products.existsBySkuAndIdNot(sku, id)) {
            throw new ConflictException("sku", "is already used by another product");
        }
        List<String> oldImages = p.getImages().stream().map(ProductImage::getUrl).toList();
        p.setName(name);
        p.setSlug(slug);
        p.setSku(sku);
        p.setCategory(category(req.categoryId()));
        p.setBrand(brand(req.brandId()));
        p.setPackSize(req.packSize().trim());
        p.setUnit(req.unit());
        p.setMrp(req.mrp());
        p.setPrice(req.price());
        apply(p, req);
        p.setStockQuantity(previousStock);
        Product saved = save(p);
        inventory.recordFormChange(saved, previousStock, req.stockQuantity(), false);
        audit.record("PRODUCT_UPDATED", "PRODUCT", id, saved.getName() + " (" + saved.getSku() + ") ₹" + saved.getPrice().toPlainString()
                + (saved.isActive() ? "" : ", inactive") + (saved.isVisibleOnWebsite() ? "" : ", hidden"));
        imageCleanup.deleteIfUnusedAfterCommit(oldImages);
        return toAdmin(saved);
    }

    /** Show/hide, discontinue, feature or change stock straight from the list. */
    @Transactional
    public AdminProduct quickUpdate(Long id, QuickUpdateRequest req) {
        inventory.lock(id);
        Product p = find(id);
        if (req.visibleOnWebsite() != null) {
            p.setVisibleOnWebsite(req.visibleOnWebsite());
        }
        if (req.active() != null) {
            p.setActive(req.active());
        }
        if (req.featured() != null) {
            p.setFeatured(req.featured());
        }
        Product saved = products.saveAndFlush(p);
        if (req.stockQuantity() != null) {
            inventory.recordFormChange(saved, saved.getStockQuantity(), req.stockQuantity(), false);
        }
        audit.record("PRODUCT_UPDATED", "PRODUCT", id, saved.getName() + ": quick change "
                + (req.visibleOnWebsite() != null ? "visible=" + req.visibleOnWebsite() + " " : "")
                + (req.active() != null ? "active=" + req.active() + " " : "")
                + (req.featured() != null ? "featured=" + req.featured() + " " : "")
                + (req.stockQuantity() != null ? "stock=" + req.stockQuantity() : ""));
        return toAdmin(saved);
    }

    /**
     * Deletes a product and its image list. Nothing else in the database refers to products
     * (Estimate Lists live only in visitors' browsers and simply skip products that no longer exist).
     */
    @Transactional
    public void delete(Long id) {
        Product p = find(id);
        List<String> images = p.getImages().stream().map(ProductImage::getUrl).toList();
        products.delete(p);
        products.flush();
        audit.record("PRODUCT_DELETED", "PRODUCT", id, p.getName() + " (" + p.getSku() + ")");
        imageCleanup.deleteIfUnusedAfterCommit(images);
    }

    // ------------------------------------------------------------------ helpers

    private void apply(Product p, ProductRequest req) {
        if (req.price().compareTo(req.mrp()) > 0) {
            throw new InvalidRequestException("price", "must not be more than the MRP");
        }
        p.setDescription(blankToNull(req.description()));
        p.setReorderLevel(req.reorderLevel());
        p.setFeatured(req.featured());
        p.setVisibleOnWebsite(req.visibleOnWebsite());
        p.setActive(req.active());
        p.getImages().clear();
        p.getImages().addAll(images(req.images(), p.getName()));
    }

    private static List<ProductImage> images(List<ImageInput> input, String productName) {
        Set<String> seen = new LinkedHashSet<>();
        List<ProductImage> out = new ArrayList<>();
        for (ImageInput img : input) {
            String url = img.url().trim();
            if (!ImageStorage.isUploadUrl(url, ImageFolder.PRODUCTS)) {
                throw new InvalidRequestException("images", "must be pictures uploaded through the admin");
            }
            if (seen.add(url)) {
                String alt = blankToNull(img.alt());
                out.add(new ProductImage(url, alt == null ? productName : alt));
            }
        }
        return out;
    }

    private Product save(Product p) {
        try {
            return products.saveAndFlush(p);
        } catch (DataIntegrityViolationException e) {
            // A concurrent save took the same slug or code between the check and the insert.
            throw new ConflictException("This product name or code is already used. Please choose another.");
        }
    }

    private String slugFor(String requested, String name) {
        String slug = requested == null || requested.isBlank() ? Slugs.from(name, 170) : requested.trim();
        if (slug.isEmpty()) {
            throw new InvalidRequestException("name", "must contain letters or numbers");
        }
        return slug;
    }

    private Product find(Long id) {
        return products.findWithParentsById(id).orElseThrow(() -> new NotFoundException("Product not found."));
    }

    private Category category(Long id) {
        return categories.findById(id).orElseThrow(() -> new InvalidRequestException("categoryId", "is not a valid category"));
    }

    private Brand brand(Long id) {
        return brands.findById(id).orElseThrow(() -> new InvalidRequestException("brandId", "is not a valid brand"));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static boolean publiclyVisible(Product p) {
        return p.isPubliclyVisible() && p.getCategory().isActive() && p.getBrand().isActive();
    }

    static AdminProduct toAdmin(Product p) {
        return new AdminProduct(p.getId(), p.getName(), p.getSlug(), p.getSku(), ProductMapper.ref(p.getCategory()),
                ProductMapper.ref(p.getBrand()), p.getDescription(), p.getPackSize(), p.getUnit(), p.getMrp(), p.getPrice(),
                p.getStockQuantity(), p.getReorderLevel(), p.getStockStatus(), p.isFeatured(), p.isVisibleOnWebsite(),
                p.isActive(), p.isDemo(), publiclyVisible(p),
                p.getImages().stream().map(i -> new ImageDto(i.getUrl(), i.getAltText())).toList(),
                p.getCreatedAt(), p.getUpdatedAt());
    }

    public static AdminProductRow toRow(Product p) {
        String image = p.getImages().isEmpty() ? null : p.getImages().getFirst().getUrl();
        return new AdminProductRow(p.getId(), p.getName(), p.getSlug(), p.getSku(), ProductMapper.ref(p.getCategory()),
                ProductMapper.ref(p.getBrand()), p.getMrp(), p.getPrice(), p.getStockQuantity(), p.getStockStatus(),
                p.isFeatured(), p.isVisibleOnWebsite(), p.isActive(), p.isDemo(), publiclyVisible(p), image, p.getCreatedAt());
    }
}
