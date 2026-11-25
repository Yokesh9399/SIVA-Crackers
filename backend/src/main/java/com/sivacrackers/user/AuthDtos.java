package com.sivacrackers.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Request / response bodies for login and accounts.
 * No response ever contains a password, password hash or session token.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    /** Optional 10-digit mobile (spaces, dashes and +91 allowed; normalised on the server). */
    static final String PHONE = "^(\\+?91[\\s-]?|0)?[6-9][0-9]{4}[\\s-]?[0-9]{5}$";
    static final String PHONE_MESSAGE = "must be a 10-digit Indian mobile number";

    /** Public sign-up. There is deliberately no role field: every new account is a CUSTOMER. */
    public record RegisterRequest(
            @NotBlank(message = "is required") @Size(max = 100, message = "must be at most 100 characters") String fullName,
            @NotBlank(message = "is required") @Email(message = "must be a valid email address")
            @Size(max = 254, message = "is too long") String email,
            @Pattern(regexp = PHONE, message = PHONE_MESSAGE) String phone,
            @NotBlank(message = "is required") @Size(max = 200, message = "is too long") String password) {
    }

    /** Email address or 10-digit mobile number, plus password. */
    public record LoginRequest(
            @NotBlank(message = "is required") @Size(max = 254, message = "is too long") String identifier,
            @NotBlank(message = "is required") @Size(max = 200, message = "is too long") String password) {
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "is required") @Size(max = 100, message = "must be at most 100 characters") String fullName,
            @Pattern(regexp = PHONE, message = PHONE_MESSAGE) String phone) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "is required") @Size(max = 200, message = "is too long") String currentPassword,
            @NotBlank(message = "is required") @Size(max = 200, message = "is too long") String newPassword) {
    }

    /** OWNER-only staff provisioning: role must be MANAGER or CASHIER. */
    public record CreateStaffRequest(
            @NotBlank(message = "is required") @Size(max = 100, message = "must be at most 100 characters") String fullName,
            @NotBlank(message = "is required") @Email(message = "must be a valid email address")
            @Size(max = 254, message = "is too long") String email,
            @Pattern(regexp = PHONE, message = PHONE_MESSAGE) String phone,
            @NotBlank(message = "is required") @Size(max = 200, message = "is too long") String password,
            @NotNull(message = "is required") Role role) {
    }

    /** The current user, safe to show in the browser. */
    public record UserResponse(Long id, String fullName, String email, String phone, Role role, Instant createdAt) {

        public static UserResponse of(User u) {
            return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getRole(), u.getCreatedAt());
        }
    }

    /** Who is logged in, for the website on page load: {@code user} is null for visitors (always 200). */
    public record SessionResponse(UserResponse user) {
    }

    /** Tells the frontend which header carries the CSRF token (the token itself is in the XSRF-TOKEN cookie). */
    public record CsrfResponse(String headerName) {
    }
}
