package com.sivacrackers.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivacrackers.catalog.product.Product;
import com.sivacrackers.catalog.product.ProductRepository;
import com.sivacrackers.catalog.product.StockStatus;
import com.sivacrackers.demo.DemoDataSeeder;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

/** Demo catalogue seeding on the real MySQL test database: counts, flags, idempotency, no overwrites. */
@Import(DemoDataSeeder.class)
class DemoDataSeederIT extends AbstractMySqlIT {

    @Autowired DemoDataSeeder seeder;
    @Autowired ProductRepository products;
    @Autowired EntityManager em;
    @Autowired DataSource dataSource;

    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
    }

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    @Test
    @DisplayName("Seeds 12 categories, 8 brands and 48 products – every row marked as demo")
    void seedsEverythingAsDemo() {
        DemoDataSeeder.Result r = seeder.seed();
        assertThat(r.categoriesInserted()).isEqualTo(12);
        assertThat(r.brandsInserted()).isEqualTo(8);
        assertThat(r.productsInserted()).isEqualTo(48);
        assertThat(count("SELECT COUNT(*) FROM categories WHERE is_demo = 1")).isEqualTo(12);
        assertThat(count("SELECT COUNT(*) FROM brands WHERE is_demo = 1")).isEqualTo(8);
        assertThat(count("SELECT COUNT(*) FROM products WHERE is_demo = 1")).isEqualTo(48);
        assertThat(count("SELECT COUNT(*) FROM products WHERE is_demo = 0")).isZero();
        assertThat(count("SELECT COUNT(*) FROM category_safety_notes")).isEqualTo(36);
    }

    @Test
    @DisplayName("Running it again inserts nothing and creates no duplicates (idempotent)")
    void idempotent() {
        seeder.seed();
        DemoDataSeeder.Result second = seeder.seed();
        assertThat(second.categoriesInserted()).isZero();
        assertThat(second.brandsInserted()).isZero();
        assertThat(second.productsInserted()).isZero();
        assertThat(second.productsSkipped()).isEqualTo(48);
        assertThat(count("SELECT COUNT(*) FROM products")).isEqualTo(48);
        assertThat(count("SELECT COUNT(*) FROM categories")).isEqualTo(12);
        assertThat(count("SELECT COUNT(*) FROM category_safety_notes")).isEqualTo(36);
    }

    @Test
    @DisplayName("Never overwrites real data: an existing real product with a demo slug is left untouched")
    void neverOverwritesRealData() {
        jdbc.update("""
                INSERT INTO categories (name, slug, active, is_demo, created_at, updated_at)
                VALUES ('Rockets', 'rockets', TRUE, FALSE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))""");
        Long realCategory = jdbc.queryForObject("SELECT id FROM categories WHERE slug = 'rockets'", Long.class);
        jdbc.update("""
                INSERT INTO brands (name, slug, active, is_demo, created_at, updated_at)
                VALUES ('Real Brand', 'real-brand', TRUE, FALSE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))""");
        Long realBrand = jdbc.queryForObject("SELECT id FROM brands WHERE slug = 'real-brand'", Long.class);
        jdbc.update("""
                INSERT INTO products (name, slug, sku, category_id, brand_id, pack_size, unit, mrp, price,
                                      stock_quantity, is_demo, created_at, updated_at)
                VALUES ('Our real Colour Rocket', 'colour-rocket', 'REAL-001', ?, ?, '5 Pcs', 'BOX', 999.00, 777.00,
                        3, FALSE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))""", realCategory, realBrand);

        DemoDataSeeder.Result r = seeder.seed();

        assertThat(r.productsSkipped()).isEqualTo(1);
        assertThat(r.productsInserted()).isEqualTo(47);
        assertThat(r.categoriesReused()).isEqualTo(1);
        Map<String, Object> real = jdbc.queryForMap("SELECT name, price, is_demo FROM products WHERE slug = 'colour-rocket'");
        assertThat(real.get("name")).isEqualTo("Our real Colour Rocket");
        assertThat((java.math.BigDecimal) real.get("price")).isEqualByComparingTo("777.00");
        assertThat(real.get("is_demo")).isEqualTo(false);
        // The real category stays real (not re-flagged as demo)
        assertThat(jdbc.queryForObject("SELECT is_demo FROM categories WHERE slug = 'rockets'", Boolean.class)).isFalse();
    }

    @Test
    @DisplayName("Seeded prices and stock match the website: price ≤ MRP; 3 out of stock, 4 low stock")
    void pricesAndStock() {
        seeder.seed();
        em.clear();
        List<Product> all = products.findAll();
        assertThat(all).allSatisfy(p -> assertThat(p.getPrice()).isLessThanOrEqualTo(p.getMrp()));
        Map<StockStatus, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(Product::getStockStatus, Collectors.counting()));
        assertThat(byStatus).containsEntry(StockStatus.OUT_OF_STOCK, 3L).containsEntry(StockStatus.LOW_STOCK, 4L)
                .containsEntry(StockStatus.IN_STOCK, 41L);
        Map<String, Product> bySlug = all.stream().collect(Collectors.toMap(Product::getSlug, Function.identity()));
        assertThat(bySlug.get("colour-rocket").getPrice()).isEqualByComparingTo("225");
        assertThat(bySlug.get("colour-rocket").getCategory().getSlug()).isEqualTo("rockets");
        assertThat(bySlug.get("colour-rocket").getBrand().getSlug()).isEqualTo("minnal-pyro");
    }

    @Test
    @DisplayName("Original 'added' dates are kept, so New Arrivals order matches the website")
    void createdDatesPreserved() {
        seeder.seed();
        em.clear();
        List<Product> newest = products.findAll(Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        assertThat(newest.getFirst().getSlug()).isEqualTo("premium-celebration-box"); // 2026-11-20
        assertThat(newest.get(1).getSlug()).isEqualTo("kids-delight-box");             // 2026-11-19
    }

    @Test
    @DisplayName("Business settings and banners are not touched by the demo seed")
    void settingsUntouched() {
        String before = jdbc.queryForObject("SELECT CONCAT(shop_name, '|', COALESCE(phone, 'null')) FROM business_settings", String.class);
        seeder.seed();
        assertThat(jdbc.queryForObject("SELECT CONCAT(shop_name, '|', COALESCE(phone, 'null')) FROM business_settings", String.class))
                .isEqualTo(before);
        assertThat(count("SELECT COUNT(*) FROM banners")).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM banners WHERE is_demo = 1")).isZero();
    }
}
