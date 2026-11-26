-- =====================================================================
-- SIVA Crackers – V4 internal shop management (Phase 6) + audit log (Phase 5)
--
-- Staff-only data, never shown on the public website:
--   audit_log         who did what (staff changes, stock, suppliers, expenses, customers, deletes)
--   suppliers         where stock comes from
--   stock_movements   every change to products.stock_quantity, with before/after quantities
--   expense_categories, expenses   shop running costs (cancelled, never hard-deleted)
--   shop_customers    contact records kept by the shop (no purchase data)
--
-- Deliberately still NO order, cart, checkout, payment, booking, reservation,
-- sales or invoice tables (in-store billing is not approved).
-- Existing tables are not changed.
-- =====================================================================

CREATE TABLE audit_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    occurred_at   DATETIME(6)  NOT NULL,
    actor_id      BIGINT       NULL,
    actor_name    VARCHAR(100) NULL,                 -- kept if the account is removed later
    action        VARCHAR(60)  NOT NULL,             -- e.g. STAFF_CREATED, EXPENSE_CANCELLED
    entity_type   VARCHAR(40)  NOT NULL,
    entity_id     BIGINT       NULL,
    details       VARCHAR(1000) NULL,                -- never passwords or tokens
    CONSTRAINT pk_audit_log PRIMARY KEY (id),
    CONSTRAINT fk_audit_log_actor FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX ix_audit_log_time (occurred_at),
    INDEX ix_audit_log_entity (entity_type, entity_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE suppliers (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    name            VARCHAR(120) NOT NULL,
    contact_person  VARCHAR(100) NULL,
    phone           VARCHAR(15)  NULL,
    email           VARCHAR(254) NULL,
    address         VARCHAR(300) NULL,
    notes           VARCHAR(1000) NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    is_demo         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_suppliers PRIMARY KEY (id),
    CONSTRAINT uq_suppliers_name UNIQUE (name),
    CONSTRAINT ck_suppliers_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_suppliers_phone CHECK (phone IS NULL OR REGEXP_LIKE(phone, '^[0-9]{10}$')),
    INDEX ix_suppliers_active (active)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE stock_movements (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    product_id         BIGINT        NULL,               -- NULL only if the product was deleted later
    product_name       VARCHAR(150)  NOT NULL,           -- snapshot, so history survives a product delete
    product_sku        VARCHAR(40)   NOT NULL,
    movement_type      VARCHAR(12)   NOT NULL,
    reason             VARCHAR(24)   NOT NULL,
    previous_quantity  INT           NOT NULL,
    quantity_change    INT           NOT NULL,           -- + for stock in, - for stock out
    new_quantity       INT           NOT NULL,
    supplier_id        BIGINT        NULL,
    unit_cost          DECIMAL(10,2) NULL,               -- purchase cost per unit, stock in only
    note               VARCHAR(500)  NULL,
    performed_by       BIGINT        NULL,
    performed_by_name  VARCHAR(100)  NULL,
    created_at         DATETIME(6)   NOT NULL,
    CONSTRAINT pk_stock_movements PRIMARY KEY (id),
    CONSTRAINT fk_stock_movements_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE SET NULL,
    CONSTRAINT fk_stock_movements_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE RESTRICT,
    CONSTRAINT fk_stock_movements_user FOREIGN KEY (performed_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_stock_movements_type CHECK (movement_type IN ('STOCK_IN', 'STOCK_OUT', 'ADJUSTMENT')),
    CONSTRAINT ck_stock_movements_math CHECK (new_quantity = previous_quantity + quantity_change),
    CONSTRAINT ck_stock_movements_never_negative CHECK (previous_quantity >= 0 AND new_quantity >= 0),
    CONSTRAINT ck_stock_movements_direction CHECK (
        (movement_type = 'STOCK_IN' AND quantity_change > 0) OR
        (movement_type = 'STOCK_OUT' AND quantity_change < 0) OR
        (movement_type = 'ADJUSTMENT' AND quantity_change <> 0)),
    CONSTRAINT ck_stock_movements_cost CHECK (unit_cost IS NULL OR unit_cost >= 0),
    INDEX ix_stock_movements_product_time (product_id, created_at),
    INDEX ix_stock_movements_time (created_at),
    INDEX ix_stock_movements_supplier (supplier_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE expense_categories (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    name           VARCHAR(60) NOT NULL,
    display_order  INT         NOT NULL DEFAULT 0,
    active         BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    CONSTRAINT pk_expense_categories PRIMARY KEY (id),
    CONSTRAINT uq_expense_categories_name UNIQUE (name),
    CONSTRAINT ck_expense_categories_name CHECK (CHAR_LENGTH(TRIM(name)) > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE expenses (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    category_id        BIGINT        NOT NULL,
    expense_date       DATE          NOT NULL,
    amount             DECIMAL(12,2) NOT NULL,
    description        VARCHAR(200)  NOT NULL,
    paid_to            VARCHAR(120)  NULL,
    reference          VARCHAR(60)   NULL,              -- bill / receipt number
    notes              VARCHAR(500)  NULL,
    status             VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE',
    cancel_reason      VARCHAR(300)  NULL,
    cancelled_at       DATETIME(6)   NULL,
    recorded_by        BIGINT        NULL,
    recorded_by_name   VARCHAR(100)  NULL,
    created_at         DATETIME(6)   NOT NULL,
    updated_at         DATETIME(6)   NOT NULL,
    CONSTRAINT pk_expenses PRIMARY KEY (id),
    CONSTRAINT fk_expenses_category FOREIGN KEY (category_id) REFERENCES expense_categories (id) ON DELETE RESTRICT,
    CONSTRAINT fk_expenses_user FOREIGN KEY (recorded_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_expenses_amount CHECK (amount > 0),
    CONSTRAINT ck_expenses_status CHECK (status IN ('ACTIVE', 'CANCELLED')),
    CONSTRAINT ck_expenses_cancel CHECK (
        (status = 'ACTIVE' AND cancelled_at IS NULL) OR
        (status = 'CANCELLED' AND cancelled_at IS NOT NULL AND cancel_reason IS NOT NULL)),
    CONSTRAINT ck_expenses_description CHECK (CHAR_LENGTH(TRIM(description)) > 0),
    INDEX ix_expenses_date (expense_date),
    INDEX ix_expenses_category_date (category_id, expense_date),
    INDEX ix_expenses_status_date (status, expense_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE shop_customers (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    full_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(15)  NULL,
    email           VARCHAR(254) NULL,
    address         VARCHAR(300) NULL,
    notes           VARCHAR(500) NULL,
    created_by      BIGINT       NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_shop_customers PRIMARY KEY (id),
    CONSTRAINT uq_shop_customers_phone UNIQUE (phone),
    CONSTRAINT fk_shop_customers_user FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_shop_customers_name CHECK (CHAR_LENGTH(TRIM(full_name)) > 0),
    CONSTRAINT ck_shop_customers_phone CHECK (phone IS NULL OR REGEXP_LIKE(phone, '^[0-9]{10}$')),
    CONSTRAINT ck_shop_customers_contact CHECK (phone IS NOT NULL OR email IS NOT NULL),
    INDEX ix_shop_customers_name (full_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Expense categories suggested for the shop (labels only – no amounts or business data).
INSERT INTO expense_categories (name, display_order, created_at, updated_at) VALUES
    ('Shop rent',    1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Electricity',  2, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Transport',    3, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Packaging',    4, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Staff salary', 5, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Maintenance',  6, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Other',        7, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
