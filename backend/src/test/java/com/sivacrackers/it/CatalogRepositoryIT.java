package com.sivacrackers.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sivacrackers.catalog.brand.Brand;
import com.sivacrackers.catalog.brand.BrandRepository;
import com.sivacrackers.catalog.category.Category;
import com.sivacrackers.catalog.category.CategoryRepository;
import com.sivacrackers.catalog.product.Product;
import com.sivacrackers.catalog.product.ProductImage;
import com.sivacrackers.catalog.product.ProductRepository;
import com.sivacrackers.catalog.product.ProductUnit;
import com.sivacrackers.catalog.product.StockStatus;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Categories, brands and products: relationships, constraints, cascades and repository queries. */
class CatalogRepositoryIT extends AbstractMySqlIT {

    @Autowired CategoryRepository categories;
    @Autowired BrandRepository brands;
    @Autowired ProductRepository products;
    @Autowired EntityManager em;
    @Autowired DataSource dataSource;

    JdbcTemplate jdbc;
    Category rockets;
    Brand kovai;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        rockets = new Category("Rockets", "rockets");
        rockets.setDisplayOrder(4);
        rockets.replaceSafetyNotes(List.of("Launch from a bottle.", "Use open spaces.", "Never point at people."));
        categories.saveAndFlush(rockets);
        kovai = brands.saveAndFlush(new Brand("Kovai Star", "kovai-star"));
    }

    private Product product(String slug, String sku, String mrp, String price, int stock) {
        Product p = new Product("Product " + slug, slug, sku, rockets, kovai, "10 Pcs", ProductUnit.BOX,
                new BigDecimal(mrp), new BigDecimal(price));
        p.setStockQuantity(stock);
        return p;
    }

    @Test
    @DisplayName("Product saved with category and brand; findBySlug loads both")
    void saveAndFindBySlug() {
        products.saveAndFlush(product("colour-rocket", "RK-002", "300.00", "225.00", 130));
        em.clear();
        Product found = products.findBySlug("colour-rocket").orElseThrow();
        assertThat(found.getCategory().getSlug()).isEqualTo("rockets");
        assertThat(found.getBrand().getName()).isEqualTo("Kovai Star");
        assertThat(found.getPrice()).isEqualByComparingTo("225.00");
        assertThat(found.getMrp()).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("Stock status is calculated: 0 → OUT, ≤ reorder level → LOW, otherwise IN")
    void stockStatusCalculated() {
        assertThat(product("a", "A-1", "10", "5", 0).getStockStatus()).isEqualTo(StockStatus.OUT_OF_STOCK);
        assertThat(product("b", "B-1", "10", "5", 10).getStockStatus()).isEqualTo(StockStatus.LOW_STOCK);
        assertThat(product("c", "C-1", "10", "5", 11).getStockStatus()).isEqualTo(StockStatus.IN_STOCK);
    }

    @Test
    @DisplayName("Duplicate product slug is rejected")
    void duplicateSlug() {
        products.saveAndFlush(product("dup", "D-1", "10", "5", 1));
        assertThatThrownBy(() -> products.saveAndFlush(product("dup", "D-2", "10", "5", 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Duplicate product code (SKU) is rejected")
    void duplicateSku() {
        products.saveAndFlush(product("one", "SAME-1", "10", "5", 1));
        assertThatThrownBy(() -> products.saveAndFlush(product("two", "SAME-1", "10", "5", 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Duplicate category / brand slug is rejected (case-insensitive)")
    void duplicateCategoryAndBrandSlug() {
        assertThatThrownBy(() -> categories.saveAndFlush(new Category("Rockets 2", "rockets")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> brands.saveAndFlush(new Brand("KOVAI STAR", "kovai-star-2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Negative price, negative MRP and negative stock are rejected by the database")
    void noNegatives() {
        assertThatThrownBy(() -> products.saveAndFlush(product("neg-price", "N-1", "10", "-1", 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> products.saveAndFlush(product("neg-mrp", "N-2", "-1", "0", 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> products.saveAndFlush(product("neg-stock", "N-3", "10", "5", -5)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Indicative price equal to MRP is allowed")
    void priceEqualToMrpAllowed() {
        Product p = products.saveAndFlush(product("equal-mrp", "M-2", "100.00", "100.00", 1));
        assertThat(p.getId()).isNotNull();
    }

    @Test
    @DisplayName("Indicative price above MRP is rejected (ck_products_price_le_mrp)")
    void priceNotAboveMrp() {
        assertThatThrownBy(() -> products.saveAndFlush(product("above-mrp", "M-1", "100.00", "100.01", 1)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_products_price_le_mrp");
    }

    @Test
    @DisplayName("Badly formatted slugs are rejected (ck_products_slug)")
    void slugFormat() {
        assertThatThrownBy(() -> products.saveAndFlush(product("Bad Slug!", "S-1", "10", "5", 1)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_products_slug");
    }

    @Test
    @DisplayName("Unknown product units are rejected (ck_products_unit)")
    void unitChecked() {
        Long id = products.saveAndFlush(product("unit-test", "U-1", "10", "5", 1)).getId();
        assertThatThrownBy(() -> jdbc.update("UPDATE products SET unit = 'CRATE' WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_products_unit");
    }

    @Test
    @DisplayName("A category or brand that still has products cannot be deleted (foreign keys)")
    void restrictDeleteWithProducts() {
        products.saveAndFlush(product("keep", "K-1", "10", "5", 1));
        // MySQL error 1451 does not name the key for this account, so check its fixed wording.
        assertThatThrownBy(() -> jdbc.update("DELETE FROM categories WHERE id = ?", rockets.getId()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Cannot delete or update a parent row: a foreign key constraint fails");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM brands WHERE id = ?", kovai.getId()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Cannot delete or update a parent row: a foreign key constraint fails");
        assertThat(categories.existsById(rockets.getId())).isTrue();
        assertThat(brands.existsById(kovai.getId())).isTrue();
    }

    @Test
    @DisplayName("Deleting a product removes its images (cascade); images keep their order")
    void imagesCascadeAndOrder() {
        Product p = product("with-images", "I-1", "10", "5", 1);
        p.getImages().add(new ProductImage("/uploads/a.jpg", "front"));
        p.getImages().add(new ProductImage("/uploads/b.jpg", "back"));
        Long id = products.saveAndFlush(p).getId();
        em.clear();
        assertThat(products.findById(id).orElseThrow().getImages())
                .extracting(ProductImage::getUrl).containsExactly("/uploads/a.jpg", "/uploads/b.jpg");
        products.deleteById(id);
        products.flush();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_images WHERE product_id = ?", Integer.class, id)).isZero();
    }

    @Test
    @DisplayName("Category safety notes are stored and returned in order")
    void safetyNotesOrdered() {
        em.clear();
        assertThat(categories.findBySlug("rockets").orElseThrow().getSafetyNotes())
                .containsExactly("Launch from a bottle.", "Use open spaces.", "Never point at people.");
    }

    @Test
    @DisplayName("Created/updated timestamps are filled automatically and updated on change")
    void auditTimestamps() {
        Product p = products.saveAndFlush(product("audited", "T-1", "10", "5", 1));
        Instant created = p.getCreatedAt();
        Instant updated = p.getUpdatedAt();
        assertThat(created).isNotNull();
        assertThat(updated).isNotNull();
        p.setStockQuantity(2);
        products.saveAndFlush(p);
        assertThat(p.getCreatedAt()).isEqualTo(created);
        assertThat(p.getUpdatedAt()).isAfterOrEqualTo(updated);
    }

    @Test
    @DisplayName("Counts and price range include only active, website-visible products")
    void visibleCountsAndPriceRange() {
        products.saveAndFlush(product("v1", "V-1", "100", "50", 1));
        products.saveAndFlush(product("v2", "V-2", "300", "250", 1));
        Product hidden = product("hidden", "V-3", "9999", "9000", 1);
        hidden.setVisibleOnWebsite(false);
        products.saveAndFlush(hidden);
        Product inactive = product("inactive", "V-4", "5", "1", 1);
        inactive.setActive(false);
        products.saveAndFlush(inactive);

        Map<Long, Long> byCategory = products.countVisibleByCategory().stream()
                .collect(Collectors.toMap(ProductRepository.CountByParent::getParentId, ProductRepository.CountByParent::getProductCount));
        assertThat(byCategory).containsEntry(rockets.getId(), 2L);
        Map<Long, Long> byBrand = products.countVisibleByBrand().stream()
                .collect(Collectors.toMap(ProductRepository.CountByParent::getParentId, ProductRepository.CountByParent::getProductCount));
        assertThat(byBrand).containsEntry(kovai.getId(), 2L);

        ProductRepository.PriceRangeView range = products.findVisiblePriceRange();
        assertThat(range.getMin()).isEqualByComparingTo("50");
        assertThat(range.getMax()).isEqualByComparingTo("250");
    }

    @Test
    @DisplayName("findAllBySlugIn returns only existing slugs (Estimate List lookup)")
    void findBySlugs() {
        products.saveAndFlush(product("e1", "E-1", "10", "5", 1));
        products.saveAndFlush(product("e2", "E-2", "10", "5", 1));
        assertThat(products.findAllBySlugIn(List.of("e1", "e2", "ghost")))
                .extracting(Product::getSlug).containsExactlyInAnyOrder("e1", "e2");
    }

    @Test
    @DisplayName("Active categories are listed in display order")
    void activeCategoriesOrdered() {
        Category first = new Category("Sparklers", "sparklers");
        first.setDisplayOrder(1);
        categories.saveAndFlush(first);
        Category hidden = new Category("Old", "old");
        hidden.setActive(false);
        categories.saveAndFlush(hidden);
        assertThat(categories.findAllByActiveTrueOrderByDisplayOrderAscIdAsc())
                .extracting(Category::getSlug).containsExactly("sparklers", "rockets");
    }
}
