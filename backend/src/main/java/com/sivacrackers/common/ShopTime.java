package com.sivacrackers.common;

import com.sivacrackers.common.exception.InvalidRequestException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * The shop works in Indian time; the database stores UTC. Dates chosen in the admin ("1 to 31 March")
 * mean whole days in Indian time, so they are converted here.
 */
public final class ShopTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    /** MySQL offset for grouping by Indian day / month / year (India has no daylight saving). */
    public static final String MYSQL_OFFSET = "+05:30";
    /** Longest period a report or filter may cover. */
    public static final long MAX_DAYS = 366L * 5;

    private ShopTime() {
    }

    public static LocalDate today(Clock clock) {
        return LocalDate.now(clock.withZone(ZONE));
    }

    /** Start of {@code date} in Indian time, as an instant. */
    public static Instant startOf(LocalDate date) {
        return date.atStartOfDay(ZONE).toInstant();
    }

    /** First instant after {@code date} ends in Indian time (exclusive upper bound). */
    public static Instant endOf(LocalDate date) {
        return date.plusDays(1).atStartOfDay(ZONE).toInstant();
    }

    /** A validated date range: defaults to the last 30 days; from ≤ to; at most {@link #MAX_DAYS}. */
    public record Range(LocalDate from, LocalDate to) {

        public static Range of(LocalDate from, LocalDate to, Clock clock) {
            LocalDate end = to != null ? to : today(clock);
            LocalDate start = from != null ? from : end.minusDays(29);
            if (start.isAfter(end)) {
                throw new InvalidRequestException("from", "must not be after the end date");
            }
            if (ChronoUnit.DAYS.between(start, end) > MAX_DAYS) {
                throw new InvalidRequestException("from", "the period can be at most 5 years");
            }
            return new Range(start, end);
        }

        public Instant startInstant() {
            return startOf(from);
        }

        public Instant endInstantExclusive() {
            return endOf(to);
        }
    }
}
