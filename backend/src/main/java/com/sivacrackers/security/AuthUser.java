package com.sivacrackers.security;

import com.sivacrackers.user.Role;

/** The logged-in user as seen by controllers (@AuthenticationPrincipal). Holds no password data. */
public record AuthUser(Long id, String email, String fullName, Role role) {
}
