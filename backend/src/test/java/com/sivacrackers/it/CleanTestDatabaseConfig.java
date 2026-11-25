package com.sivacrackers.it;

import javax.sql.DataSource;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Test-only Flyway strategy: wipe the TEST database and apply every migration from scratch,
 * so each run starts from the exact state a brand-new installation would have.
 *
 * <p>Second safety check (after {@link TestDatabase}): asks MySQL which database the connection
 * really uses and refuses to clean anything whose name does not end in {@code _test}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class CleanTestDatabaseConfig {

    @Bean
    FlywayMigrationStrategy cleanThenMigrate(DataSource dataSource) {
        return flyway -> {
            String database = new JdbcTemplate(dataSource).queryForObject("SELECT DATABASE()", String.class);
            if (database == null || !database.endsWith("_test")) {
                throw new IllegalStateException("Refusing to clean database '" + database + "': not a *_test database.");
            }
            flyway.clean();
            flyway.migrate();
        };
    }
}
