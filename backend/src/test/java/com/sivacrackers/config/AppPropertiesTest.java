package com.sivacrackers.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Startup validation of security-relevant settings (no Spring context needed). */
class AppPropertiesTest {

    private static final String FAKE_32_BYTE_SECRET = "abcdefghijklmnopqrstuvwxyz012345"; // exactly 32 bytes, test only

    @Test
    @DisplayName("JWT secret: missing value is rejected with a clear message")
    void missingSecretRejected() {
        assertThatThrownBy(() -> new AppProperties.Jwt("  ", 60))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT_SECRET is not set");
        assertThatThrownBy(() -> new AppProperties.Jwt(null, 60))
                .hasMessageContaining("JWT_SECRET is not set");
    }

    @Test
    @DisplayName("JWT secret: shorter than 32 bytes is rejected, and the message never contains the secret")
    void shortSecretRejected() {
        String shortSecret = "short-secret-value";
        assertThatThrownBy(() -> new AppProperties.Jwt(shortSecret, 60))
                .hasMessageContaining("at least 32 bytes")
                .message().doesNotContain(shortSecret);
    }

    @Test
    @DisplayName("JWT secret: 32 bytes is accepted and toString() masks it")
    void validSecretMasked() {
        AppProperties.Jwt jwt = new AppProperties.Jwt(FAKE_32_BYTE_SECRET, 60);
        assertThat(jwt.toString()).doesNotContain(FAKE_32_BYTE_SECRET).contains("****");
    }

    @Test
    @DisplayName("Bootstrap owner: toString() never reveals email or password")
    void bootstrapMasked() {
        AppProperties.Bootstrap bootstrap = new AppProperties.Bootstrap("owner@example.test", "Sup3r-secret-pass");
        assertThat(bootstrap.isConfigured()).isTrue();
        assertThat(bootstrap.toString())
                .doesNotContain("owner@example.test")
                .doesNotContain("Sup3r-secret-pass");
        assertThat(new AppProperties.Bootstrap("", "").isConfigured()).isFalse();
    }

    @Test
    @DisplayName("CORS: blank entries removed; wildcard origins rejected")
    void corsOrigins() {
        assertThat(new AppProperties.Cors(List.of(" http://a.test ", "")).allowedOrigins()).containsExactly("http://a.test");
        assertThat(new AppProperties.Cors(null).allowedOrigins()).isEmpty();
        assertThatThrownBy(() -> new AppProperties.Cors(List.of("*")))
                .hasMessageContaining("wildcards are not allowed");
    }
}
