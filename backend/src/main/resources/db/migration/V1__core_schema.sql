-- =====================================================================
-- SIVA Crackers – V1 core schema (Phase 4.2)
--
-- Information catalogue + business settings + user accounts.
-- Deliberately NO order, cart, checkout, payment, booking or reservation
-- tables: firecrackers are only sold in person at the shop.
--
-- Conventions
--   * InnoDB, utf8mb4_0900_ai_ci (case- and accent-insensitive comparisons,
--     so UNIQUE(email) / UNIQUE(slug) are case-insensitive too)
--   * money: DECIMAL(10,2); never negative; indicative price never above MRP
--   * created_at / updated_at on every top-level table (filled by JPA auditing, UTC)
--   * is_demo flags demonstration rows so they can never be mistaken for real data
--   * stock status is NOT stored: it is calculated from stock_quantity and
--     reorder_level, so it can never contradict the stock number
-- =====================================================================

-- ---------------------------------------------------------------------
-- Categories
-- ---------------------------------------------------------------------
CREATE TABLE categories (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    name           VARCHAR(80)  NOT NULL,
    slug           VARCHAR(100) NOT NULL,
    description    VARCHAR(500) NULL,
    image_url      VARCHAR(500) NULL,
    display_order  INT          NOT NULL DEFAULT 0,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    is_demo        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uq_categories_name UNIQUE (name),
    CONSTRAINT uq_categories_slug UNIQUE (slug),
    CONSTRAINT ck_categories_slug CHECK (REGEXP_LIKE(slug, '^[a-z0-9]+(-[a-z0-9]+)*$', 'c')),
    CONSTRAINT ck_categories_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    INDEX ix_categories_active_order (active, display_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Ordered safety notes per category (shown on every product page of that category)
CREATE TABLE category_safety_notes (
    category_id  BIGINT       NOT NULL,
    position     INT          NOT NULL,
    note         VARCHAR(300) NOT NULL,
    CONSTRAINT pk_category_safety_notes PRIMARY KEY (category_id, position),
    CONSTRAINT fk_safety_notes_category FOREIGN KEY (category_id)
        REFERENCES categories (id) ON DELETE CASCADE,
    CONSTRAINT ck_safety_notes_position CHECK (position >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- Brands
-- ---------------------------------------------------------------------
CREATE TABLE brands (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    name           VARCHAR(80)  NOT NULL,
    slug           VARCHAR(100) NOT NULL,
    description    VARCHAR(500) NULL,
    logo_url       VARCHAR(500) NULL,
    display_order  INT          NOT NULL DEFAULT 0,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    is_demo        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    CONSTRAINT pk_brands PRIMARY KEY (id),
    CONSTRAINT uq_brands_name UNIQUE (name),
    CONSTRAINT uq_brands_slug UNIQUE (slug),
    CONSTRAINT ck_brands_slug CHECK (REGEXP_LIKE(slug, '^[a-z0-9]+(-[a-z0-9]+)*$', 'c')),
    CONSTRAINT ck_brands_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    INDEX ix_brands_active_order (active, display_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- Products
-- ---------------------------------------------------------------------
CREATE TABLE products (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    name                VARCHAR(150)  NOT NULL,
    slug                VARCHAR(170)  NOT NULL,
    sku                 VARCHAR(40)   NOT NULL,              -- product code shown to visitors
    category_id         BIGINT        NOT NULL,
    brand_id            BIGINT        NOT NULL,
    description         VARCHAR(2000) NULL,
    pack_size           VARCHAR(50)   NOT NULL,
    unit                VARCHAR(10)   NOT NULL,
    mrp                 DECIMAL(10,2) NOT NULL,
    price               DECIMAL(10,2) NOT NULL,              -- indicative selling price
    stock_quantity      INT           NOT NULL DEFAULT 0,
    reorder_level       INT           NOT NULL DEFAULT 10,   -- at or below this = "few left"
    featured            BOOLEAN       NOT NULL DEFAULT FALSE,
    visible_on_website  BOOLEAN       NOT NULL DEFAULT TRUE,
    active              BOOLEAN       NOT NULL DEFAULT TRUE,
    is_demo             BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uq_products_slug UNIQUE (slug),
    CONSTRAINT uq_products_sku UNIQUE (sku),
    -- A category or brand that still has products cannot be deleted (deactivate it instead).
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT,
    CONSTRAINT fk_products_brand    FOREIGN KEY (brand_id)    REFERENCES brands (id)     ON DELETE RESTRICT,
    CONSTRAINT ck_products_slug     CHECK (REGEXP_LIKE(slug, '^[a-z0-9]+(-[a-z0-9]+)*$', 'c')),
    CONSTRAINT ck_products_name     CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_products_sku      CHECK (CHAR_LENGTH(TRIM(sku)) > 0),
    CONSTRAINT ck_products_unit     CHECK (unit IN ('BOX', 'PACKET', 'PIECE', 'BUNDLE')),
    CONSTRAINT ck_products_mrp      CHECK (mrp >= 0),
    CONSTRAINT ck_products_price    CHECK (price >= 0),
    CONSTRAINT ck_products_price_le_mrp CHECK (price <= mrp),   -- never sell above MRP
    CONSTRAINT ck_products_stock    CHECK (stock_quantity >= 0),
    CONSTRAINT ck_products_reorder  CHECK (reorder_level >= 0),
    INDEX ix_products_category (category_id),
    INDEX ix_products_brand (brand_id),
    INDEX ix_products_price (price),
    INDEX ix_products_name (name),
    INDEX ix_products_created (created_at),
    INDEX ix_products_listing (active, visible_on_website, featured)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Ordered images per product (uploaded from the admin app in Phase 5)
CREATE TABLE product_images (
    product_id     BIGINT       NOT NULL,
    display_order  INT          NOT NULL,
    url            VARCHAR(500) NOT NULL,
    alt_text       VARCHAR(200) NULL,
    CONSTRAINT pk_product_images PRIMARY KEY (product_id, display_order),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT ck_product_images_order CHECK (display_order >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- Business settings (exactly one row, id = 1). Unknown details stay NULL –
-- they are shown on the website as "To be updated", never invented.
-- ---------------------------------------------------------------------
CREATE TABLE business_settings (
    id                        BIGINT        NOT NULL,
    shop_name                 VARCHAR(100)  NOT NULL,
    tagline                   VARCHAR(200)  NULL,
    address_line1             VARCHAR(200)  NULL,
    area                      VARCHAR(100)  NULL,
    city                      VARCHAR(100)  NULL,
    state                     VARCHAR(100)  NULL,
    pincode                   VARCHAR(10)   NULL,
    phone                     VARCHAR(20)   NULL,
    alternate_phone           VARCHAR(20)   NULL,
    whatsapp_number           VARCHAR(20)   NULL,
    whatsapp_enquiry_enabled  BOOLEAN       NOT NULL DEFAULT TRUE,
    email                     VARCHAR(254)  NULL,
    business_hours            VARCHAR(200)  NULL,
    map_embed_url             VARCHAR(500)  NULL,
    map_is_approximate        BOOLEAN       NOT NULL DEFAULT TRUE,
    licence_details           VARCHAR(300)  NULL,
    announcement_text         VARCHAR(300)  NULL,
    announcement_active       BOOLEAN       NOT NULL DEFAULT FALSE,
    about_text                VARCHAR(2000) NULL,
    price_disclaimer          VARCHAR(500)  NULL,
    legal_notice              VARCHAR(1000) NULL,
    whatsapp_notice           VARCHAR(500)  NULL,
    created_at                DATETIME(6)   NOT NULL,
    updated_at                DATETIME(6)   NOT NULL,
    CONSTRAINT pk_business_settings PRIMARY KEY (id),
    CONSTRAINT ck_business_settings_single_row CHECK (id = 1),
    CONSTRAINT ck_business_settings_pincode CHECK (pincode IS NULL OR REGEXP_LIKE(pincode, '^[0-9]{6}$')),
    CONSTRAINT ck_business_settings_map CHECK (map_embed_url IS NULL OR map_embed_url LIKE 'https://%')
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE business_highlights (
    settings_id  BIGINT       NOT NULL,
    position     INT          NOT NULL,
    title        VARCHAR(80)  NOT NULL,
    text         VARCHAR(300) NOT NULL,
    CONSTRAINT pk_business_highlights PRIMARY KEY (settings_id, position),
    CONSTRAINT fk_highlights_settings FOREIGN KEY (settings_id)
        REFERENCES business_settings (id) ON DELETE CASCADE,
    CONSTRAINT ck_highlights_position CHECK (position >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE safety_tips (
    settings_id  BIGINT       NOT NULL,
    position     INT          NOT NULL,
    text         VARCHAR(300) NOT NULL,
    CONSTRAINT pk_safety_tips PRIMARY KEY (settings_id, position),
    CONSTRAINT fk_safety_tips_settings FOREIGN KEY (settings_id)
        REFERENCES business_settings (id) ON DELETE CASCADE,
    CONSTRAINT ck_safety_tips_position CHECK (position >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- Homepage banners. Links must be internal site paths (no external URLs).
-- ---------------------------------------------------------------------
CREATE TABLE banners (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    eyebrow        VARCHAR(80)  NULL,
    title          VARCHAR(120) NOT NULL,
    subtitle       VARCHAR(300) NULL,
    cta_label      VARCHAR(40)  NULL,
    cta_link       VARCHAR(200) NULL,
    theme          VARCHAR(10)  NOT NULL DEFAULT 'gold',
    display_order  INT          NOT NULL DEFAULT 0,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    is_demo        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    CONSTRAINT pk_banners PRIMARY KEY (id),
    CONSTRAINT ck_banners_theme CHECK (theme IN ('gold', 'orange', 'blue')),
    CONSTRAINT ck_banners_link CHECK (cta_link IS NULL OR (cta_link LIKE '/%' AND cta_link NOT LIKE '//%')),
    CONSTRAINT ck_banners_title CHECK (CHAR_LENGTH(TRIM(title)) > 0),
    INDEX ix_banners_active_order (active, display_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- User accounts (customers and staff). Passwords are BCrypt hashes only.
-- Public registration creates CUSTOMER only (enforced in the service, Phase 4.5).
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    full_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(254) NOT NULL,
    phone          VARCHAR(15)  NULL,
    password_hash  VARCHAR(100) NOT NULL,
    role           VARCHAR(16)  NOT NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at  DATETIME(6)  NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_phone UNIQUE (phone),
    CONSTRAINT ck_users_role CHECK (role IN ('OWNER', 'MANAGER', 'CASHIER', 'CUSTOMER')),
    CONSTRAINT ck_users_email CHECK (email LIKE '%_@_%'),
    CONSTRAINT ck_users_phone CHECK (phone IS NULL OR REGEXP_LIKE(phone, '^[0-9]{10}$')),
    -- BCrypt hashes are 60 characters and start with $2a$, $2b$ or $2y$
    CONSTRAINT ck_users_password_hash CHECK (REGEXP_LIKE(password_hash, '^\\$2[aby]\\$[0-9]{2}\\$.{53}$', 'c')),
    CONSTRAINT ck_users_name CHECK (CHAR_LENGTH(TRIM(full_name)) > 0),
    INDEX ix_users_role (role)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
