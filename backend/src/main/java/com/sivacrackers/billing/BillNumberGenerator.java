package com.sivacrackers.billing;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unique, gap-free-per-year bill numbers such as {@code SC-2026-000001}. The counter row is incremented
 * atomically inside the bill's own transaction, so concurrent bills never get the same number.
 */
@Component
public class BillNumberGenerator {

    static final String PREFIX = "SC";

    private final JdbcTemplate jdbc;

    public BillNumberGenerator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String next(int year) {
        // LAST_INSERT_ID(expr) makes the new value readable on this connection without a second race.
        jdbc.update("""
                INSERT INTO bill_counters (bill_year, last_number) VALUES (?, LAST_INSERT_ID(1))
                ON DUPLICATE KEY UPDATE last_number = LAST_INSERT_ID(last_number + 1)""", year);
        Long n = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return "%s-%d-%06d".formatted(PREFIX, year, n);
    }
}
