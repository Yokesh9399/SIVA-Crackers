-- =====================================================================
-- SIVA Crackers – V5 internal in-store billing (Phase 6, approved 26 Nov 2026)
--
-- Staff-only counter billing inside the admin dashboard. Records payments received
-- AT THE SHOP (cash / UPI / card / other) – no payment gateway, no online payment,
-- no public ordering. Finalized bills are never edited or deleted: they can only be
-- voided (with a reason), which returns the items to stock.
-- =====================================================================

CREATE TABLE payment_methods (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    code           VARCHAR(20) NOT NULL,
    label          VARCHAR(60) NOT NULL,
    display_order  INT         NOT NULL DEFAULT 0,
    active         BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    CONSTRAINT pk_payment_methods PRIMARY KEY (id),
    CONSTRAINT uq_payment_methods_code UNIQUE (code),
    CONSTRAINT uq_payment_methods_label UNIQUE (label),
    CONSTRAINT ck_payment_methods_code CHECK (REGEXP_LIKE(code, '^[A-Z][A-Z0-9_]*$', 'c'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- One running number per calendar year: bill numbers look like SC-2026-000001.
CREATE TABLE bill_counters (
    bill_year    INT    NOT NULL,
    last_number  BIGINT NOT NULL,
    CONSTRAINT pk_bill_counters PRIMARY KEY (bill_year),
    CONSTRAINT ck_bill_counters_number CHECK (last_number >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE bills (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    bill_number         VARCHAR(20)   NOT NULL,
    request_key         VARCHAR(64)   NOT NULL,          -- sent by the billing screen; a repeated submit returns the same bill
    status              VARCHAR(10)   NOT NULL DEFAULT 'FINALIZED',
    customer_id         BIGINT        NULL,
    customer_name       VARCHAR(100)  NULL,              -- copied at billing time
    customer_phone      VARCHAR(15)   NULL,
    subtotal            DECIMAL(12,2) NOT NULL,
    discount_amount     DECIMAL(12,2) NOT NULL DEFAULT 0,
    total               DECIMAL(12,2) NOT NULL,
    payment_method_id   BIGINT        NOT NULL,
    payment_status      VARCHAR(10)   NOT NULL,
    amount_received     DECIMAL(12,2) NULL,
    notes               VARCHAR(300)  NULL,
    created_by          BIGINT        NULL,
    created_by_name     VARCHAR(100)  NULL,
    voided_at           DATETIME(6)   NULL,
    voided_by_name      VARCHAR(100)  NULL,
    void_reason         VARCHAR(300)  NULL,
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    CONSTRAINT pk_bills PRIMARY KEY (id),
    CONSTRAINT uq_bills_number UNIQUE (bill_number),
    CONSTRAINT uq_bills_request_key UNIQUE (request_key),
    CONSTRAINT fk_bills_customer FOREIGN KEY (customer_id) REFERENCES shop_customers (id) ON DELETE SET NULL,
    CONSTRAINT fk_bills_payment_method FOREIGN KEY (payment_method_id) REFERENCES payment_methods (id) ON DELETE RESTRICT,
    CONSTRAINT fk_bills_user FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_bills_status CHECK (status IN ('FINALIZED', 'VOIDED')),
    CONSTRAINT ck_bills_payment_status CHECK (payment_status IN ('PAID', 'PENDING')),
    CONSTRAINT ck_bills_amounts CHECK (subtotal > 0 AND discount_amount >= 0 AND discount_amount <= subtotal
                                       AND total = subtotal - discount_amount),
    CONSTRAINT ck_bills_discount_limit CHECK (discount_amount <= ROUND(subtotal * 0.20, 2)),
    CONSTRAINT ck_bills_received CHECK (amount_received IS NULL OR amount_received >= 0),
    CONSTRAINT ck_bills_void CHECK ((status = 'FINALIZED' AND voided_at IS NULL)
                                    OR (status = 'VOIDED' AND voided_at IS NOT NULL AND void_reason IS NOT NULL)),
    INDEX ix_bills_created (created_at),
    INDEX ix_bills_customer (customer_id, created_at),
    INDEX ix_bills_status_created (status, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE bill_items (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    bill_id       BIGINT        NOT NULL,
    line_no       INT           NOT NULL,
    product_id    BIGINT        NULL,                    -- NULL only if the product was deleted later
    product_name  VARCHAR(150)  NOT NULL,                -- copied at billing time
    product_sku   VARCHAR(40)   NOT NULL,
    pack_size     VARCHAR(50)   NOT NULL,
    unit          VARCHAR(10)   NOT NULL,
    unit_price    DECIMAL(10,2) NOT NULL,
    quantity      INT           NOT NULL,
    line_total    DECIMAL(12,2) NOT NULL,
    CONSTRAINT pk_bill_items PRIMARY KEY (id),
    CONSTRAINT uq_bill_items_line UNIQUE (bill_id, line_no),
    CONSTRAINT fk_bill_items_bill FOREIGN KEY (bill_id) REFERENCES bills (id) ON DELETE RESTRICT,
    CONSTRAINT fk_bill_items_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE SET NULL,
    CONSTRAINT ck_bill_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_bill_items_price CHECK (unit_price >= 0),
    CONSTRAINT ck_bill_items_total CHECK (line_total = unit_price * quantity),
    INDEX ix_bill_items_product (product_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Stock changes caused by a bill (sale) or a void (items back to stock) point to the bill.
ALTER TABLE stock_movements
    ADD COLUMN bill_id BIGINT NULL AFTER supplier_id,
    ADD CONSTRAINT fk_stock_movements_bill FOREIGN KEY (bill_id) REFERENCES bills (id) ON DELETE RESTRICT,
    ADD INDEX ix_stock_movements_bill (bill_id);

-- Payment methods offered at the shop counter (labels only; the owner can rename, add or switch them off).
INSERT INTO payment_methods (code, label, display_order, created_at, updated_at) VALUES
    ('CASH',  'Cash',                   1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('UPI',   'UPI (paid at the shop)', 2, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('CARD',  'Card (paid at the shop)', 3, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('OTHER', 'Other (in-store)',       4, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
