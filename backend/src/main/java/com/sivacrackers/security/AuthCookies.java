package com.sivacrackers.security;

import com.sivacrackers.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * The session cookie that carries the login token.
 * HttpOnly (JavaScript can never read it) · Secure (APP_COOKIE_SECURE, true outside dev) ·
 * SameSite=Strict · Path=/api (only sent to the API) · Max-Age = token lifetime.
 */
@Component
public class AuthCookies {

    private final AppProperties props;

    public AuthCookies(AppProperties props) {
        this.props = props;
    }

    public void write(HttpServletResponse response, String token, Duration lifetime) {
        response.addHeader(HttpHeaders.SET_COOKIE, build(token, lifetime).toString());
    }

    /** Logout: an empty cookie with Max-Age=0 makes the browser delete it. */
    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, build("", Duration.ZERO).toString());
    }

    public String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (props.cookie().name().equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(props.cookie().name(), value)
                .httpOnly(true)
                .secure(props.cookie().secure())
                .sameSite(props.cookie().sameSite())
                .path("/api")
                .maxAge(maxAge)
                .build();
    }
}
