package com.sivacrackers.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sivacrackers.settings.BannerRepository;
import com.sivacrackers.settings.BusinessSettings;
import com.sivacrackers.settings.BusinessSettingsRepository;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Flyway migrations, schema shape and the V2 business defaults, against the real MySQL test database. */
class SchemaMigrationIT extends AbstractMySqlIT {

    @Autowired
    DataSource dataSource;

    @Autowired
    BusinessSettingsRepository settingsRepository;

    @Autowired
    BannerRepository bannerRepository;

    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    @DisplayName("Connected to the *_test database, never the development database")
    void connectedToTestDatabase() {
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class)).endsWith("_test");
    }

    @Test
    @DisplayName("Flyway applied V1 to V5 successfully, in order")
    void migrationsApplied() {
        List<String> applied = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = 1 AND version IS NOT NULL ORDER BY installed_rank",
                String.class);
        assertThat(applied).containsExactly("1", "2", "3", "4", "5");
    }

    @Test
    @DisplayName("Exactly the approved 20 tables exist (plus Flyway's history)")
    void approvedTables() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() ORDER BY table_name",
                String.class);
        assertThat(tables).containsExactlyInAnyOrder(
                "audit_log", "banners", "bill_counters", "bill_items", "bills", "brands", "business_highlights", "business_settings", "categories",
                "category_safety_notes", "expense_categories", "expenses", "flyway_schema_history", "product_images",
                "payment_methods", "products", "safety_tips", "shop_customers", "stock_movements", "suppliers", "users");
    }

    @Test
    @DisplayName("No online ordering schema: no order, cart, checkout, booking, reservation, invoice tables; no card or gateway columns")
    void noOnlineSaleSchema() {
        Integer forbidden = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND (table_name REGEXP 'order|cart|checkout|booking|reservation|invoice'
                       OR column_name REGEXP 'order_id|cart|checkout|booking|reservation|card_number|cvv|gateway|transaction')""",
                Integer.class);
        assertThat(forbidden).isZero();
    }

    @Test
    @DisplayName("All tables use InnoDB and utf8mb4")
    void engineAndCharset() {
        List<String> wrong = jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
                  AND (engine <> 'InnoDB' OR table_collation NOT LIKE 'utf8mb4%')""",
                String.class);
        assertThat(wrong).isEmpty();
    }

    @Test
    @DisplayName("Key CHECK, UNIQUE and FOREIGN KEY constraints exist")
    void constraintsExist() {
        List<String> names = jdbc.queryForList(
                "SELECT constraint_name FROM information_schema.table_constraints WHERE table_schema = DATABASE()",
                String.class);
        assertThat(names).contains(
                "ck_products_price", "ck_products_mrp", "ck_products_price_le_mrp", "ck_products_stock",
                "ck_products_unit", "ck_products_slug", "uq_products_slug", "uq_products_sku",
                "fk_products_category", "fk_products_brand", "uq_categories_slug", "uq_brands_slug",
                "uq_users_email", "ck_users_role", "ck_users_password_hash", "ck_business_settings_single_row",
                "ck_banners_link");
    }

    @Test
    @DisplayName("Search/filter indexes exist on products")
    void productIndexes() {
        List<String> indexes = jdbc.queryForList(
                "SELECT DISTINCT index_name FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'products'",
                String.class);
        assertThat(indexes).contains("ix_products_category", "ix_products_brand", "ix_products_price",
                "ix_products_name", "ix_products_created", "ix_products_listing");
    }

    @Test
    @DisplayName("V2: one settings row with known facts only – unknown details stay empty")
    void businessDefaults() {
        BusinessSettings s = settingsRepository.findSingleton().orElseThrow();
        assertThat(s.getShopName()).isEqualTo("SIVA Crackers");
        assertThat(s.getArea()).isEqualTo("Eachanari");
        assertThat(s.getCity()).isEqualTo("Coimbatore");
        assertThat(s.getState()).isEqualTo("Tamil Nadu");
        // Never invented:
        assertThat(s.getPhone()).isNull();
        assertThat(s.getAlternatePhone()).isNull();
        assertThat(s.getWhatsappNumber()).isNull();
        assertThat(s.getEmail()).isNull();
        assertThat(s.getBusinessHours()).isNull();
        assertThat(s.getAddressLine1()).isNull();
        assertThat(s.getPincode()).isNull();
        assertThat(s.getLicenceDetails()).isNull();
        assertThat(s.isMapIsApproximate()).isTrue();
        assertThat(s.getLegalNotice()).contains("does not sell, book, accept payment for, or deliver firecrackers online");
        assertThat(s.getHighlights()).hasSize(4);
        assertThat(s.getHighlights().getFirst().getTitle()).isEqualTo("Family-owned shop");
        assertThat(s.getSafetyTips()).hasSize(7);
        assertThat(s.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("V2: three active, non-demo banners with internal links")
    void defaultBanners() {
        var banners = bannerRepository.findAllByActiveTrueOrderByDisplayOrderAscIdAsc();
        assertThat(banners).hasSize(3);
        assertThat(banners).allSatisfy(b -> {
            assertThat(b.isDemo()).isFalse();
            assertThat(b.getCtaLink()).startsWith("/");
        });
        assertThat(banners.getFirst().getTitle()).isEqualTo("Light up every celebration");
    }

    @Test
    @DisplayName("Only one business_settings row can ever exist")
    void singleSettingsRow() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO business_settings (id, shop_name, created_at, updated_at) VALUES (2, 'Other', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_business_settings_single_row");
    }

    @Test
    @DisplayName("Banner links must be internal paths (external URLs rejected)")
    void bannerLinksInternalOnly() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO banners (title, cta_link, theme, created_at, updated_at) VALUES ('x', 'https://example.com', 'gold', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_banners_link");
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO banners (title, cta_link, theme, created_at, updated_at) VALUES ('x', '//example.com', 'gold', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_banners_link");
    }

    @Test
    @DisplayName("Unknown banner themes are rejected")
    void bannerThemeChecked() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO banners (title, theme, created_at, updated_at) VALUES ('x', 'purple', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_banners_theme");
    }
}
