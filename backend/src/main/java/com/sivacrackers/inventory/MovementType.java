package com.sivacrackers.inventory;

/** Direction of a stock change. */
public enum MovementType {
    /** Stock received (quantity goes up). */
    STOCK_IN,
    /** Stock leaving the shop (quantity goes down). */
    STOCK_OUT,
    /** Correction to a counted / known quantity (up or down). */
    ADJUSTMENT
}
