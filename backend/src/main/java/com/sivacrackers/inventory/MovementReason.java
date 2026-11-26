package com.sivacrackers.inventory;

/** Why stock changed. Each reason belongs to one movement type. */
public enum MovementReason {
    PURCHASE(MovementType.STOCK_IN, "Received from supplier"),
    CUSTOMER_RETURN(MovementType.STOCK_IN, "Returned by a customer"),
    OTHER_IN(MovementType.STOCK_IN, "Other stock in"),

    SOLD_AT_SHOP(MovementType.STOCK_OUT, "Sold at the shop counter"),
    DAMAGED(MovementType.STOCK_OUT, "Damaged"),
    RETURNED_TO_SUPPLIER(MovementType.STOCK_OUT, "Returned to supplier"),
    EXPIRED(MovementType.STOCK_OUT, "Expired / unsafe"),
    OTHER_OUT(MovementType.STOCK_OUT, "Other stock out"),

    STOCK_COUNT(MovementType.ADJUSTMENT, "Stock count correction"),
    PRODUCT_EDIT(MovementType.ADJUSTMENT, "Changed in the product form"),
    INITIAL_STOCK(MovementType.ADJUSTMENT, "Opening stock of a new product"),
    OTHER_ADJUSTMENT(MovementType.ADJUSTMENT, "Other adjustment");

    private final MovementType type;
    private final String label;

    MovementReason(MovementType type, String label) {
        this.type = type;
        this.label = label;
    }

    public MovementType type() {
        return type;
    }

    public String label() {
        return label;
    }
}
