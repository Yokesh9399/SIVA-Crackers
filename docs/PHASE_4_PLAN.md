# Phase 4 – Spring Boot Backend and MySQL Integration (Plan)

**Status:** Approved 23 Nov 2026 with amendments (see §0). Implementation in progress, one step at a time.

---

## 0. Approved decisions and amendments (these override the proposal text below)

| # | Decision |
|---|---|
| D1 | Spring Boot **4.1.3**, Java **21** target |
| D2 | **Amended:** authentication uses an **HttpOnly, Secure, SameSite cookie** holding the JWT – never localStorage, and no JavaScript access to the token. CSRF protection is on (double-submit cookie pattern, see §0.1). Axios uses `withCredentials`; CORS and the Vite dev proxy are configured for credentials |
| D3 | Customer favourites deferred |
| D4 | Demo database seed is optional and **disabled by default** |
| D5 | Dedicated MySQL account `siva_app` |
| – | Existing frontend API response contracts and all Phase 1–3 functionality are preserved |

**Additional security requirements**
- Public registration creates **CUSTOMER** accounts only. OWNER, MANAGER and CASHIER accounts can only be created through authorised processes (the one-time owner bootstrap now; owner-only staff management later).
- Secure **one-time OWNER bootstrap** from environment variables (only when no OWNER exists; password never logged).
- **Login rate limiting** and safe failed-login handling (same message for unknown user and wrong password, temporary lockout, `429` with `Retry-After`).
- Passwords, password hashes, JWT secrets, tokens and cookies are **never** written to logs or returned in API responses.

### 0.1 Cookie authentication design (replaces §6.3 token-storage text and §8 item 3)
| Item | Setting |
|---|---|
| Auth cookie | `SIVA_SESSION` = signed JWT. `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/api`, `Max-Age` = token lifetime. Cleared on logout (`Max-Age=0`) |
| CSRF | Spring Security CSRF enabled for all state-changing requests. Token in readable cookie `XSRF-TOKEN`; the frontend sends it back in header `X-XSRF-TOKEN` (Axios does this automatically for same-origin requests). `GET /api/auth/csrf` issues the token. The CSRF cookie is not a credential, so being readable is safe (double-submit pattern) |
| Same origin in development | Vite proxies `/api` to `http://localhost:8080`, so the browser sees one origin (`localhost:5173`): `SameSite=Strict` works and no CORS is needed. Browsers accept `Secure` cookies on `http://localhost`. `APP_COOKIE_SECURE` exists only for unusual local setups and must stay `true` in production |
| CORS | Only for an optional separately hosted frontend: explicit origins from `CORS_ALLOWED_ORIGINS` (no wildcards), `allowCredentials=true`, header `X-XSRF-TOKEN` allowed. Empty by default = no cross-origin access |
| Axios | `withCredentials: true`, `xsrfCookieName: 'XSRF-TOKEN'`, `xsrfHeaderName: 'X-XSRF-TOKEN'`; the old `localStorage['siva_token']` code is removed |
| Login state in the UI | Read from `GET /api/auth/me` (returns the user, never the token) |

---

## 1. What I found in the repository

Inspected: `README.md`, `docs/PHASE_0_PLAN.md`, Phase 1–3 notes, and the actual frontend code (commit `6507474`).

### 1.1 The frontend already defines the API contract
All data goes through 4 service files. Each one checks `USE_MOCKS` and otherwise calls Axios. The backend must return exactly the shapes the mock API returns today, so **no React component needs to change**.

| # | Service call | Endpoint the frontend calls | Response shape |
|---|---|---|---|
| 1 | `productService.getProducts(params)` | `GET /api/public/products?search&categories&brands&minPrice&maxPrice&inStock&sort&page&size` | `{ content[], page, size, totalElements, totalPages }` |
| 2 | `getFeatured(limit)` | `GET /api/public/products/featured?limit` | `Product[]` |
| 3 | `getNewArrivals(limit)` | `GET /api/public/products/new-arrivals?limit` | `Product[]` |
| 4 | `getBySlug(slug)` | `GET /api/public/products/{slug}` | `ProductDetail` (404 if unknown) |
| 5 | `getRelated(slug, limit)` | `GET /api/public/products/{slug}/related?limit` | `Product[]` |
| 6 | `getBySlugs(slugs)` | `GET /api/public/products/by-slugs?slugs=a,b` | `Product[]` (Estimate List) |
| 7 | `getPriceList()` | `GET /api/public/price-list` | `[{ category, products[] }]` |
| 8 | `getPriceRange()` | `GET /api/public/price-range` | `{ min, max }` |
| 9 | `categoryService.getAll()` | `GET /api/public/categories` | `Category[]` with `productCount` |
| 10 | `brandService.getAll()` | `GET /api/public/brands` | `Brand[]` with `productCount`, `isDemo` |
| 11 | `settingsService.get()` | `GET /api/public/settings` | business settings object (nested `address`, `announcement`, `highlights[]`, `safetyTips[]`, notices) |
| 12 | `settingsService.getBanners()` | `GET /api/public/banners` | `Banner[]` |

`Product` = `id, name, slug, sku, description, packSize, unit, mrp, price, stockStatus (IN_STOCK|LOW_STOCK|OUT_OF_STOCK), featured, createdAt, images[], isDemo, category{id,name,slug}, brand{id,name,slug}`.
`ProductDetail` adds `category.description` and `safetyNotes[]`.

### 1.2 Other facts that shape the design
- Filters use **slugs**, multi-value as comma lists (Phase 2 decision). Sort values: `default, name,asc, name,desc, price,asc, price,desc, newest`. Page is **0-based**; sizes 12/24/48.
- `apiClient.js` already sends `Authorization: Bearer <token>` from `localStorage['siva_token']` and keeps the HTTP status on errors (`error.status === 404` shows the Not Found page).
- `/login` and `/register` are placeholders that say "coming in Phase 4" (moved from Phase 3).
- The Estimate List is browser-only and must **stay** browser-only – no backend table or endpoint for it.
- The "Demonstration data" notice is currently controlled by `VITE_USE_MOCKS`; with a real backend it must instead come from the data.
- Business details (phone, WhatsApp, email, hours, street address, licence) are `null` and must stay `null` – never invented.
- Local machine: **Java 25, Maven 3.9.11, MySQL 8.0.42 running on port 3306**. Docker CLI is installed but its engine is not running, so tests will not depend on Docker.

### 1.3 Differences from the Phase 0 plan (to approve)
| Phase 0 plan | Proposal now | Why |
|---|---|---|
| Filters by `categoryIds`/`brandIds` | `categories`/`brands` **slugs** | Matches the Phase 2 frontend |
| All tables (sales, stock, expenses…) | Only the tables Phase 4 needs; later phases add theirs with new migrations | Smaller, testable phase; Flyway versions each step |
| `business_settings` flat row | Settings row + child tables for highlights, safety tips and banners | Matches the frontend's richer settings object |
| Customer favourites in Phase 4 | **Not included** (see decision D3) | Not requested for Phase 4; keeps scope focused |

---

## 2. Scope of Phase 4

**In scope**
1. New `backend/` Spring Boot project (Maven).
2. MySQL schema via Flyway migrations; demo data as a **separate, optional** migration set, every row flagged `is_demo = 1`.
3. The 12 public read endpoints above, matching the mock responses exactly.
4. Spring Security + JWT: register/login for customers, login for staff, `/me`, profile update, change password. Roles OWNER, MANAGER, CASHIER, CUSTOMER enforced on the server.
5. A small admin endpoint group to prove role-based access on real business data: **read/update business settings** (OWNER updates; OWNER/MANAGER read). Full admin features stay in Phase 5.
6. Validation and one consistent JSON error format.
7. Frontend integration: switch services to the real API via environment config; build the **Login, Register and My Account** pages in the existing design (they were deferred from Phase 3); show "My Account" in the header when logged in.
8. Backend tests, API documentation (`docs/API.md` + OpenAPI/Swagger UI in dev), setup guide.

**Out of scope (unchanged rules)**
- No checkout, payment, online ordering, booking or reservation – no tables, endpoints or UI for them.
- No admin app, product CRUD, inventory, sales, invoices or reports (Phases 5–6).
- No real business details invented; no frontend redesign.

---

## 3. Backend architecture

```
React (Vite :5173) ── Axios ──►  /api/**  ──►  Spring Boot (:8080)
                                              │
        JwtAuthenticationFilter ── SecurityFilterChain (stateless, role rules, CORS)
                                              │
        Controller  (HTTP only: params, @Valid, status codes)
             │  DTOs (Java records)
        Service     (business rules, transactions, mapping)
             │  Entities
        Repository  (Spring Data JPA + Specifications for catalogue filters)
                                              │
                                    MySQL 8  (schema owned by Flyway)
```

### 3.1 Package structure (feature-based, as in Phase 0)

```
backend/
├── pom.xml
├── .env.example                      # names of required env vars, no values
├── README.md                         # setup + run + test instructions
└── src/
    ├── main/java/com/sivacrackers/
    │   ├── SivaCrackersApplication.java
    │   ├── config/        SecurityConfig, CorsProperties, JwtProperties, AppProperties, OpenApiConfig
    │   ├── security/      JwtService, JwtAuthenticationFilter, AppUserDetailsService,
    │   │                  RestAuthenticationEntryPoint, RestAccessDeniedHandler, LoginAttemptService
    │   ├── common/
    │   │   ├── entity/    BaseEntity (id, createdAt, updatedAt via JPA auditing)
    │   │   ├── dto/       PageResponse, ApiError, FieldErrorDto
    │   │   ├── exception/ NotFoundException, ConflictException, BadRequestException, GlobalExceptionHandler
    │   │   └── util/      SlugUtil, CsvParam (comma lists → List)
    │   ├── catalog/
    │   │   ├── category/  Category, CategorySafetyNote, CategoryRepository, CategoryService,
    │   │   │              PublicCategoryController, dto/
    │   │   ├── brand/     Brand, BrandRepository, BrandService, PublicBrandController, dto/
    │   │   └── product/   Product, ProductImage, StockStatus, ProductUnit, ProductRepository,
    │   │                  ProductSpecifications, ProductSort, ProductService, ProductMapper,
    │   │                  PublicProductController, PriceListController, dto/
    │   ├── settings/      BusinessSettings, BusinessHighlight, SafetyTip, Banner, repositories,
    │   │                  SettingsService, PublicSettingsController, AdminSettingsController, dto/
    │   └── user/          User, Role, UserRepository, AuthService, AccountService,
    │                      AuthController, AccountController, OwnerBootstrap, dto/
    └── main/resources/
        ├── application.yml           # all secrets as ${ENV_VAR}; no defaults for secrets
        ├── application-dev.yml       # SQL logging, Swagger UI on
        ├── application-test.yml
        ├── db/migration/             # schema – always applied
        │   └── V1__core_schema.sql
        └── db/demo/                  # demo data – applied only when demo seeding is on
            └── V100__demo_catalogue.sql
```

### 3.2 Key design decisions
1. **Response compatibility first.** DTOs are Java records shaped exactly like today's mock JSON (field names, nesting, `stockStatus`, 0-based paging). The mock API stays in the repo for offline UI work (`VITE_USE_MOCKS=true`).
2. **Catalogue filtering** with JPA `Specification`s (search across product/brand/category name, every word must match; slug lists; price bounds; in-stock), whitelisted sorts, `size` limited to 12/24/48 (max 48).
3. **Stock status** computed from `stock_quantity` and `reorder_level` (`0` → OUT_OF_STOCK, `≤ reorder_level` → LOW_STOCK). Demo `reorder_level` = 10, which reproduces today's labels exactly.
4. **Money** as `DECIMAL(10,2)` / `BigDecimal`, serialised as numbers (the UI already formats ₹).
5. **Demo data is optional and labelled.** Schema migrations live in `db/migration`; demo rows live in `db/demo` and are applied only when `APP_SEED_DEMO=true`. Every demo product and brand has `is_demo = 1`. The settings API returns `demoMode = true` while any demo product exists, which drives the existing "Demonstration data" notice and DEMO tags. Production can start with an empty, real catalogue.
6. **No hard-coded secrets or accounts.** DB credentials, `JWT_SECRET`, CORS origins and the first OWNER account come from environment variables. The app **refuses to start** if `JWT_SECRET` is missing or shorter than 32 bytes. The first OWNER is created once at startup from `BOOTSTRAP_OWNER_EMAIL` / `BOOTSTRAP_OWNER_PASSWORD` only if no OWNER exists; nothing is seeded with a known password.
7. **Business information stays honest.** The settings seed copies today's `settings.json`: shop name, tagline, Eachanari/Coimbatore/Tamil Nadu and the existing notices; every unknown detail is `NULL`.

---

## 4. Dependencies

| Purpose | Dependency | Version |
|---|---|---|
| Framework | `spring-boot-starter-parent` | **4.1.3** (latest patch of 4.1; 4.2.0 was just released – see D1) |
| Language | Java | 21 bytecode target (LTS), runs on your Java 25 – see D1 |
| REST | `spring-boot-starter-web` | (Boot-managed) |
| Persistence | `spring-boot-starter-data-jpa` | (Boot-managed, Hibernate) |
| Validation | `spring-boot-starter-validation` | (Boot-managed) |
| Security | `spring-boot-starter-security` | (Boot-managed) |
| Migrations | `spring-boot-starter-flyway` + `flyway-mysql` | (Boot-managed) |
| Database driver | `mysql-connector-j` | (Boot-managed) |
| JWT | `jjwt-api`, `jjwt-impl`, `jjwt-jackson` | 0.13.0 |
| API docs | `springdoc-openapi-starter-webmvc-ui` | 3.x (Swagger UI only in `dev` profile) |
| Tests | `spring-boot-starter-test` (JUnit 5, Mockito, AssertJ, MockMvc), `spring-security-test` | (Boot-managed) |

Not used: Lombok (records + plain classes keep the code explicit), Testcontainers (Docker engine not running).

---

## 5. Database schema (Phase 4 tables)

Database `siva_crackers`, MySQL 8, InnoDB, `utf8mb4_0900_ai_ci`. Every table has `created_at`, `updated_at`.

```
categories 1──* category_safety_notes
categories 1──* products *──1 brands
products   1──* product_images
business_settings (single row) 1──* business_highlights, safety_tips
banners
users
```

| Table | Columns (key ones) | Constraints / indexes |
|---|---|---|
| **categories** | id, name, slug, description, image_url, display_order, active | UNIQUE(name), UNIQUE(slug) |
| **category_safety_notes** | id, category_id, position, note (VARCHAR 300) | FK → categories ON DELETE CASCADE; UNIQUE(category_id, position) |
| **brands** | id, name, slug, description, logo_url, display_order, active, is_demo | UNIQUE(name), UNIQUE(slug) |
| **products** | id, name, slug, sku, category_id, brand_id, description, pack_size, unit ENUM(BOX,PACKET,PIECE,BUNDLE), mrp DECIMAL(10,2), price DECIMAL(10,2), stock_quantity INT, reorder_level INT, featured, visible_on_website, active, is_demo, created_at | UNIQUE(slug), UNIQUE(sku); FKs RESTRICT; indexes on (category_id), (brand_id), (price), (name), (featured, visible_on_website, active); CHECK (price ≥ 0, mrp ≥ 0, stock_quantity ≥ 0) |
| **product_images** | id, product_id, url, alt_text, display_order, is_primary | FK → products CASCADE |
| **business_settings** | id (always 1), shop_name, tagline, address_line1, area, city, state, pincode, phone, alternate_phone, whatsapp_number, whatsapp_enquiry_enabled, email, business_hours, map_embed_url, map_is_approximate, licence_details, announcement_text, announcement_active, about_text, price_disclaimer, legal_notice, whatsapp_notice | Unknown values `NULL` |
| **business_highlights** | id, settings_id, position, title, text | FK → business_settings |
| **safety_tips** | id, settings_id, position, text | FK → business_settings |
| **banners** | id, eyebrow, title, subtitle, cta_label, cta_link, theme, display_order, active | – |
| **users** | id, full_name, email, phone, password_hash, role ENUM(OWNER,MANAGER,CASHIER,CUSTOMER), active, last_login_at | UNIQUE(email), UNIQUE(phone) (nullable) |

There are deliberately **no** order, cart, booking, reservation or payment tables.

Later phases add tables with new migrations (e.g. Phase 6: `stock_movements`, `sales`, `sale_items`, `customers`, `expenses`).

---

## 6. API design

### 6.1 Public (no login) – the 12 endpoints in §1.1
Validation: `page ≥ 0`; `size ∈ {12, 24, 48}`; `sort` whitelisted; `minPrice`, `maxPrice ≥ 0` and `minPrice ≤ maxPrice`; `search` ≤ 100 characters; at most 48 slugs per list; `limit` 1–12. Invalid input → 400 with field errors. Unknown slug → 404.

### 6.2 Authentication and accounts
| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | public | Customer signup (full name, email, optional phone, password) → role CUSTOMER + token |
| POST | `/api/auth/login` | public | Customer login (email or phone + password) → token |
| POST | `/api/auth/staff/login` | public | Staff login; rejects CUSTOMER accounts |
| GET | `/api/auth/me` | any logged-in user | Current user |
| GET / PUT | `/api/account/profile` | CUSTOMER | View / update name and phone |
| PUT | `/api/account/password` | any logged-in user | Change password (current password required) |
| GET | `/api/admin/settings` | OWNER, MANAGER | Read full business settings |
| PUT | `/api/admin/settings` | OWNER | Update business settings (validated) |

Role rules live in one place (`SecurityConfig`) plus `@PreAuthorize` on admin methods. CASHIER has no admin endpoint yet (sales arrive in Phase 6), which the tests will confirm (403).

### 6.3 Security details
- Passwords hashed with **BCrypt (strength 12)**. Rules: 8–72 characters, at least one letter and one number. Raw passwords are never logged or returned.
- **JWT** (HS256) signed with `JWT_SECRET`; claims: user id, role; expiry 60 minutes (configurable). Inactive users are rejected even with a valid token.
- **Stateless** sessions; CSRF disabled for the token API; CORS allows only the origins in `CORS_ALLOWED_ORIGINS`.
- **Login protection:** after 5 failed attempts for an account, login is blocked for 15 minutes (in-memory). Same message for "wrong email" and "wrong password".
- Security headers via Spring Security defaults. Swagger UI is only enabled in the `dev` profile.

### 6.4 Error format (every error)
```json
{
  "timestamp": "2026-11-23T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/public/products",
  "fieldErrors": [{ "field": "size", "message": "must be one of 12, 24, 48" }]
}
```
Used for validation (400), bad credentials (401), forbidden (403), not found (404), duplicate email/phone (409), locked login (429) and unexpected errors (500, generic message, details only in server logs). `apiClient.js` already reads `message` and `status`.

---

## 7. Configuration (environment variables)

| Variable | Example (not a real value) | Required |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/siva_crackers` | yes |
| `DB_USERNAME` | `siva_app` | yes |
| `DB_PASSWORD` | *(your password)* | yes |
| `JWT_SECRET` | *(random 32+ byte string; generation command in README)* | yes |
| `JWT_EXPIRY_MINUTES` | `60` | no |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | yes |
| `APP_SEED_DEMO` | `true` for development, `false` for production | no (default false) |
| `BOOTSTRAP_OWNER_EMAIL` / `BOOTSTRAP_OWNER_PASSWORD` | *(you choose)* | only for first start |
| `TEST_DB_URL` / `TEST_DB_USERNAME` / `TEST_DB_PASSWORD` | `jdbc:mysql://localhost:3306/siva_crackers_test` | integration tests |

I recommend a dedicated MySQL user (`siva_app`) with rights only on `siva_crackers` and `siva_crackers_test`, not `root`. The README will include the SQL to create it. **You set the passwords yourself**; I will never put them in files or commit them. `backend/.env` (if used locally) is git-ignored; `backend/.env.example` lists names only.

Frontend: `VITE_USE_MOCKS=false`, `VITE_API_URL=/api`, and a Vite dev proxy `/api → http://localhost:8080`. The same build then works behind any host in Phase 8.

---

## 8. Frontend integration (no redesign)
1. `.env` → `VITE_USE_MOCKS=false`; Vite proxy for `/api`.
2. `IS_DEMO_DATA` reads `settings.demoMode` from the API (falls back to the mock flag in mock mode).
3. New `AuthContext` + `authService` (register, login, logout, me, profile, password); token in `localStorage['siva_token']`, cleared on logout or any 401.
4. Replace the Login/Register placeholders with real pages; add **My Account** (profile + change password) using existing components (`PageHeader`, buttons, form styles). Header shows "My Account" instead of "Login / Signup" when logged in.
5. Keep the mock API for offline UI work; everything else unchanged.

---

## 9. Testing plan

| Level | What | How |
|---|---|---|
| Unit | `ProductService` (filters, sorting, stock status, related, price list), `AuthService` (register duplicates, login lockout, inactive user), `JwtService` (sign, expiry, tamper), validators | JUnit 5 + Mockito, no database |
| Web slice | Every controller: status codes, JSON shape, validation errors, security rules per role (anonymous / CUSTOMER / CASHIER / MANAGER / OWNER) | `@WebMvcTest` + MockMvc + `spring-security-test` |
| Integration | Flyway migrations run on a real MySQL test DB; repository queries; full HTTP flow: register → login → me → change password; public catalogue responses compared against the frontend contract (48 products, 12 categories, 8 brands) | `@SpringBootTest` against `siva_crackers_test` (Maven profile `integration`) |
| Frontend | Re-run the Phase 1–3 browser suites against the real backend; new login/register/account checks; lint and build | Browser scripts as in earlier phases |
| Manual | Postman collection `postman/SivaCrackers.postman_collection.json` | – |

`mvn test` runs unit + web tests (no database needed). `mvn verify -Pintegration` adds the MySQL integration tests.

---

## 10. Implementation sequence (each step ends with a working, tested checkpoint)

| Step | Work | Checkpoint |
|---|---|---|
| 4.1 | Maven project, profiles, env-based config, health check, global error handler, CORS | App starts; fails clearly when env vars are missing |
| 4.2 | Flyway `V1__core_schema.sql`; entities and repositories | Migrations run on MySQL; schema verified |
| 4.3 | Demo seed `V100__demo_catalogue.sql` generated from the existing JSON (same 48 products, 12 categories, 8 brands, settings, banners), all marked demo | Row counts match the frontend data |
| 4.4 | Public catalogue, price list, categories, brands, settings, banners APIs + DTOs + validation | Responses match mock JSON; unit + web tests |
| 4.5 | Security: users, BCrypt, JWT, filter, auth + account endpoints, owner bootstrap, login lockout, admin settings endpoints | Role matrix tests pass |
| 4.6 | Integration tests on MySQL test DB; OpenAPI; `docs/API.md`; Postman collection; `backend/README.md` | `mvn verify -Pintegration` green |
| 4.7 | Frontend: real API switch, demo flag from API, Login / Register / My Account | Phase 1–3 browser suites pass on the real API |
| 4.8 | Final audit (security, no ordering features, no invented data), docs, README | Then I stop and ask for approval to commit and push |

---

## 11. Decisions needed from you

| # | Decision | My recommendation |
|---|---|---|
| D1 | Spring Boot version and Java target | **Spring Boot 4.1.3, Java 21 target** (runs on your Java 25). Alternative: 4.2.0 (newest, released very recently) or Java 25 target |
| D2 | Token storage in the browser | **Bearer token in localStorage** (already wired in `apiClient.js`), 60-minute expiry. The more secure alternative, an httpOnly cookie, needs CSRF protection and more setup; it can be adopted in Phase 7 hardening |
| D3 | Customer favourites (Phase 0 feature) | **Defer** (not in your Phase 4 brief). Accounts will have profile + password only for now |
| D4 | Demo data in the real database | **Yes for development** (`APP_SEED_DEMO=true`), clearly flagged; off by default so production starts clean |
| D5 | Database user | **Dedicated `siva_app` MySQL user** (you create it with the README SQL and set its password yourself) rather than `root` |

### What I will need from you before step 4.2
- Either create the `siva_app` user yourself with the SQL I provide, **or** tell me to use an existing MySQL account. In both cases you put the password in your own environment variables. Please don't paste passwords into the chat.
- Choose an email and password for the first OWNER account and set them as `BOOTSTRAP_OWNER_EMAIL` / `BOOTSTRAP_OWNER_PASSWORD`.

---

## Completion status (updated 26 Nov 2026)

| Item | Status | Evidence |
|---|---|---|
| Development database `siva_crackers` | **Done** | `DB_PASSWORD` corrected locally (value never displayed); backend started against it: Flyway applied V1–V3, then V4; demo catalogue seeded (12 / 8 / 48); public APIs 200, unknown product 404 |
| `docs/API.md` | **Done** | All 91 endpoints that exist in the code, with auth, roles, parameters, bodies, responses and errors |
| Postman collection | **Done** (in `docs/postman/`, not `postman/`) | `SivaCrackers.postman_collection.json` (93 requests covering all 91 endpoints; CSRF header filled automatically) + `SivaCrackers.local.postman_environment.json` (placeholders only, no credentials) |
| Test-database safeguard | **Strengthened** | Besides the `_test`/localhost checks in the integration tests, every Spring test context now refuses to open a connection pool to anything but a local `*_test` MySQL database (`TestDatabaseGuard`, registered in `src/test/resources/META-INF/spring.factories`; tested by `TestDatabaseGuardTest`) |
| Backend verification | **Done** | `run-integration-tests.ps1`: 175 / 175 (58 unit/web + 117 MySQL) |

