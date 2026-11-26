package com.sivacrackers.supplier;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/** Supplier API bodies ({@code /api/admin/suppliers}). */
public final class SupplierDtos {

    private SupplierDtos() {
    }

    static final String PHONE = "^$|^(\\+?91[\\s-]?|0)?[6-9][0-9]{4}[\\s-]?[0-9]{5}$";

    public record SupplierRequest(
            @NotBlank(message = "is required") @Size(max = 120, message = "must be at most 120 characters") String name,
            @Size(max = 100, message = "must be at most 100 characters") String contactPerson,
            @Pattern(regexp = PHONE, message = "must be a 10-digit Indian mobile number") String phone,
            @Email(message = "must be a valid email address") @Size(max = 254, message = "is too long") String email,
            @Size(max = 300, message = "must be at most 300 characters") String address,
            @Size(max = 1000, message = "must be at most 1000 characters") String notes) {
    }

    public record StatusRequest(@NotNull(message = "is required") Boolean active) {
    }

    public record SupplierDto(Long id, String name, String contactPerson, String phone, String email, String address,
                              String notes, boolean active, boolean isDemo, Instant createdAt, Instant updatedAt) {

        static SupplierDto of(Supplier s) {
            return new SupplierDto(s.getId(), s.getName(), s.getContactPerson(), s.getPhone(), s.getEmail(), s.getAddress(),
                    s.getNotes(), s.isActive(), s.isDemo(), s.getCreatedAt(), s.getUpdatedAt());
        }
    }

    /** Supplier with its purchase history totals (from stock received). */
    public record SupplierDetail(SupplierDto supplier, long deliveries, long unitsReceived, BigDecimal purchaseValue,
                                 Instant lastDeliveryAt) {
    }
}
