package com.sivacrackers.security;

import com.sivacrackers.config.AppProperties;
import com.sivacrackers.user.Role;
import com.sivacrackers.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Signs and verifies session tokens (HS256 with JWT_SECRET).
 *
 * <p>The token only travels inside the HttpOnly session cookie; it is never returned in a
 * response body and never logged. Claims: subject = user id, role, ver = token version.
 */
@Service
public class JwtService {

    static final String ISSUER = "siva-crackers";

    private final SecretKey key;
    private final Duration lifetime;
    private final Clock clock;

    public JwtService(AppProperties props, Clock clock) {
        this.key = Keys.hmacShaKeyFor(props.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.lifetime = Duration.ofMinutes(props.jwt().expiryMinutes());
        this.clock = clock;
    }

    public Duration lifetime() {
        return lifetime;
    }

    public String issue(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(user.getId()))
                .claim("role", user.getRole().name())
                .claim("ver", user.getTokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(lifetime)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** Verified claims, or empty for any invalid, expired, tampered or foreign token. */
    public Optional<TokenClaims> verify(String token) {
        if (token == null || token.isBlank() || token.length() > 4096) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(ISSUER)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            long userId = Long.parseLong(claims.getSubject());
            Role role = Role.valueOf(claims.get("role", String.class));
            Integer version = claims.get("ver", Integer.class);
            return Optional.of(new TokenClaims(userId, role, version == null ? -1 : version));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public record TokenClaims(long userId, Role role, int version) {
    }
}
