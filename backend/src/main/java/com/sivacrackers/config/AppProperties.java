package com.sivacrackers.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Application settings bound from {@code app.*} in application.yml, which in turn
 * reads environment variables. Validated at startup: the application refuses to
 * start with a missing or weak JWT secret.
 *
 * <p>SECURITY: {@link #toString()} of every nested record masks secrets so this
 * object can never leak a secret into logs.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotNull @Valid Jwt jwt,
        @NotNull @Valid AuthCookie cookie,
        @NotNull @Valid Cors cors,
        boolean seedDemo,
        @NotNull @Valid Bootstrap bootstrap) {

    /** Minimum HMAC-SHA256 key length in bytes (RFC 7518 §3.2). */
    public static final int MIN_SECRET_BYTES = 32;

    /** JWT signing settings. Env: JWT_SECRET, JWT_EXPIRY_MINUTES. */
    public record Jwt(@NotBlank String secret, @Min(5) @Max(1440) int expiryMinutes) {
        public Jwt {
            // The message names the variable but never contains the secret itself.
            if (secret == null || secret.isBlank()) {
                throw new IllegalArgumentException("JWT_SECRET is not set. Set a random value of at least "
                        + MIN_SECRET_BYTES + " bytes (see backend/README.md).");
            }
            if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
                throw new IllegalArgumentException("JWT_SECRET is too short: it must be at least "
                        + MIN_SECRET_BYTES + " bytes (see backend/README.md).");
            }
        }

        @Override
        public String toString() {
            return "Jwt[secret=****, expiryMinutes=" + expiryMinutes + "]";
        }
    }

    /**
     * Authentication cookie. HttpOnly is always on and not configurable.
     * Env: APP_COOKIE_SECURE (keep true outside local development), APP_COOKIE_SAME_SITE.
     */
    public record AuthCookie(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]+") String name,
            boolean secure,
            @NotBlank @Pattern(regexp = "Strict|Lax", message = "must be Strict or Lax") String sameSite) {
    }

    /** Cross-origin access for a separately hosted frontend. Env: CORS_ALLOWED_ORIGINS (comma-separated). */
    public record Cors(List<String> allowedOrigins) {
        public Cors {
            allowedOrigins = allowedOrigins == null
                    ? List.of()
                    : allowedOrigins.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
            if (allowedOrigins.stream().anyMatch(o -> o.contains("*"))) {
                throw new IllegalArgumentException("CORS_ALLOWED_ORIGINS must list exact origins; wildcards are not allowed with credentials.");
            }
        }
    }

    /**
     * One-time OWNER bootstrap (used from Phase 4.5). Only applied when no OWNER exists.
     * Env: BOOTSTRAP_OWNER_EMAIL, BOOTSTRAP_OWNER_PASSWORD. Both may be empty.
     */
    public record Bootstrap(String ownerEmail, String ownerPassword) {
        public boolean isConfigured() {
            return ownerEmail != null && !ownerEmail.isBlank() && ownerPassword != null && !ownerPassword.isBlank();
        }

        @Override
        public String toString() {
            return "Bootstrap[ownerEmail=" + (ownerEmail == null || ownerEmail.isBlank() ? "<not set>" : "<set>")
                    + ", ownerPassword=" + (ownerPassword == null || ownerPassword.isBlank() ? "<not set>" : "****") + "]";
        }
    }
}
