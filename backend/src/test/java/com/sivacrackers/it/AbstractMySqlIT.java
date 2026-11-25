package com.sivacrackers.it;

import com.sivacrackers.config.JpaConfig;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base for repository / schema integration tests against the real MySQL TEST database.
 * Flyway migrations V1, V2, … are applied to a clean database; Hibernate then validates that
 * every entity matches the migrated schema. Each test runs in a transaction that is rolled back.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("it")
@Import({JpaConfig.class, CleanTestDatabaseConfig.class})
public abstract class AbstractMySqlIT {

    @DynamicPropertySource
    static void testDatabase(DynamicPropertyRegistry registry) {
        TestDatabase.register(registry);
    }
}
