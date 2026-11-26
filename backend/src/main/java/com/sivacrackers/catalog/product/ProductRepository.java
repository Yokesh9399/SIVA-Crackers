package com.sivacrackers.catalog.product;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 * Products. Catalogue filters (search, category/brand slugs, price, stock, sort, paging)
 * are built as Specifications in Phase 4.4 via {@link JpaSpecificationExecutor}.
 * "Visible" = active AND visible_on_website.
 */
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /** One product with its category and brand loaded in the same query. */
    @EntityGraph(attributePaths = {"category", "brand"})
    Optional<Product> findBySlug(String slug);

    /** For the Estimate List: the products a visitor saved, by slug. */
    @EntityGraph(attributePaths = {"category", "brand"})
    List<Product> findAllBySlugIn(Collection<String> slugs);

    boolean existsBySku(String sku);

    // ---------- Admin (Phase 5) ----------

    /** One product with category and brand, by id (admin view / edit). */
    @EntityGraph(attributePaths = {"category", "brand"})
    Optional<Product> findWithParentsById(Long id);

    boolean existsBySlug(String slug);

    // ---------- Inventory (Phase 6) ----------

    /** Locks the product row until the transaction ends, so two stock changes can never overwrite each other. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findForUpdateById(@org.springframework.data.repository.query.Param("id") Long id);

    /** Active products at or below their "few left" level but not empty, lowest stock first. */
    @EntityGraph(attributePaths = {"category", "brand"})
    @Query("select p from Product p where p.active = true and p.stockQuantity > 0 and p.stockQuantity <= p.reorderLevel order by p.stockQuantity asc, p.name asc")
    List<Product> findActiveLowStock();

    /** Active products with no stock. */
    @EntityGraph(attributePaths = {"category", "brand"})
    @Query("select p from Product p where p.active = true and p.stockQuantity = 0 order by p.name asc")
    List<Product> findActiveOutOfStock();

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsBySkuAndIdNot(String sku, Long id);

    long countByCategoryId(Long categoryId);

    long countByBrandId(Long brandId);

    long countByActiveTrue();

    long countByActiveFalse();

    long countByActiveTrueAndVisibleOnWebsiteTrue();

    long countByActiveTrueAndVisibleOnWebsiteFalse();

    long countByActiveTrueAndFeaturedTrue();

    long countByDemoTrue();

    @Query("select count(p) from Product p where p.active = true and p.stockQuantity = 0")
    long countActiveOutOfStock();

    @Query("select count(p) from Product p where p.active = true and p.stockQuantity > 0 and p.stockQuantity <= p.reorderLevel")
    long countActiveLowStock();

    /** All products (any status) per category id. */
    @Query("select p.category.id as parentId, count(p) as productCount from Product p group by p.category.id")
    List<CountByParent> countAllByCategory();

    /** All products (any status) per brand id. */
    @Query("select p.brand.id as parentId, count(p) as productCount from Product p group by p.brand.id")
    List<CountByParent> countAllByBrand();

    boolean existsByDemoTrue();

    boolean existsByDemoFalse();

    // "Visible" everywhere below = product active and on the website, and its category and brand active
    // (the same rule as ProductSpecifications).

    /** Number of visible products per category id (category tiles, filters). */
    @Query("""
            select p.category.id as parentId, count(p) as productCount
            from Product p
            where p.active = true and p.visibleOnWebsite = true
              and p.category.active = true and p.brand.active = true
            group by p.category.id""")
    List<CountByParent> countVisibleByCategory();

    /** Number of visible products per brand id. */
    @Query("""
            select p.brand.id as parentId, count(p) as productCount
            from Product p
            where p.active = true and p.visibleOnWebsite = true
              and p.category.active = true and p.brand.active = true
            group by p.brand.id""")
    List<CountByParent> countVisibleByBrand();

    /** Lowest and highest indicative price of visible products (price filter hints). */
    @Query("""
            select min(p.price) as min, max(p.price) as max
            from Product p
            where p.active = true and p.visibleOnWebsite = true
              and p.category.active = true and p.brand.active = true""")
    PriceRangeView findVisiblePriceRange();

    /** True while any visible demo product exists – drives the website's "Demonstration data" notice. */
    @Query("""
            select count(p) > 0 from Product p
            where p.demo = true and p.active = true and p.visibleOnWebsite = true
              and p.category.active = true and p.brand.active = true""")
    boolean anyVisibleDemoProduct();

    /** Projection: (category or brand id, product count). */
    interface CountByParent {
        Long getParentId();
        long getProductCount();
    }

    /** Projection: lowest / highest price (both null when there are no visible products). */
    interface PriceRangeView {
        BigDecimal getMin();
        BigDecimal getMax();
    }
}
