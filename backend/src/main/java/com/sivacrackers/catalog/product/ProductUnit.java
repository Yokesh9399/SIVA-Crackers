package com.sivacrackers.catalog.product;

/** How a product is sold. Must match the CHECK constraint ck_products_unit. */
public enum ProductUnit {
    BOX,
    PACKET,
    PIECE,
    BUNDLE
}
