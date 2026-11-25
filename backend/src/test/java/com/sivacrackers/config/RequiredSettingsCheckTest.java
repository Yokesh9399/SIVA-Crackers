package com.sivacrackers.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class RequiredSettingsCheckTest {

    @Test
    @DisplayName("All four required settings missing → one clear message naming each")
    void allMissing() {
        assertThatThrownBy(() -> RequiredSettingsCheck.verify(new MockEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_URL is not set")
                .hasMessageContaining("DB_USERNAME is not set")
                .hasMessageContaining("DB_PASSWORD is not set")
                .hasMessageContaining("JWT_SECRET is not set")
                .hasMessageContaining("backend/README.md");
    }

    @Test
    @DisplayName("Non-MySQL URL is rejected; values are never printed")
    void wrongUrlNoValuesPrinted() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("DB_URL", "jdbc:postgresql://x/y")
                .withProperty("DB_USERNAME", "someone")
                .withProperty("DB_PASSWORD", "pw-value-should-not-appear")
                .withProperty("JWT_SECRET", "secret-value-should-not-appear-0123456789");
        assertThatThrownBy(() -> RequiredSettingsCheck.verify(env))
                .hasMessageContaining("DB_URL must start with jdbc:mysql://")
                .message()
                .doesNotContain("pw-value-should-not-appear")
                .doesNotContain("secret-value-should-not-appear")
                .doesNotContain("postgresql");
    }

    @Test
    @DisplayName("Short JWT secret is reported before any database connection is attempted")
    void shortSecret() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("DB_URL", "jdbc:mysql://localhost:3306/siva_crackers")
                .withProperty("DB_USERNAME", "siva_app")
                .withProperty("DB_PASSWORD", "x")
                .withProperty("JWT_SECRET", "short-value");
        assertThatThrownBy(() -> RequiredSettingsCheck.verify(env))
                .hasMessageContaining("JWT_SECRET is too short")
                .message().doesNotContain("short-value");
    }

    @Test
    @DisplayName("APP_COOKIE_SECURE=false is refused outside the dev profile")
    void insecureCookieOnlyInDev() {
        MockEnvironment prod = completeEnv().withProperty("APP_COOKIE_SECURE", "false");
        assertThatThrownBy(() -> RequiredSettingsCheck.verify(prod))
                .hasMessageContaining("APP_COOKIE_SECURE=false is only allowed with the dev profile");

        MockEnvironment dev = completeEnv().withProperty("APP_COOKIE_SECURE", "false");
        dev.setActiveProfiles("dev");
        assertThatCode(() -> RequiredSettingsCheck.verify(dev)).doesNotThrowAnyException();

        MockEnvironment secureProd = completeEnv().withProperty("APP_COOKIE_SECURE", "true");
        assertThatCode(() -> RequiredSettingsCheck.verify(secureProd)).doesNotThrowAnyException();
    }

    private static MockEnvironment completeEnv() {
        return new MockEnvironment()
                .withProperty("DB_URL", "jdbc:mysql://localhost:3306/siva_crackers")
                .withProperty("DB_USERNAME", "siva_app")
                .withProperty("DB_PASSWORD", "x")
                .withProperty("JWT_SECRET", "a-fake-test-value-that-is-long-enough-0123");
    }

    @Test
    @DisplayName("Complete settings pass")
    void complete() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("DB_URL", "jdbc:mysql://localhost:3306/siva_crackers")
                .withProperty("DB_USERNAME", "siva_app")
                .withProperty("DB_PASSWORD", "x")
                .withProperty("JWT_SECRET", "a-fake-test-value-that-is-long-enough-0123");
        assertThatCode(() -> RequiredSettingsCheck.verify(env)).doesNotThrowAnyException();
    }
}
