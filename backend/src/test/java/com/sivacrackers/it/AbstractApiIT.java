package com.sivacrackers.it;

import com.sivacrackers.demo.DemoDataSeeder;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Full application (controllers, services, security, JPA) against the MySQL TEST database,
 * loaded with the demo catalogue. Requests go through MockMvc exactly like browser requests.
 *
 * <p>Data is committed (not rolled back) so it is removed again after the class, leaving the
 * test database clean for the repository tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@Import(CleanTestDatabaseConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractApiIT {

    @DynamicPropertySource
    static void testDatabase(DynamicPropertyRegistry registry) {
        TestDatabase.register(registry);
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected DataSource dataSource;

    @Autowired
    private DemoDataSeeder seeder;

    protected JdbcTemplate jdbc;

    @BeforeAll
    void loadDemoCatalogue() {
        jdbc = new JdbcTemplate(dataSource);
        removeCatalogue();
        seeder.seed();
    }

    @AfterAll
    void removeCatalogue() {
        requireTestDatabase();
        jdbc.update("DELETE FROM stock_movements");
        jdbc.update("DELETE FROM bill_items");
        jdbc.update("DELETE FROM bills");
        jdbc.update("DELETE FROM bill_counters");
        // keep the 4 payment methods created by migration V5; remove ones added by tests
        jdbc.update("DELETE FROM payment_methods WHERE code NOT IN ('CASH', 'UPI', 'CARD', 'OTHER')");
        jdbc.update("DELETE FROM products");
        jdbc.update("DELETE FROM suppliers");
        jdbc.update("DELETE FROM brands");
        jdbc.update("DELETE FROM categories");
        jdbc.update("DELETE FROM expenses");
        // keep the 7 categories created by migration V4; remove ones added by tests
        jdbc.update("DELETE FROM expense_categories WHERE name NOT IN "
                + "('Shop rent', 'Electricity', 'Transport', 'Packaging', 'Staff salary', 'Maintenance', 'Other')");
        jdbc.update("DELETE FROM shop_customers");
        jdbc.update("DELETE FROM audit_log");
        jdbc.update("DELETE FROM users");
    }

    private void requireTestDatabase() {
        String db = jdbc.queryForObject("SELECT DATABASE()", String.class);
        if (db == null || !db.endsWith("_test")) {
            throw new IllegalStateException("Refusing to delete data outside a *_test database");
        }
    }
}
