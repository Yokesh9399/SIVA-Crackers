package com.sivacrackers.it;

import java.util.regex.Pattern;
import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Connection settings for MySQL integration tests, read from TEST_DB_URL / TEST_DB_USERNAME /
 * TEST_DB_PASSWORD (never hard-coded, never printed).
 *
 * <p>SAFETY: integration tests rebuild their database from scratch. To make it impossible to run
 * them against the development or production database, the URL must point to a localhost
 * database whose name ends in {@code _test}. Anything else aborts the test run.
 */
public final class TestDatabase {

    private static final Pattern SAFE_URL =
            Pattern.compile("^jdbc:mysql://(localhost|127\\.0\\.0\\.1)(:\\d+)?/[A-Za-z0-9_]+_test(\\?.*)?$");

    private TestDatabase() {
    }

    public static void register(DynamicPropertyRegistry registry) {
        String url = required("TEST_DB_URL");
        if (!SAFE_URL.matcher(url).matches()) {
            throw new IllegalStateException("Refusing to run integration tests: TEST_DB_URL must point to a localhost "
                    + "MySQL database whose name ends in _test (for example jdbc:mysql://localhost:3306/siva_crackers_test).");
        }
        String username = required("TEST_DB_USERNAME");
        String password = required("TEST_DB_PASSWORD");
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> username);
        registry.add("spring.datasource.password", () -> password);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is not set. Integration tests need TEST_DB_URL, TEST_DB_USERNAME and "
                    + "TEST_DB_PASSWORD (run them with backend/scripts/run-integration-tests.ps1).");
        }
        return value;
    }
}
