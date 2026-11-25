package com.sivacrackers.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.sivacrackers.config.AppProperties;
import com.sivacrackers.user.Role;
import com.sivacrackers.user.User;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private static final String SECRET = "test-only-fake-secret-not-for-production-0123456789";

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-25T10:00:00Z"));
    private final JwtService jwt = new JwtService(props(SECRET), clock);

    private static AppProperties props(String secret) {
        return new AppProperties(new AppProperties.Jwt(secret, 60),
                new AppProperties.AuthCookie("SIVA_SESSION", true, "Strict"),
                new AppProperties.Cors(List.of()), false, new AppProperties.Bootstrap("", ""));
    }

    private static User user(long id, Role role) {
        User u = new User("Test", "t@example.test", "$2a$04$abcdefghijklmnopqrstuuJ2b7Sd8n5Y1aQk0iZ0E0p1ZQnQ5yN5K", role);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    @Test
    @DisplayName("A token it issued verifies with the user id, role and version")
    void roundTrip() {
        String token = jwt.issue(user(42, Role.MANAGER));
        var claims = jwt.verify(token).orElseThrow();
        assertThat(claims.userId()).isEqualTo(42);
        assertThat(claims.role()).isEqualTo(Role.MANAGER);
        assertThat(claims.version()).isZero();
    }

    @Test
    @DisplayName("Expired tokens are rejected (60-minute lifetime)")
    void expired() {
        String token = jwt.issue(user(1, Role.CUSTOMER));
        clock.advance(Duration.ofMinutes(59));
        assertThat(jwt.verify(token)).isPresent();
        clock.advance(Duration.ofMinutes(2));
        assertThat(jwt.verify(token)).isEmpty();
    }

    @Test
    @DisplayName("Tampered payload (e.g. role changed to OWNER) is rejected")
    void tampered() {
        String[] parts = jwt.issue(user(1, Role.CUSTOMER)).split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8).replace("CUSTOMER", "OWNER");
        String forged = parts[0] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + parts[2];
        assertThat(jwt.verify(forged)).isEmpty();
    }

    @Test
    @DisplayName("Tokens signed with another secret are rejected")
    void otherSecret() {
        JwtService other = new JwtService(props("a-completely-different-secret-value-9876543210"), clock);
        assertThat(jwt.verify(other.issue(user(1, Role.OWNER)))).isEmpty();
    }

    @Test
    @DisplayName("Unsigned 'alg: none' tokens and garbage are rejected")
    void unsignedAndGarbage() {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String body = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"iss\":\"siva-crackers\",\"sub\":\"1\",\"role\":\"OWNER\",\"ver\":0}".getBytes(StandardCharsets.UTF_8));
        assertThat(jwt.verify(header + "." + body + ".")).isEmpty();
        assertThat(jwt.verify("not-a-token")).isEmpty();
        assertThat(jwt.verify("")).isEmpty();
        assertThat(jwt.verify(null)).isEmpty();
        assertThat(jwt.verify("x".repeat(5000))).isEmpty();
    }
}
