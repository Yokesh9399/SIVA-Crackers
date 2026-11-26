package com.sivacrackers.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** OWNER staff management ({@code /api/admin/staff}). Responses never contain password hashes or tokens. */
public final class StaffDtos {

    private StaffDtos() {
    }

    public record StaffMember(Long id, String fullName, String email, String phone, Role role, boolean active,
                              Instant createdAt, Instant lastLoginAt) {

        public static StaffMember of(User u) {
            return new StaffMember(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getRole(), u.isActive(),
                    u.getCreatedAt(), u.getLastLoginAt());
        }
    }

    public record UpdateStaffRequest(
            @NotBlank(message = "is required") @Size(max = 100, message = "must be at most 100 characters") String fullName,
            @NotBlank(message = "is required") @Email(message = "must be a valid email address")
            @Size(max = 254, message = "is too long") String email,
            @Pattern(regexp = AuthDtos.PHONE, message = AuthDtos.PHONE_MESSAGE) String phone,
            @NotNull(message = "is required") Role role) {
    }

    public record StatusRequest(@NotNull(message = "is required") Boolean active) {
    }

    /** The OWNER sets a temporary password and gives it to the staff member in person. */
    public record ResetPasswordRequest(
            @NotBlank(message = "is required") @Size(max = 200, message = "is too long") String newPassword) {
    }
}
