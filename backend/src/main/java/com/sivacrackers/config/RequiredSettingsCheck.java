package com.sivacrackers.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Fails fast, before any bean is created, when required settings are missing.
 *
 * <p>Without this, Spring leaves an unset {@code ${DB_URL}} as literal text and the
 * failure surfaces much later as an unclear stack trace. This check names every
 * missing variable in one message. It reports names only – never values.
 *
 * <p>Registered in {@link com.sivacrackers.SivaCrackersApplication#main}, so it applies
 * to the real application but not to web-layer tests that run without a database.
 */
public class RequiredSettingsCheck implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    static final List<String> REQUIRED = List.of("DB_URL", "DB_USERNAME", "DB_PASSWORD", "JWT_SECRET");

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        verify(event.getEnvironment());
    }

    static void verify(ConfigurableEnvironment env) {
        List<String> problems = new ArrayList<>();
        for (String name : REQUIRED) {
            String value = env.getProperty(name);
            if (value == null || value.isBlank()) {
                problems.add(name + " is not set");
            }
        }
        String secret = env.getProperty("JWT_SECRET");
        if (secret != null && !secret.isBlank()
                && secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < AppProperties.MIN_SECRET_BYTES) {
            problems.add("JWT_SECRET is too short: it must be at least " + AppProperties.MIN_SECRET_BYTES + " bytes");
        }
        // Insecure (non-Secure) cookies are only ever allowed for local development.
        String cookieSecure = env.getProperty("APP_COOKIE_SECURE");
        boolean devProfile = java.util.Arrays.asList(env.getActiveProfiles()).contains("dev");
        if ("false".equalsIgnoreCase(cookieSecure) && !devProfile) {
            problems.add("APP_COOKIE_SECURE=false is only allowed with the dev profile (SPRING_PROFILES_ACTIVE=dev); "
                    + "production cookies must be Secure");
        }
        String url = env.getProperty("DB_URL");
        if (url != null && !url.isBlank() && !url.startsWith("jdbc:mysql://")) {
            problems.add("DB_URL must start with jdbc:mysql:// (for example jdbc:mysql://localhost:3306/siva_crackers)");
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException("SIVA Crackers backend cannot start - required settings are missing or invalid:\n  - "
                    + String.join("\n  - ", problems)
                    + "\nSet them as environment variables (see backend/README.md, section 'Configuration').");
        }
    }
}
