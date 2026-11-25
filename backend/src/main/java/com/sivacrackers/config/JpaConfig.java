package com.sivacrackers.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Fills createdAt / updatedAt automatically on entities (Phase 4.2).
 * Kept in its own class (not on the application class) so web-layer tests can run without a database.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
