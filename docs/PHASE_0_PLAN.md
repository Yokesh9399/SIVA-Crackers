# SIVA Crackers – Phase 0 Planning Document

**Project:** SIVA Crackers – Product Catalogue & Business Management Platform
**Business:** SIVA Crackers, family-owned crackers retail shop, Eachanari, Coimbatore, Tamil Nadu
**Status:** Phase 0 (planning). No code written yet. Awaiting approval.
**Date:** 22 Nov 2026

---

## Table of contents

1. Reference website analysis
2. Features adopted, adapted and excluded (please review)
3. Detailed feature list for SIVA Crackers
4. System architecture
5. Frontend folder structure (public site and admin app)
6. Reusable React components
7. Backend folder structure
8. Database schema
9. API development plan
10. Design system
11. Legal and safety approach
12. Development roadmap (Phases 1–8)
13. Open questions for you

---

## 1. Reference website analysis

Reference: `archanfireworks.in/whatsapp-catalog` and its linked pages (home, categories, login). This section covers what the site **does**, not how it looks. None of its code, branding, text or images will be reused.

### 1.1 Navigation structure
| Area | What it has |
|---|---|
| Top nav | Categories, Order (full catalogue / price list), Chit Fund, Blog |
| Header icons | WhatsApp chat link, Login, Cart with item count |
| Mobile menu | Slide-in panel with **Categories** and **Brands** tabs |
| Footer nav | Home, Categories, Order, Login, plus quick links (Privacy, Terms, Contact, Blogs, Videos, Images, Refund policy) |
| URL pattern | `/categories`, `/category/<slug>`, `/product/<slug>`, `/search?sort_by=newest` |

### 1.2 Homepage layout (top to bottom)
1. Announcement bar (season message + legal compliance notice)
2. Brand row and a trust strip (4 short value statements)
3. Hero banner
4. Three quick-action tiles (full price list, chat, call)
5. Promotional block (chit fund)
6. "Shop by category" tiles
7. Horizontal product rows per theme, each with a "See all" link (Best sellers, several categories, New arrivals)
8. Small feature strip
9. Footer: location, most-viewed categories, most-viewed products, blog links, SEO text

### 1.3 Product catalogue and product cards
- Card fields: image, discount badge, product name, pack descriptor (Box / Pkt / Pc / "10 Pcs"), price, struck-through MRP, brand line ("By: …"), Add button + quantity stepper, "Sold out" state.
- Category is not on the card. It comes from section headings.
- Placeholder image when no photo exists.

### 1.4 Search and filtering
- Text search with an empty state ("no products found") and a clear-search action.
- Brand filter shown as a removable chip.
- Sort by newest / best sellers.
- No page-number pagination. The catalogue is one long list grouped by category.

### 1.5 Price list
- A long card-based list grouped under category headings.
- MRP struck through, discounted price highlighted.
- Cart running total with a minimum order threshold.

### 1.6 Product detail
- `/product/<slug>` pages with name, pack size, brand, price, MRP and an Add button.

### 1.7 Login and signup
- Login with name + phone number, "Remember me", link to register.
- Register with name + phone + terms checkbox.
- A separate account for the chit fund scheme.

### 1.8 Footer and business info
- Address, phone (click-to-call), email, WhatsApp, social links, copyright.
- No licence number or GST number displayed.

### 1.9 Observations that shape our plan
- The reference shows a "product details and price estimates only" legal notice, **but also** has a cart, checkout, WhatsApp ordering and invoices. Those two things contradict each other. Your brief rules out online ordering, so SIVA Crackers will **not** copy the ordering features (see Section 2).
- The reference has no page-number pagination, no price filter and no alphabetical sort. **Your brief asks for these, so we add them.**
- The reference has no dedicated safety information. **We add it** on product pages and a Safety page.

---

## 2. Features adopted, adapted and excluded (please review)

Your rule 4 says not to remove reference features without discussing them. These are the decisions that need your sign-off.

### 2.1 Adopted (same functionality, original design)
- Announcement bar, hero banner, category tiles, themed product rows with "See all"
- Category and brand browsing, mobile menu with Categories / Brands tabs
- Product cards with image, name, brand, pack size, MRP and display price, "Out of stock" state
- Search with empty state and clear action, brand filter chips, sorting
- Category-grouped price list
- Product detail page with SEO-friendly slugs (`/products/<slug>`)
- Login / signup, footer with business info and quick links

### 2.2 Adapted
| Reference feature | SIVA Crackers version | Reason |
|---|---|---|
| Cart + "Add" + quantity stepper | **"My Estimate List"** (optional, see Q1): customer can note quantities and see an indicative total, then **print it or show it at the shop**. Nothing is submitted, booked or paid. Saved only in the browser. | Keeps the useful "work out my budget" feature without any online ordering |
| Discount badge "85% off" | MRP and **indicative price** fields. Discount badge shown only if the owner enters both values in admin. | Prices are your data, not invented |
| Long single-page list | Paginated catalogue (12/24/48 per page) **plus** a full single-page Price List | Brief asks for pagination; price list stays complete for printing |
| Login with name + phone, no password | Email or phone + password (BCrypt), profile, change password | Brief asks for password management |
| Card-based "Order on WhatsApp" page | Table-based **Price List** with category grouping, search and print layout | Brief asks for print-friendly layout |
| WhatsApp header icon | **Call** and **Visit shop (map)** buttons. WhatsApp link only if you confirm it is allowed (Q2). | Brief says not to assume WhatsApp enquiries are permitted |

### 2.3 Excluded (not built)
| Reference feature | Why excluded |
|---|---|
| Online checkout, delivery details, order-placed screen, online invoices | Prohibited by your brief (online cracker sale / payment / booking / delivery) |
| "Send order to WhatsApp" | Prohibited by your brief |
| Chit fund scheme and chit fund login | It is a savings / deposit scheme. These are regulated financial products and tied to pre-booking crackers. Out of scope. |
| Spin-to-win prizes | Linked to purchases. Not needed for a catalogue. |
| "Bid for product" | A form of online price negotiation / ordering |
| "Fast all-India delivery" claims | Not applicable; we will not invent business claims |
| Blog / videos | **Deferred**, not excluded. Can be added after Phase 8 if you want content pages. |

### 2.4 Payment gateway demo
Your brief allows an optional payment demo kept completely separate, using unrelated permitted demo products. **Recommendation:** leave it out of this project, or build it later as a separate mini-project, so the crackers site has no payment code at all. (Q3)

---

## 3. Detailed feature list for SIVA Crackers

### 3.1 Public website

**A. Header and navigation**
- Logo (original text/icon logo for SIVA Crackers, created by us)
- Menu: Home, All Products, Price List, Brands, Categories, About Us, Contact Us, Login / Signup (becomes "My Account" when logged in)
- Sticky header, quick search box, mobile slide-in menu with Categories / Brands tabs
- Persistent legal notice strip

**B. Homepage**
- Announcement bar (text managed from admin, can be switched off)
- Hero banner carousel (slides managed from admin)
- Quick tiles: Price List, Call Shop, Visit Shop (map), Safety Tips
- Shop by Category grid
- Featured Products row (products marked "featured" in admin)
- New Arrivals row
- Popular Brands strip
- About the shop (short text from admin)
- Why visit us / trust points (from admin; no invented claims)
- Contact section with address, phone, hours, map embed
- Footer

**C. Product catalogue (`/products`)**
- Card: image, name, brand, category tag, pack size, MRP, indicative price, stock badge (In stock / Low stock / Out of stock)
- Grid / list view toggle
- Product detail link on every card

**D. Search and filter**
- Search by product name (debounced, also in header)
- Filter by category (multi-select), brand (multi-select), price range (min/max slider + presets)
- "In stock only" toggle
- Sort: Name A–Z, Name Z–A, Price low–high, Price high–low, Newest
- Active filter chips with individual remove + "Clear all"
- Pagination with page size selector
- Filters stored in the URL (`?category=sparklers&brand=x&page=2`) so pages can be shared and the back button works
- Empty state with "Clear filters"

**E. Price list (`/price-list`)**
- Table grouped by category: S.No, Product, Brand, Pack size, MRP, Indicative price
- Search box and category jump links
- "Print price list" button with a dedicated print stylesheet (no header/footer clutter, page breaks per category, shop name, date and disclaimer on top)
- Price disclaimer: prices are indicative, may change without notice, confirm at the shop

**F. Product details (`/products/:slug`)**
- Image gallery (main image + thumbnails, zoom on click)
- Name, brand, category, pack size, MRP, indicative price, stock status
- Description
- Safety information (from product record, with a standard fallback text)
- Related products (same category)
- Breadcrumbs

**G. Business pages**
- About Us, Contact Us (address, phone, email, hours, map, contact form for general questions only, see Q2), Safety Guidelines, Legal Disclaimer, Privacy Policy, Terms of Use, 404 page

**H. Customer account**
- Register, login, logout
- Profile view / edit (name, email, phone)
- Change password
- Favourites / saved products (catalogue use only)
- My Estimate List if Q1 is approved
- No orders, no payments, no booking

### 3.2 Admin application

| Module | Features |
|---|---|
| Admin login | Separate login, JWT, role check, auto-logout on token expiry |
| Dashboard | Today's sales, month sales, expenses, low-stock alerts, top-selling products, recent sales, simple charts |
| Products | List with search/filter/pagination, create, edit, archive, image upload (multiple), featured flag, active/hidden flag, MRP, indicative price, purchase cost, reorder level, safety notes |
| Categories | CRUD, image, display order, active flag |
| Brands | CRUD, logo, display order, active flag |
| Inventory | Current stock per product, low-stock list, stock adjustment |
| Stock in / stock out | Record stock received (supplier, quantity, cost, date), damaged / returned / adjustments, full movement history per product |
| Shop sales (POS) | Counter-style billing screen: search product, add quantity, discount, tax, payment mode (cash / UPI / card, recorded only), customer link. Stock is deducted automatically. |
| Invoices | PDF invoice for each in-store sale (shop details, invoice number, items, totals), reprint, download |
| Customers | Shop customer records (name, phone, address optional), purchase history; also a read-only list of website accounts |
| Expenses | Expense categories, record expenses, filter by date / category |
| Reports | Daily, monthly, yearly: sales, expenses, gross profit, top products, category-wise sales, stock valuation. Export to PDF / CSV. |
| Staff | Create staff users, assign roles, activate / deactivate, reset password |
| Business settings | Shop name, address, phone, email, hours, map link, licence details, GST no. (optional), invoice prefix, tax %, announcement text, banners, About text |
| Role-based access | See matrix below |

**Role matrix**

| Role | Access |
|---|---|
| OWNER | Everything, including staff, settings, reports |
| MANAGER | Products, categories, brands, inventory, sales, customers, expenses, reports. No staff or settings. |
| CASHIER | Sales (billing), invoices, customer lookup, view stock |
| CUSTOMER | Public site account only. No admin access. |

---

## 4. System architecture

```
┌─────────────────────────┐     ┌─────────────────────────┐
│  Public website (React) │     │  Admin app (React)      │
│  frontend/  :5173       │     │  admin/  :5174          │
└───────────┬─────────────┘     └───────────┬─────────────┘
            │  HTTPS + JSON (Axios)          │  + JWT Bearer token
            ▼                                ▼
┌─────────────────────────────────────────────────────────┐
│  Spring Boot REST API   backend/  :8080                 │
│                                                         │
│  Controller  →  Service  →  Repository (Spring Data JPA)│
│      │             │                                    │
│     DTOs     business rules, validation, mapping        │
│                                                         │
│  Spring Security + JWT filter  │  Global exception      │
│  File storage (product images) │  PDF generation        │
└───────────────────────────┬─────────────────────────────┘
                            ▼
                   ┌─────────────────┐
                   │   MySQL 8       │
                   │  siva_crackers  │
                   └─────────────────┘
```

### Key decisions

1. **Two React apps (`frontend/` and `admin/`).** Your brief asks for a separate admin application. Keeping them apart means the public bundle never contains admin code, and each can be deployed on its own (for example `sivacrackers.in` and `admin.sivacrackers.in`). Shared pieces are small (API client, formatters), so a little duplication is simpler than a monorepo tool.
2. **One Spring Boot backend** serving both apps. URL prefixes separate them:
   - `/api/public/**` – no login needed (catalogue, price list, settings)
   - `/api/auth/**` – login / register
   - `/api/account/**` – logged-in customers
   - `/api/admin/**` – staff only, with role checks
3. **Stateless JWT authentication.** Short-lived access token (for example 60 min). Passwords hashed with BCrypt. Staff and customers share one `users` table with a `role` column; admin endpoints reject CUSTOMER tokens.
4. **Data layer before backend (Phases 1–3).** The UI is built before the backend exists. To avoid hardcoding data in components, the React apps call **service functions** (`productService.getProducts(filters)`). In Phases 1–3 these read demo JSON files through a fake API layer that mimics pagination and filtering. In Phase 4 the same functions switch to Axios calls. Components do not change.
5. **Stock is a ledger.** Every change to stock (stock in, sale, damage, adjustment) writes a `stock_movements` row, and `products.stock_quantity` is updated in the same database transaction. This gives a full audit trail and accurate reports.
6. **Prices stored as `DECIMAL(10,2)`** in MySQL and `BigDecimal` in Java (never floating point).
7. **Business information lives in the database** (`business_settings`), edited in admin. Until you give real details, the UI shows clearly marked placeholders such as "Phone number – to be updated", never invented values.
8. **Images** stored on disk under `backend/uploads/` in development and served at `/uploads/**`. The storage class sits behind an interface so it can move to cloud storage at deployment.
9. **Schema migrations with Flyway**, so the database structure is versioned in Git and demo data is loaded repeatably. (Flyway is a Spring Boot add-on, not a stack change; see Q6.)
10. **PDF invoices** generated on the backend with OpenPDF (open source), so invoices look the same everywhere.

---

## 5. Frontend folder structure

### 5.1 Public website – `frontend/`

```
frontend/
├── public/
│   ├── favicon.svg
│   └── images/                  # placeholder product art, banners (original)
├── src/
│   ├── main.jsx
│   ├── App.jsx                  # providers + router
│   ├── index.css                # Tailwind directives, base styles, print styles
│   ├── routes/
│   │   └── AppRoutes.jsx        # all routes, lazy-loaded pages
│   ├── layouts/
│   │   ├── MainLayout.jsx       # announcement bar, header, footer, legal strip
│   │   └── AccountLayout.jsx    # sidebar for My Account pages
│   ├── pages/
│   │   ├── HomePage.jsx
│   │   ├── ProductsPage.jsx
│   │   ├── ProductDetailPage.jsx
│   │   ├── PriceListPage.jsx
│   │   ├── CategoriesPage.jsx
│   │   ├── CategoryProductsPage.jsx
│   │   ├── BrandsPage.jsx
│   │   ├── BrandProductsPage.jsx
│   │   ├── AboutPage.jsx
│   │   ├── ContactPage.jsx
│   │   ├── SafetyPage.jsx
│   │   ├── LegalPage.jsx        # disclaimer / privacy / terms by slug
│   │   ├── auth/
│   │   │   ├── LoginPage.jsx
│   │   │   └── RegisterPage.jsx
│   │   ├── account/
│   │   │   ├── ProfilePage.jsx
│   │   │   ├── ChangePasswordPage.jsx
│   │   │   ├── FavouritesPage.jsx
│   │   │   └── EstimateListPage.jsx   # only if Q1 approved
│   │   └── NotFoundPage.jsx
│   ├── components/
│   │   ├── layout/              # Header, Navbar, MobileMenu, Footer, AnnouncementBar, LegalNotice
│   │   ├── home/                # HeroCarousel, QuickTiles, CategoryGrid, ProductRow, BrandStrip, AboutBlock, ContactBlock
│   │   ├── product/             # ProductCard, ProductGrid, ProductListItem, PriceTag, StockBadge, ImageGallery, SafetyInfo
│   │   ├── catalogue/           # SearchBar, FilterSidebar, FilterDrawer, ActiveFilterChips, SortSelect, PriceRangeFilter, ViewToggle
│   │   ├── pricelist/           # PriceTable, CategoryJumpNav, PrintHeader
│   │   └── ui/                  # Button, Input, Select, Checkbox, Badge, Modal, Drawer, Pagination,
│   │                            # Breadcrumbs, Spinner, Skeleton, EmptyState, ErrorState, SectionHeading, Toast
│   ├── context/
│   │   ├── AuthContext.jsx
│   │   └── SettingsContext.jsx  # business info loaded once
│   ├── hooks/
│   │   ├── useProducts.js
│   │   ├── useCatalogueFilters.js  # sync filters with URL query string
│   │   ├── useDebounce.js
│   │   └── useAuth.js
│   ├── services/
│   │   ├── apiClient.js         # Axios instance, base URL, JWT interceptor
│   │   ├── productService.js
│   │   ├── categoryService.js
│   │   ├── brandService.js
│   │   ├── settingsService.js
│   │   ├── authService.js
│   │   └── accountService.js
│   ├── mocks/                   # Phases 1–3 only; removed/disabled in Phase 4
│   │   ├── data/                # products.json, categories.json, brands.json, settings.json
│   │   └── mockApi.js           # filter / sort / paginate like the real API
│   ├── utils/
│   │   ├── formatCurrency.js    # ₹ en-IN formatting
│   │   ├── slugify.js
│   │   └── constants.js
│   └── config/
│       └── env.js               # VITE_API_URL, VITE_USE_MOCKS
├── .env.example
├── index.html
├── package.json
├── vite.config.js
├── tailwind.config.js (if Tailwind v3) / CSS @theme (if v4)
└── eslint.config.js
```

### 5.2 Admin application – `admin/`

```
admin/
├── src/
│   ├── main.jsx
│   ├── App.jsx
│   ├── routes/
│   │   ├── AdminRoutes.jsx
│   │   └── ProtectedRoute.jsx   # login + role check
│   ├── layouts/
│   │   └── AdminLayout.jsx      # sidebar, top bar, breadcrumb, content area
│   ├── pages/
│   │   ├── LoginPage.jsx
│   │   ├── DashboardPage.jsx
│   │   ├── products/            # ProductListPage, ProductFormPage
│   │   ├── categories/          # CategoryListPage (with modal form)
│   │   ├── brands/              # BrandListPage
│   │   ├── inventory/           # StockOverviewPage, StockInPage, StockOutPage, MovementHistoryPage
│   │   ├── sales/               # NewSalePage (billing), SalesListPage, SaleDetailPage
│   │   ├── customers/           # CustomerListPage, CustomerDetailPage
│   │   ├── expenses/            # ExpenseListPage, ExpenseCategoriesPage
│   │   ├── reports/             # SalesReportPage, ExpenseReportPage, ProfitReportPage, StockReportPage
│   │   ├── staff/               # StaffListPage
│   │   ├── settings/            # BusinessSettingsPage, BannersPage
│   │   └── NotFoundPage.jsx
│   ├── components/
│   │   ├── layout/              # Sidebar, Topbar, UserMenu
│   │   ├── dashboard/           # StatCard, SalesChart, LowStockTable, RecentSalesTable
│   │   ├── data/                # DataTable, TableToolbar, Pagination, DateRangePicker, ConfirmDialog
│   │   ├── forms/               # FormField, ImageUploader, MoneyInput, SearchSelect
│   │   ├── sales/               # ProductPicker, BillItemsTable, BillSummary
│   │   └── ui/                  # Button, Input, Badge, Modal, Drawer, Toast, Spinner, EmptyState
│   ├── context/AuthContext.jsx
│   ├── hooks/                   # useAuth, usePagedQuery, useDebounce, usePermission
│   ├── services/                # apiClient + one service file per module
│   ├── utils/                   # formatCurrency, formatDate, downloadFile, roles.js
│   └── config/env.js
└── (same config files as frontend)
```

---

## 6. Reusable React components

| Component | Used in | Purpose |
|---|---|---|
| `ProductCard` | Home rows, catalogue, category/brand pages, related products, favourites | Image, name, brand, pack size, price, stock badge, favourite toggle |
| `ProductGrid` / `ProductRow` | Catalogue / home | Responsive grid / horizontal scroll row with "View all" |
| `PriceTag` | Cards, detail, price list | Shows MRP struck through + indicative price, handles missing values |
| `StockBadge` | Cards, detail, admin tables | In stock / Low / Out |
| `SearchBar` | Header, catalogue, price list, admin | Debounced search input with clear button |
| `FilterSidebar` / `FilterDrawer` | Catalogue (desktop / mobile) | Category, brand, price, stock filters |
| `ActiveFilterChips` | Catalogue | Removable chips + Clear all |
| `SortSelect`, `Pagination` | Catalogue, admin tables | Sorting and paging controls |
| `ImageGallery` | Product detail | Thumbnails, main image, lightbox |
| `SafetyInfo`, `PriceDisclaimer`, `LegalNotice` | Detail, price list, footer | Standard legal / safety text blocks |
| `SectionHeading` | All pages | Consistent title + subtitle + action link |
| `Breadcrumbs` | Detail, category, brand pages | Navigation trail |
| `EmptyState`, `ErrorState`, `Skeleton` | Everywhere | Loading, empty and error UI |
| `Modal`, `Drawer`, `Toast`, `ConfirmDialog` | Both apps | Overlays and feedback |
| `DataTable` | Admin | Sortable, paginated table with toolbar slot |
| `FormField`, `MoneyInput`, `ImageUploader` | Admin forms | Validated inputs |
| `StatCard` | Admin dashboard, reports | KPI tile |
| `ProtectedRoute` | Both apps | Requires login / role |

---

## 7. Backend folder structure

Feature-based packages (each feature holds its own controller, service, repository, entity and DTOs). This keeps related code together as the project grows.

```
backend/
├── pom.xml
├── src/main/java/com/sivacrackers/
│   ├── SivaCrackersApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java        # filter chain, CORS, public vs protected URLs
│   │   ├── CorsProperties.java
│   │   ├── WebConfig.java             # static /uploads mapping
│   │   └── JpaAuditingConfig.java     # createdAt / updatedAt
│   ├── security/
│   │   ├── JwtService.java            # create / validate tokens
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── CustomUserDetailsService.java
│   │   └── RestAuthEntryPoint.java    # JSON 401/403 responses
│   ├── common/
│   │   ├── entity/BaseEntity.java     # id, createdAt, updatedAt
│   │   ├── dto/ApiResponse.java, PageResponse.java, ErrorResponse.java
│   │   ├── exception/                 # ResourceNotFoundException, BadRequestException,
│   │   │                              # InsufficientStockException, GlobalExceptionHandler
│   │   └── util/SlugUtil.java, InvoiceNumberGenerator.java
│   ├── auth/            AuthController, AuthService, dto/(LoginRequest, RegisterRequest, AuthResponse)
│   ├── user/            User, Role, UserRepository, AccountController, StaffController, UserService, dto/
│   ├── category/        Category, CategoryRepository, CategoryService, PublicCategoryController, AdminCategoryController, dto/
│   ├── brand/           (same pattern)
│   ├── product/         Product, ProductImage, ProductRepository, ProductSpecifications (dynamic filters),
│   │                    ProductService, PublicProductController, AdminProductController, dto/, mapper/
│   ├── favourite/       Favourite, FavouriteRepository, FavouriteService, FavouriteController
│   ├── inventory/       StockMovement, MovementType, StockMovementRepository, InventoryService, InventoryController, dto/
│   ├── supplier/        Supplier, SupplierRepository, SupplierService, SupplierController, dto/
│   ├── customer/        Customer, CustomerRepository, CustomerService, CustomerController, dto/
│   ├── sale/            Sale, SaleItem, PaymentMode, SaleRepository, SaleService, SaleController, dto/
│   ├── invoice/         InvoicePdfService, InvoiceController
│   ├── expense/         Expense, ExpenseCategory, repositories, ExpenseService, ExpenseController, dto/
│   ├── report/          ReportService, ReportController, dto/ (DailySummary, MonthlySummary, TopProduct, ...)
│   ├── dashboard/       DashboardService, DashboardController, dto/
│   ├── settings/        BusinessSetting, Banner, repositories, SettingsService, PublicSettingsController, AdminSettingsController
│   └── storage/         FileStorageService (interface), LocalFileStorageService
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── db/migration/
│       ├── V1__init_schema.sql
│       └── V2__demo_data.sql
├── src/test/java/com/sivacrackers/   # service unit tests, controller tests, repository tests
└── uploads/                          # git-ignored
```

**Main dependencies:** spring-boot-starter-web, -data-jpa, -security, -validation, mysql-connector-j, jjwt, flyway-core + flyway-mysql, openpdf, lombok (optional), spring-boot-starter-test.

**Versions:** Java 25 is installed on your machine. Spring Boot 4.x supports Java 25. I will confirm the exact latest stable Spring Boot version at the start of Phase 4.

---

## 8. Database schema

Database: `siva_crackers`, MySQL 8, InnoDB, utf8mb4. All tables have `created_at`, `updated_at`.

### 8.1 Entity relationships

```
categories 1───* products *───1 brands
products   1───* product_images
products   1───* stock_movements *───0..1 suppliers
products   1───* sale_items *───1 sales *───0..1 customers
sales      *───1 users (created_by staff)
users      1───* favourites *───1 products
expenses   *───1 expense_categories
expenses   *───1 users (recorded_by)
business_settings (single row), banners
```

### 8.2 Tables

**users** – staff and website customers
| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK AI | |
| full_name | VARCHAR(100) NOT NULL | |
| email | VARCHAR(150) UNIQUE | |
| phone | VARCHAR(15) UNIQUE | |
| password_hash | VARCHAR(255) NOT NULL | BCrypt |
| role | ENUM('OWNER','MANAGER','CASHIER','CUSTOMER') | |
| active | BOOLEAN DEFAULT TRUE | |
| last_login_at | DATETIME NULL | |

**categories**
| id | name VARCHAR(80) UNIQUE | slug VARCHAR(100) UNIQUE | description TEXT | image_url | display_order INT | active BOOLEAN |

**brands**
| id | name VARCHAR(80) UNIQUE | slug UNIQUE | description TEXT | logo_url | display_order INT | active BOOLEAN |

**products**
| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| name | VARCHAR(150) NOT NULL | |
| slug | VARCHAR(170) UNIQUE | for URLs |
| sku | VARCHAR(50) UNIQUE | product code |
| category_id | FK → categories | NOT NULL |
| brand_id | FK → brands | NOT NULL |
| description | TEXT | |
| pack_size | VARCHAR(50) | e.g. "10 Pcs", "1 Box" |
| unit | ENUM('BOX','PACKET','PIECE','BUNDLE') | |
| mrp | DECIMAL(10,2) | |
| indicative_price | DECIMAL(10,2) | shown on website |
| purchase_price | DECIMAL(10,2) | admin only, for profit reports |
| stock_quantity | INT DEFAULT 0 | updated with movements |
| reorder_level | INT DEFAULT 10 | low-stock alert |
| safety_notes | TEXT | product-specific safety text |
| featured | BOOLEAN | homepage |
| visible_on_website | BOOLEAN | hide without deleting |
| active | BOOLEAN | soft delete / archive |

Indexes: (category_id), (brand_id), (name), (indicative_price), (featured, visible_on_website).

**product_images**
| id | product_id FK | image_url | alt_text | display_order | is_primary |

**suppliers**
| id | name | phone | address | gst_number (nullable) | notes |

**stock_movements** – inventory ledger
| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| product_id | FK | |
| movement_type | ENUM('STOCK_IN','SALE','RETURN','DAMAGE','ADJUSTMENT_IN','ADJUSTMENT_OUT') | |
| quantity | INT | always positive; type gives direction |
| unit_cost | DECIMAL(10,2) NULL | for stock in |
| supplier_id | FK NULL | |
| sale_id | FK NULL | links sale movements |
| reference_no | VARCHAR(50) | supplier bill no. etc. |
| balance_after | INT | stock after this movement |
| notes | VARCHAR(255) | |
| created_by | FK → users | |

**customers** – in-store customer records
| id | name | phone (indexed) | email NULL | address NULL | notes |

**sales**
| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| invoice_number | VARCHAR(30) UNIQUE | e.g. SC-2026-000123 |
| sale_date | DATETIME | |
| customer_id | FK NULL | walk-in allowed |
| subtotal | DECIMAL(12,2) | |
| discount_amount | DECIMAL(12,2) | |
| tax_amount | DECIMAL(12,2) | from configurable tax % |
| total_amount | DECIMAL(12,2) | |
| payment_mode | ENUM('CASH','UPI','CARD','OTHER') | recorded only, no gateway |
| status | ENUM('COMPLETED','CANCELLED') | cancel puts stock back |
| created_by | FK → users | |
| notes | VARCHAR(255) | |

**sale_items**
| id | sale_id FK | product_id FK | product_name_snapshot | unit_price | quantity | line_total |

(Name and price are copied at sale time so old invoices stay correct after price changes.)

**expense_categories**
| id | name UNIQUE | active |

**expenses**
| id | expense_category_id FK | amount DECIMAL(12,2) | expense_date DATE | payment_mode | description | recorded_by FK → users |

**favourites**
| id | user_id FK | product_id FK | UNIQUE(user_id, product_id) |

**business_settings** – single row, edited in admin
| shop_name | tagline | address_line | city | state | pincode | phone | alternate_phone | email | business_hours | map_embed_url | licence_details | gst_number | invoice_prefix | tax_percent | announcement_text | announcement_active | about_text | price_disclaimer | safety_text | logo_url |

**banners**
| id | title | subtitle | image_url | link_url | display_order | active |

### 8.3 Demo data
Realistic but clearly fictional: about 15 categories (Sparklers, Flower Pots, Ground Chakkars, Rockets, Single Sound, Garlands / Walas, Aerial Shots, Fancy Items, Kids Items, Gift Boxes, etc.), about 8 **fictional** brand names, about 80–100 products with original placeholder images, a few demo sales, expenses and stock movements. Business details stay as placeholders until you provide them.

---

## 9. API development plan

All responses are JSON. Lists are paginated with `page`, `size`, `sort` and return `{ content, page, size, totalElements, totalPages }`. Errors return `{ timestamp, status, error, message, fieldErrors }`.

### 9.1 Public (no login)
| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/public/products` | Catalogue. Params: `search`, `categoryIds`, `brandIds`, `minPrice`, `maxPrice`, `inStock`, `sort` (name,asc / price,desc / newest), `page`, `size` |
| GET | `/api/public/products/{slug}` | Product detail with images |
| GET | `/api/public/products/{slug}/related` | Same-category products |
| GET | `/api/public/products/featured` | Homepage featured |
| GET | `/api/public/products/new-arrivals` | Homepage new arrivals |
| GET | `/api/public/price-list` | All visible products grouped by category (unpaginated, for print) |
| GET | `/api/public/categories` | Active categories with product counts |
| GET | `/api/public/categories/{slug}` | Category info |
| GET | `/api/public/brands` | Active brands with product counts |
| GET | `/api/public/brands/{slug}` | Brand info |
| GET | `/api/public/settings` | Business info, announcement, disclaimers |
| GET | `/api/public/banners` | Hero slides |
| GET | `/api/public/price-range` | Min / max price for the slider |

### 9.2 Auth
| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Customer signup |
| POST | `/api/auth/login` | Customer login → JWT |
| POST | `/api/auth/admin/login` | Staff login → JWT (rejects CUSTOMER) |
| GET | `/api/auth/me` | Current user |

### 9.3 Customer account (role CUSTOMER)
| Method | Endpoint | Purpose |
|---|---|---|
| GET / PUT | `/api/account/profile` | View / update profile |
| PUT | `/api/account/password` | Change password (needs current password) |
| GET | `/api/account/favourites` | Saved products |
| POST / DELETE | `/api/account/favourites/{productId}` | Add / remove |

### 9.4 Admin (staff roles)
| Area | Endpoints | Roles |
|---|---|---|
| Dashboard | `GET /api/admin/dashboard/summary` | All staff (cashier sees limited data) |
| Products | `GET/POST /api/admin/products`, `GET/PUT/DELETE /api/admin/products/{id}`, `POST /api/admin/products/{id}/images`, `DELETE /api/admin/products/{id}/images/{imageId}` | OWNER, MANAGER (CASHIER read-only) |
| Categories | `GET/POST /api/admin/categories`, `PUT/DELETE /api/admin/categories/{id}` | OWNER, MANAGER |
| Brands | `GET/POST /api/admin/brands`, `PUT/DELETE /api/admin/brands/{id}` | OWNER, MANAGER |
| Inventory | `GET /api/admin/inventory` (stock list), `GET /api/admin/inventory/low-stock`, `POST /api/admin/inventory/stock-in`, `POST /api/admin/inventory/stock-out`, `GET /api/admin/inventory/movements?productId=&type=&from=&to=` | OWNER, MANAGER (CASHIER view) |
| Suppliers | `GET/POST/PUT/DELETE /api/admin/suppliers` | OWNER, MANAGER |
| Sales | `POST /api/admin/sales`, `GET /api/admin/sales?from=&to=&customerId=`, `GET /api/admin/sales/{id}`, `POST /api/admin/sales/{id}/cancel` | All staff (cancel: OWNER, MANAGER) |
| Invoices | `GET /api/admin/sales/{id}/invoice.pdf` | All staff |
| Customers | `GET/POST /api/admin/customers`, `GET/PUT /api/admin/customers/{id}`, `GET /api/admin/customers/{id}/sales`, `GET /api/admin/website-users` | All staff (edit: OWNER, MANAGER) |
| Expenses | `GET/POST /api/admin/expenses`, `PUT/DELETE /api/admin/expenses/{id}`, `GET/POST/PUT /api/admin/expense-categories` | OWNER, MANAGER |
| Reports | `GET /api/admin/reports/sales?period=daily|monthly|yearly&from=&to=`, `/reports/expenses`, `/reports/profit`, `/reports/top-products`, `/reports/category-sales`, `/reports/stock-valuation`, each with `?format=pdf|csv` export | OWNER, MANAGER |
| Staff | `GET/POST /api/admin/staff`, `PUT /api/admin/staff/{id}`, `PATCH /api/admin/staff/{id}/status`, `POST /api/admin/staff/{id}/reset-password` | OWNER |
| Settings | `GET/PUT /api/admin/settings`, `GET/POST/PUT/DELETE /api/admin/banners` | OWNER |

### 9.5 Business rules enforced in services
- A sale cannot reduce stock below zero (`InsufficientStockException`).
- Sale creation, stock deduction and movement rows happen in one `@Transactional` method.
- Cancelling a sale writes `RETURN` movements and restores stock.
- Categories / brands with products cannot be hard-deleted (deactivate instead).
- Invoice numbers are sequential per year.
- The last active OWNER account cannot be deactivated (prevents lock-out).

### 9.6 Testing tools
- Postman collection (`postman/SivaCrackers.postman_collection.json`) with an environment file, kept up to date per phase.
- JUnit + Mockito for services, `@WebMvcTest` for controllers, `@DataJpaTest` for repositories.

---

## 10. Design system (original for SIVA Crackers)

| Token | Value | Use |
|---|---|---|
| Navy 900 | `#0B1636` | Header, footer, hero background |
| Navy 700 | `#1B2A5C` | Cards on dark, admin sidebar |
| Gold 400 | `#F5B921` | Primary accents, prices, buttons |
| Orange 500 | `#F26A1B` | Calls to action, badges, hover states |
| White / Cream | `#FFFFFF` / `#FFF8EC` | Page and card backgrounds |
| Slate | `#475569` | Body text |

- **Typography:** "Poppins" or "Outfit" for headings, "Inter" for body text (Google Fonts). Tamil text support can be added later if you want bilingual labels.
- **Style:** dark navy hero with subtle gold sparkle / firework line-art (CSS and SVG, our own), white product cards with soft shadows, rounded corners, gold price text, orange buttons.
- **Motion:** gentle fade/slide-in on scroll, card lift on hover, carousel transitions. Respects `prefers-reduced-motion`.
- **Mobile-first:** 2-column product grid on phones, filter drawer from the bottom, sticky search, large tap targets. Breakpoints at Tailwind defaults (sm 640, md 768, lg 1024, xl 1280).
- **Accessibility:** colour contrast AA, keyboard-navigable menus and modals, alt text on images, form labels.
- **Admin app:** same colours, calmer layout: navy sidebar, white content, dense tables.

---

## 11. Legal and safety approach

- **Persistent notice** on every public page (strip near the footer and on the Price List): the website shows product information and indicative prices only; no online sale, booking, payment or delivery of firecrackers; purchases only in person at the shop, subject to applicable laws.
- **Price disclaimer** on cards (short), Price List and product pages (full text, editable in admin).
- **Safety information** on every product page and a dedicated Safety Guidelines page (general safe-use guidance: adult supervision, open space, water bucket, distance, no relighting duds, follow local timing rules, store away from heat).
- **No cart checkout, payment, booking or delivery code** anywhere in the public app or public API.
- **Customer accounts** limited to profile, password and favourites.
- **Exact legal wording** should be reviewed by you or a legal adviser. I will write a clear draft but it is not legal advice.
- **Licence / registration details** shown only if you provide them.
- **Privacy:** passwords hashed, minimal personal data collected, privacy policy page.

---

## 12. Development roadmap

Each phase ends with testing, a short summary of what was built and how to run it, and a pause for your approval.

| Phase | Scope | Deliverables | Testing |
|---|---|---|---|
| **0** | Planning (this document) | This plan | Your review |
| **1** | React setup + homepage UI | `frontend/` Vite + Tailwind + Router + Lucide + Axios; design tokens; MainLayout (announcement bar, header, mobile menu, footer, legal strip); full homepage sections; mock data layer and demo JSON | Runs locally; lint passes; mobile / tablet / desktop checks in the browser; production build succeeds |
| **2** | Product catalogue + filtering | All Products page, search, category / brand / price filters, sort, chips, clear, pagination, URL-synced filters, Categories & Brands pages, category / brand product pages | Manual test matrix of filter combinations; empty states; mobile drawer |
| **3** | Product details + business pages | Product detail with gallery, safety info, related products; Price List with print layout; About, Contact, Safety, Legal, 404; Login / Register / Account UI (against mocks) | Print preview check; navigation and broken-link check; form validation |
| **4** | Spring Boot backend + MySQL | Project setup, Flyway schema + demo data, entities, repositories, services, DTOs, public catalogue APIs, customer auth (JWT), global error handling; frontend switched from mocks to Axios | JUnit / Mockito tests; Postman collection; full public site working against the real API |
| **5** | Admin authentication + dashboard | `admin/` app, staff login, protected routes, role guards, layout, dashboard; product / category / brand management with image upload; staff management; business settings | Role-access tests (each role tries forbidden actions); CRUD tests; API tests |
| **6** | Inventory + business management | Stock in / out, movement history, low stock; suppliers; billing screen; PDF invoices; customers; expenses; daily / monthly / yearly reports with PDF / CSV export | Transaction tests (stock never negative, cancel restores stock); report totals checked against seeded data; invoice PDF check |
| **7** | Testing + improvements | Fill test gaps, input validation review, security review (CORS, JWT expiry, upload limits, SQL injection, XSS), performance (indexes, lazy loading, image sizes), accessibility and SEO (meta tags, sitemap), UI polish | Full regression checklist across both apps; Lighthouse audit |
| **8** | Deployment + GitHub | Git repository with clean history, README with setup steps, `.env.example` files, production profiles, build scripts; deployment guide (hosting options for React apps, Spring Boot and MySQL) | Fresh-clone setup test; deployed smoke test |

### Final project layout

```
crackers_resume/
├── docs/            # this plan, API notes, setup guides
├── frontend/        # public website (React + Vite)
├── admin/           # admin application (React + Vite)
├── backend/         # Spring Boot API
├── postman/         # Postman collection + environment
└── README.md
```

### Your environment (checked)
Node 24, Java 25, Maven, MySQL client and Git are all installed. Nothing else needed to start Phase 1.

---

## 13. Open questions for you

Defaults are in bold; I will use them if you approve the plan without changes.

1. **My Estimate List:** build a browser-only, printable estimate list (no submission, no ordering) in place of the reference cart? **Default: yes, as an optional Phase 3 feature, clearly labelled "for your reference only".**
2. **WhatsApp / contact form:** show a WhatsApp chat link or a general contact form? **Default: no WhatsApp link; phone, address and map only. A general-questions contact form can be added if you confirm.**
3. **Payment gateway demo:** **Default: not built in this project.**
4. **Business details:** please share when ready: phone number(s), full address, email, opening hours, Google Maps link, licence details (if you want them shown), GST number (optional), logo preference. Until then I will use clearly marked placeholders.
5. **Two separate admin and public apps** (`admin/` and `frontend/`)? **Default: yes.**
6. **Small additions inside the agreed stack:** Flyway (DB migrations), OpenPDF (invoices), jjwt (JWT), and a small chart library such as Recharts in the admin dashboard. These do not change the stack but are new libraries. **Default: use them.** Say no to Recharts and I will draw simple charts with plain SVG / Tailwind.
7. **Customer login identifier:** **Default: email or phone + password.**
8. **Forgot password by email:** needs an email (SMTP) account. **Default: deferred; staff can reset passwords from admin, customers can change password when logged in.**
9. **Product images:** do you have real product photos? **Default: original placeholder illustrations until you supply photos.**
10. **Language:** **Default: English only.** Tamil labels can be added later.
