package com.sivacrackers.billing;

import com.sivacrackers.common.dto.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** In-store billing API bodies ({@code /api/admin/bills}, {@code /api/admin/payment-methods}). */
public final class BillingDtos {

    private BillingDtos() {
    }

    public enum DiscountType { AMOUNT, PERCENT }

    public record LineRequest(
            @NotNull(message = "is required") Long productId,
            @NotNull(message = "is required") @Min(value = 1, message = "must be at least 1")
            @Max(value = 10_000, message = "is too large") Integer quantity) {
    }

    /**
     * A bill to finalize. {@code requestKey} is a random id created by the billing screen for this bill; sending the same
     * key again (double click, network retry) returns the bill that was already saved instead of billing twice.
     */
    public record BillRequest(
            @NotBlank(message = "is required") @Pattern(regexp = "^[A-Za-z0-9-]{8,64}$", message = "is not valid") String requestKey,
            Long customerId,
            @NotEmpty(message = "add at least one product") @Size(max = 100, message = "at most 100 lines") List<@Valid LineRequest> items,
            DiscountType discountType,
            @DecimalMin(value = "0", message = "must not be negative") @Digits(integer = 10, fraction = 2, message = "must have at most 2 decimal places")
            BigDecimal discountValue,
            @NotBlank(message = "is required") String paymentMethod,
            @NotNull(message = "is required") Bill.PaymentStatus paymentStatus,
            @DecimalMin(value = "0", message = "must not be negative") @DecimalMax(value = "9999999999.99", message = "is too large")
            @Digits(integer = 10, fraction = 2, message = "must have at most 2 decimal places") BigDecimal amountReceived,
            @Size(max = 300, message = "must be at most 300 characters") String notes) {
    }

    public record VoidRequest(@NotBlank(message = "is required") @Size(max = 300, message = "must be at most 300 characters") String reason) {
    }

    public record PaymentUpdateRequest(
            @NotBlank(message = "is required") String paymentMethod,
            @DecimalMin(value = "0", message = "must not be negative") @Digits(integer = 10, fraction = 2, message = "must have at most 2 decimal places")
            BigDecimal amountReceived) {
    }

    public record PaymentMethodRequest(
            @NotBlank(message = "is required") @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,19}$", message = "must be 2–20 capital letters, digits or _")
            String code,
            @NotBlank(message = "is required") @Size(max = 60, message = "must be at most 60 characters") String label,
            @NotNull(message = "is required") @Min(value = 0, message = "must not be negative") @Max(value = 9999, message = "is too large") Integer displayOrder,
            @NotNull(message = "is required") Boolean active) {
    }

    public record PaymentMethodDto(Long id, String code, String label, int displayOrder, boolean active) {

        static PaymentMethodDto of(PaymentMethod m) {
            return new PaymentMethodDto(m.getId(), m.getCode(), m.getLabel(), m.getDisplayOrder(), m.isActive());
        }
    }

    public record BillLineDto(int lineNo, Long productId, String productName, String productSku, String packSize, String unit,
                              BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {

        static BillLineDto of(BillItem i) {
            return new BillLineDto(i.getLineNo(), i.getProductId(), i.getProductName(), i.getProductSku(), i.getPackSize(), i.getUnit(),
                    i.getUnitPrice(), i.getQuantity(), i.getLineTotal());
        }
    }

    public record BillDto(Long id, String billNumber, Bill.Status status, Long customerId, String customerName, String customerPhone,
                          BigDecimal subtotal, BigDecimal discountAmount, BigDecimal total, PaymentMethodDto paymentMethod,
                          Bill.PaymentStatus paymentStatus, BigDecimal amountReceived, BigDecimal changeDue, String notes,
                          String createdByName, Instant createdAt, Instant voidedAt, String voidedByName, String voidReason,
                          List<BillLineDto> items, boolean replayed) {

        static BillDto of(Bill b, boolean replayed) {
            BigDecimal change = b.getAmountReceived() == null || b.getPaymentStatus() != Bill.PaymentStatus.PAID ? null
                    : b.getAmountReceived().subtract(b.getTotal()).max(BigDecimal.ZERO);
            return new BillDto(b.getId(), b.getBillNumber(), b.getStatus(), b.getCustomerId(), b.getCustomerName(), b.getCustomerPhone(),
                    b.getSubtotal(), b.getDiscountAmount(), b.getTotal(), PaymentMethodDto.of(b.getPaymentMethod()), b.getPaymentStatus(),
                    b.getAmountReceived(), change, b.getNotes(), b.getCreatedByName(), b.getCreatedAt(), b.getVoidedAt(),
                    b.getVoidedByName(), b.getVoidReason(), b.getItems().stream().map(BillLineDto::of).toList(), replayed);
        }
    }

    /** One row of the bill history (no lines). */
    public record BillRow(Long id, String billNumber, Bill.Status status, String customerName, String customerPhone, int itemCount,
                          BigDecimal total, String paymentMethod, Bill.PaymentStatus paymentStatus, String createdByName,
                          Instant createdAt) {
    }

    /** A page of bills plus totals over every page: finalized bills only (voided ones never count as sales). */
    public record BillPage(PageResponse<BillRow> page, long finalizedCount, BigDecimal finalizedTotal, long voidedCount,
                           BigDecimal pendingTotal) {
    }
}
