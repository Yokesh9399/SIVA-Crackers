package com.sivacrackers.inventory;

import com.sivacrackers.catalog.product.StockStatus;
import com.sivacrackers.common.dto.RefDto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Inventory API bodies ({@code /api/admin/inventory}). */
public final class InventoryDtos {

    private InventoryDtos() {
    }

    /**
     * Stock in / stock out: {@code quantity} is how many units come in or go out.
     * Adjustment: {@code quantity} is the correct quantity now on hand (e.g. after counting).
     */
    public record MovementRequest(
            @NotNull(message = "is required") Long productId,
            @NotNull(message = "is required") MovementReason reason,
            @NotNull(message = "is required") @Min(value = 0, message = "must not be negative")
            @Max(value = 1_000_000, message = "is too large") Integer quantity,
            Long supplierId,
            @DecimalMin(value = "0.00", message = "must not be negative")
            @Digits(integer = 8, fraction = 2, message = "must be an amount with at most 2 decimal places") BigDecimal unitCost,
            @Size(max = 500, message = "must be at most 500 characters") String note) {
    }

    public record ThresholdRequest(
            @NotNull(message = "is required") @Min(value = 0, message = "must not be negative")
            @Max(value = 100_000, message = "is too large") Integer reorderLevel) {
    }

    public record StockRow(Long productId, String name, String sku, RefDto category, RefDto brand, int stockQuantity,
                           int reorderLevel, StockStatus stockStatus, boolean active, boolean visibleOnWebsite, String imageUrl) {
    }

    public record MovementDto(Long id, Long productId, String productName, String productSku, MovementType type,
                              MovementReason reason, String reasonLabel, int previousQuantity, int quantityChange,
                              int newQuantity, RefDto supplier, BigDecimal unitCost, String note, String performedByName,
                              Instant createdAt) {

        static MovementDto of(StockMovement m) {
            RefDto supplier = m.getSupplier() == null ? null : new RefDto(m.getSupplier().getId(), m.getSupplier().getName(), null);
            return new MovementDto(m.getId(), m.getProductId(), m.getProductName(), m.getProductSku(), m.getType(),
                    m.getReason(), m.getReason().label(), m.getPreviousQuantity(), m.getQuantityChange(), m.getNewQuantity(),
                    supplier, m.getUnitCost(), m.getNote(), m.getPerformedByName(), m.getCreatedAt());
        }
    }

    public record Alerts(long lowStockCount, long outOfStockCount, List<StockRow> lowStock, List<StockRow> outOfStock) {
    }

    public record ReasonDto(MovementReason code, MovementType type, String label) {
    }
}
