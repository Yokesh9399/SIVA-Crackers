package com.sivacrackers.demo;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Loads the optional DEMONSTRATION catalogue (12 categories, 8 fictional brands, 48 products)
 * from {@code classpath:demo/*.json} – the same sample data the website showed in Phases 1–3.
 *
 * <p>Rules:
 * <ul>
 *   <li>Only runs when {@code APP_SEED_DEMO=true} (see {@link DemoDataRunner}); default is off.</li>
 *   <li>Idempotent: inserts only rows that are missing, so restarting never creates duplicates.</li>
 *   <li>Never overwrites real data: a product whose slug or product code already exists is skipped
 *       and the existing row is left untouched. Existing categories/brands are reused, never changed.</li>
 *   <li>Every inserted category, brand and product is marked {@code is_demo = 1}.</li>
 *   <li>Business settings and banners are not touched (they are real content from migration V2).</li>
 * </ul>
 */
@Component
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final JsonMapper JSON = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final JdbcTemplate jdbc;

    public DemoDataSeeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    record DemoCategory(long id, String name, String slug, String description, int displayOrder,
                        boolean active, List<String> safetyNotes) {
    }

    record DemoBrand(long id, String name, String slug, String description, int displayOrder, boolean active) {
    }

    record DemoProduct(String name, String slug, String sku, long categoryId, long brandId, String packSize,
                       String unit, BigDecimal mrp, BigDecimal price, int stock, boolean featured,
                       LocalDate createdAt, String description) {
    }

    /** What a seeding run did. "reused" = an existing row with the same slug was kept as it is. */
    public record Result(int categoriesInserted, int categoriesReused, int brandsInserted, int brandsReused,
                         int productsInserted, int productsSkipped) {
    }

    @Transactional
    public Result seed() {
        List<DemoCategory> categories = read("demo/categories.json", new TypeReference<>() { });
        List<DemoBrand> brands = read("demo/brands.json", new TypeReference<>() { });
        List<DemoProduct> products = read("demo/products.json", new TypeReference<>() { });

        Timestamp now = Timestamp.from(Instant.now());

        // ---- categories: reuse by slug, otherwise insert as demo ----
        Map<Long, Long> categoryIds = new HashMap<>();
        int catInserted = 0;
        int catReused = 0;
        for (DemoCategory c : categories) {
            Long existing = idBySlug("categories", c.slug());
            if (existing != null) {
                categoryIds.put(c.id(), existing);
                catReused++;
                continue;
            }
            jdbc.update("""
                    INSERT INTO categories (name, slug, description, display_order, active, is_demo, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, TRUE, ?, ?)""",
                    c.name(), c.slug(), c.description(), c.displayOrder(), c.active(), now, now);
            long id = idBySlug("categories", c.slug());
            List<String> notes = c.safetyNotes() == null ? List.of() : c.safetyNotes();
            for (int i = 0; i < notes.size(); i++) {
                jdbc.update("INSERT INTO category_safety_notes (category_id, position, note) VALUES (?, ?, ?)", id, i, notes.get(i));
            }
            categoryIds.put(c.id(), id);
            catInserted++;
        }

        // ---- brands: reuse by slug, otherwise insert as demo ----
        Map<Long, Long> brandIds = new HashMap<>();
        int brandInserted = 0;
        int brandReused = 0;
        for (DemoBrand b : brands) {
            Long existing = idBySlug("brands", b.slug());
            if (existing != null) {
                brandIds.put(b.id(), existing);
                brandReused++;
                continue;
            }
            jdbc.update("""
                    INSERT INTO brands (name, slug, description, display_order, active, is_demo, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, TRUE, ?, ?)""",
                    b.name(), b.slug(), b.description(), b.displayOrder(), b.active(), now, now);
            brandIds.put(b.id(), idBySlug("brands", b.slug()));
            brandInserted++;
        }

        // ---- products: insert only if neither slug nor product code exists; never update ----
        int prodInserted = 0;
        int prodSkipped = 0;
        for (DemoProduct p : products) {
            Integer clash = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM products WHERE slug = ? OR sku = ?", Integer.class, p.slug(), p.sku());
            if (clash != null && clash > 0) {
                prodSkipped++;
                continue;
            }
            // Keep the original "added" dates so New Arrivals order matches the Phase 1–3 website.
            Timestamp created = p.createdAt() == null ? now
                    : Timestamp.from(p.createdAt().atStartOfDay().toInstant(ZoneOffset.UTC));
            jdbc.update("""
                    INSERT INTO products (name, slug, sku, category_id, brand_id, description, pack_size, unit,
                                          mrp, price, stock_quantity, reorder_level, featured,
                                          visible_on_website, active, is_demo, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 10, ?, TRUE, TRUE, TRUE, ?, ?)""",
                    p.name(), p.slug(), p.sku(), categoryIds.get(p.categoryId()), brandIds.get(p.brandId()),
                    p.description(), p.packSize(), p.unit(), p.mrp(), p.price(), p.stock(), p.featured(),
                    created, now);
            prodInserted++;
        }

        Result result = new Result(catInserted, catReused, brandInserted, brandReused, prodInserted, prodSkipped);
        log.info("Demo data: categories {} inserted / {} reused, brands {} inserted / {} reused, products {} inserted / {} skipped (already present)",
                catInserted, catReused, brandInserted, brandReused, prodInserted, prodSkipped);
        return result;
    }

    private Long idBySlug(String table, String slug) {
        // table is one of two fixed internal names, never user input
        List<Long> ids = jdbc.queryForList("SELECT id FROM " + table + " WHERE slug = ?", Long.class, slug);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    private static <T> List<T> read(String path, TypeReference<List<T>> type) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return JSON.readValue(in, type);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read demo data file " + path, e);
        }
    }
}
