package com.sivacrackers.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** The logged-in user of the current request (set by {@link JwtAuthenticationFilter}). */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<AuthUser> get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof AuthUser user ? Optional.of(user) : Optional.empty();
    }

    public static Long idOrNull() {
        return get().map(AuthUser::id).orElse(null);
    }

    public static String nameOrNull() {
        return get().map(AuthUser::fullName).orElse(null);
    }
}
