package com.sivacrackers.catalog.product;

/**
 * Stock availability shown on the website. Never stored: always calculated from
 * stock quantity and reorder level, so it cannot contradict the stock number.
 */
public enum StockStatus {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK;

    public static StockStatus of(int stockQuantity, int reorderLevel) {
        if (stockQuantity <= 0) {
            return OUT_OF_STOCK;
        }
        return stockQuantity <= reorderLevel ? LOW_STOCK : IN_STOCK;
    }
}
