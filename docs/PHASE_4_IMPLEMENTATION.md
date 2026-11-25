# Phase 4 – Implementation reference

Living reference for the backend as it is built. Plan and approved decisions: [PHASE_4_PLAN.md](PHASE_4_PLAN.md). Step-by-step progress and test results: [PHASE_4_NOTES.md](PHASE_4_NOTES.md).

| Sub-phase | Status |
|---|---|
| 4.1 Backend foundation and security | Done (approved) |
| 4.2 Database design and integration | Done (approved) |
| 4.3 Demo data and seeding | Done (approved) |
| 4.4 Public APIs | Done (approved) |
| 4.5 Authentication and authorization | Done (approved) |
| 4.6 Frontend integration | Done (approved) |
| 4.7 Testing and regression | Done |

---

## 1. Database

MySQL 8, InnoDB, `utf8mb4_0900_ai_ci` (comparisons are case- and accent-insensitive, so `UNIQUE(email)` and `UNIQUE(slug)` are case-insensitive).

| Database | Used by |
|---|---|
| `siva_crackers` | the application (`DB_URL`) |
| `siva_crackers_test` | integration tests only (`TEST_DB_URL`); rebuilt from scratch on every test run |

Account `siva_app@localhost` has `SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX, REFERENCES` on those two databases only, and no global privileges.

### 1.1 Migrations (Flyway, `backend/src/main/resources/db/migration`)

| Version | File | Contents |
|---|---|---|
| V1 | `V1__core_schema.sql` | All 10 tables, keys, constraints, indexes |
| V2 | `V2__business_defaults.sql` | The single business-settings row with known facts only (unknown details `NULL`), 4 highlights, 7 safety tips, 3 banners. Not demo data |
| V3 | `V3__user_token_version.sql` | `users.token_version` (>= 0): raised on password change so every older login session stops working |

Flyway runs automatically at startup. Hibernate only **validates** the schema (`ddl-auto: validate`); it never creates or alters tables. A changed migration that was already applied is detected (`validate-on-migrate`) and stops startup. New changes always go in a new `V<n>__…sql` file.

### 1.2 Tables

```
categories 1──* category_safety_notes
categories 1──* products *──1 brands
products   1──* product_images
business_settings (1 row) 1──* business_highlights, safety_tips
banners
users
```

| Table | Key columns | Rules enforced by the database |
|---|---|---|
| `categories` | name, slug, description, image_url, display_order, active, is_demo, timestamps | unique name, unique slug, slug format `lower-case-words` |
| `category_safety_notes` | (category_id, position), note | ordered; deleted with the category |
| `brands` | name, slug, description, logo_url, display_order, active, is_demo, timestamps | unique name, unique slug, slug format |
| `products` | name, slug, sku (product code), category_id, brand_id, description, pack_size, unit, mrp, price (indicative), stock_quantity, reorder_level, featured, visible_on_website, active, is_demo, timestamps | unique slug and sku; `0 ≤ price ≤ mrp`; `mrp ≥ 0`; `stock ≥ 0`; unit ∈ BOX/PACKET/PIECE/BUNDLE; category/brand cannot be deleted while products use them |
| `product_images` | (product_id, display_order), url, alt_text | ordered; deleted with the product |
| `business_settings` | shop name, tagline, address parts, phone, alternate phone, WhatsApp number + enquiry switch, email, hours, map URL + approximate flag, licence, announcement, about, price disclaimer, legal notice, WhatsApp notice, timestamps | exactly one row (`id = 1`); pincode 6 digits; map URL must be https |
| `business_highlights` | (settings_id, position), title, text | ordered |
| `safety_tips` | (settings_id, position), text | ordered |
| `banners` | eyebrow, title, subtitle, cta_label, cta_link, theme, display_order, active, is_demo, timestamps | theme ∈ gold/orange/blue; links must be internal paths (start with a single `/`) |
| `users` | full_name, email, phone, password_hash, role, active, last_login_at, timestamps | unique email (case-insensitive) and phone; phone 10 digits; role ∈ OWNER/MANAGER/CASHIER/CUSTOMER; **password_hash must be a BCrypt hash** (plain text is rejected) |

Indexes on `products`: category, brand, price, name, created_at, and (active, visible_on_website, featured) for listings. Categories, brands and banners have (active, display_order).

**Deliberately absent:** order, cart, checkout, payment, booking, reservation and invoice tables (a test checks this).

**Stock status is calculated, not stored:** `0` → OUT_OF_STOCK, `≤ reorder_level` → LOW_STOCK, otherwise IN_STOCK. It can never contradict the stock number.

**Demo identification:** `is_demo` on categories, brands, products and banners. The settings row and its highlights/tips are real (approved) content.

### 1.3 Java mapping

| Package | Entities | Repositories |
|---|---|---|
| `common.entity` | `AuditedEntity` (created/updated, UTC, JPA auditing), `BaseEntity` (+ id, proxy-safe equality) | – |
| `catalog.category` | `Category` (+ ordered `safetyNotes`) | `CategoryRepository` |
| `catalog.brand` | `Brand` | `BrandRepository` |
| `catalog.product` | `Product`, `ProductImage` (embeddable, ordered), `ProductUnit`, `StockStatus` | `ProductRepository` (+ Specifications for 4.4, visible counts per category/brand, price range, lookup by slugs) |
| `settings` | `BusinessSettings` (singleton), `BusinessHighlight`, `Banner` | `BusinessSettingsRepository`, `BannerRepository` |
| `user` | `User` (toString never shows the hash), `Role` | `UserRepository` (email case-insensitive, `existsByRole` for the owner bootstrap) |

"Publicly visible" product = `active AND visible_on_website`.

---

## 2. Demo data (Phase 4.3)

| | |
|---|---|
| Switch | `APP_SEED_DEMO` – **default `false`**. Only `true` creates the `DemoDataRunner` bean |
| Source | `backend/src/main/resources/demo/{categories,brands,products}.json` – the same sample data the website used in Phases 1–3 (12 categories, 8 fictional brands, 48 products) |
| When | At application startup, after Flyway migrations |
| Marking | Every inserted category, brand and product gets `is_demo = 1`. Product and brand responses will expose `isDemo` (4.4), and the website shows DEMO tags and the demonstration-data notice |
| Idempotent | Inserts only what is missing: categories/brands matched by slug, products by slug **or** product code. Restarting with the flag on never creates duplicates |
| Real data protected | Never updates or deletes anything. A real product with a demo slug/code is skipped and left exactly as it is; an existing (real) category or brand with a demo slug is reused and keeps its real status |
| Not touched | Business settings, highlights, safety tips, banners, users |
| Dates | Products keep their original "added" dates so New Arrivals order matches the Phase 1–3 website |
| Removing it | `backend/scripts/remove-demo-data.sql` (set `APP_SEED_DEMO=false` first, take a backup): deletes demo products, then demo brands/categories no real product uses |

The frontend's `src/mocks/data/*.json` stay as the offline mock data (`VITE_USE_MOCKS=true`); the backend copy is the database seed. If either changes, keep them in step (a seeder test fails if counts change).

---

## 3. Public API (Phase 4.4)

All endpoints are `GET`, need no login, return JSON, and use the standard error format (400 with `fieldErrors`, 404, 500). Response shapes match the Phase 1–3 mock API exactly, so the React components need no changes.

**Visibility rule:** a product is public only if it is active, marked visible on the website, and its category and brand are active. Hidden products behave exactly like missing ones (404, not listed, not counted).

### 3.1 Products

| Endpoint | Purpose | Parameters (validation) |
|---|---|---|
| `/api/public/products` | Catalogue: search, filter, sort, paginate | `search` (≤ 100 chars; every word must appear in product, brand or category name; case-insensitive; `%`/`_` matched literally) · `categories`, `brands` (comma-separated or repeated slugs, ≤ 48, slug format) · `minPrice`, `maxPrice` (≥ 0, inclusive, min ≤ max) · `inStock` (true hides out-of-stock) · `sort` = `default` \| `name,asc` \| `name,desc` \| `price,asc` \| `price,desc` \| `newest` · `page` (0-based, ≥ 0) · `size` = 12 \| 24 \| 48 |
| `/api/public/products/featured` | Homepage featured products | `limit` 1–12 (default 8) |
| `/api/public/products/new-arrivals` | Newest products | `limit` 1–12 (default 8) |
| `/api/public/products/{slug}` | Product detail incl. category description and safety notes | 404 if unknown/hidden |
| `/api/public/products/{slug}/related` | Same category first, then same brand | `limit` 1–12 (default 4) |
| `/api/public/products/by-slugs` | Products for the browser-only Estimate List, in the order given | `slugs` (≤ 200); unknown/hidden skipped |
| `/api/public/price-list` | All products grouped by category, display order | – |
| `/api/public/price-range` | `{ min, max }` indicative price | – |

Response: `{ content, page, size, totalElements, totalPages }` (totalPages ≥ 1). Product: `id, name, slug, sku, description, packSize, unit, mrp, price, stockStatus, featured, createdAt (YYYY-MM-DD), images[{url, alt}], isDemo, category{id,name,slug}, brand{id,name,slug}`. Detail adds `category.description` and `safetyNotes[]`. Never exposed: stock quantity, reorder level, visibility flags, update time.

Products of one category / brand: `/api/public/products?categories={slug}` / `?brands={slug}`.

### 3.2 Categories and brands

| Endpoint | Returns |
|---|---|
| `/api/public/categories` | Active categories in display order: `id, name, slug, description, imageUrl, displayOrder, active, isDemo, productCount` |
| `/api/public/categories/{slug}` | One category + `safetyNotes[]` (404 if unknown/inactive) |
| `/api/public/brands` | Active brands, each once: `id, name, slug, description, logoUrl, displayOrder, active, isDemo, productCount` |
| `/api/public/brands/{slug}` | One brand (404 if unknown/inactive) |

### 3.3 Business information

| Endpoint | Returns |
|---|---|
| `/api/public/settings` | Same object as the Phase 1–3 settings: `shopName, tagline, address{line1, area, city, state, pincode}, phone, alternatePhone, whatsappNumber, whatsappEnquiryEnabled, email, businessHours, mapEmbedUrl, mapIsApproximate, licenceDetails, announcement{active, text}, aboutText, highlights[{title, text}], priceDisclaimer, legalNotice, whatsappNotice, safetyTips[]` **+ `demoMode`** (true while any visible product is demo data) |
| `/api/public/settings/contact` | Contact subset: address, phones, WhatsApp number + enquiry switch + notice, email, hours, map |
| `/api/public/settings/safety-tips` | `string[]` |
| `/api/public/banners` | Active homepage slides: `id, eyebrow, title, subtitle, ctaLabel, ctaLink, theme, isDemo` |

Unknown business details are `null`; nothing is invented.

### 3.4 Performance notes
Category and brand are fetch-joined (one SQL query per page); images and safety notes are batch-loaded (`default_batch_fetch_size: 50`), so a page of 48 products needs a handful of queries, not 48+.

---

## 4. Authentication and authorization (Phase 4.5)

### 4.1 How a login session works
1. The browser calls `GET /api/auth/csrf` once; the response sets the readable `XSRF-TOKEN` cookie, which the frontend sends back in the `X-XSRF-TOKEN` header on every POST/PUT/DELETE.
2. On login or registration the server sets **`SIVA_SESSION`**: a signed JWT (HS256, `JWT_SECRET`, issuer `siva-crackers`, 60 minutes) in a cookie that is **HttpOnly; Secure; SameSite=Strict; Path=/api; Max-Age=3600**. JavaScript can never read it, and it is **never in a response body or in localStorage/sessionStorage**.
3. On every request the server checks the signature and expiry, loads the user from the database and requires: account still active, same role, same `token_version`. Otherwise 401 and the cookie is deleted.
4. A new CSRF token is issued at login and logout. Logout deletes the cookie (`Max-Age=0`).

Verified on the running server (packaged jar, no dev profile):
```
POST /api/auth/register 201
  Set-Cookie: SIVA_SESSION=<redacted>; Path=/api; Max-Age=3600; Expires=...; Secure; HttpOnly; SameSite=Strict
  Set-Cookie: XSRF-TOKEN=<redacted>; Path=/; Secure; SameSite=Strict
POST /api/auth/logout 204
  Set-Cookie: SIVA_SESSION=<redacted>; Path=/api; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Secure; HttpOnly; SameSite=Strict
```

### 4.2 Endpoints

| Method + path | Who | Purpose |
|---|---|---|
| `GET /api/auth/csrf` | anyone | issues the `XSRF-TOKEN` cookie; returns `{headerName}` |
| `POST /api/auth/register` | anyone | `{fullName, email, phone?, password}`: **always a CUSTOMER**; any `role` field sent is ignored; 201 + session cookie |
| `POST /api/auth/login` | customers | `{identifier (email or mobile), password}` |
| `POST /api/auth/staff/login` | OWNER / MANAGER / CASHIER | same body; customers are refused here and staff are refused on the customer login |
| `POST /api/auth/logout` | anyone | 204, deletes the cookie |
| `GET /api/auth/session` | anyone | `{user}`: the current user, or `null` for visitors (always 200; used by the website on page load) |
| `GET /api/auth/me` | logged in | the current user (`id, fullName, email, phone, role, createdAt`), 401 otherwise |
| `GET / PUT /api/account/profile` | logged in | view / change own name and mobile |
| `PUT /api/account/password` | logged in | `{currentPassword, newPassword}`; signs out every other session and issues a new cookie |
| `GET /api/admin/settings` | OWNER, MANAGER | editable business information |
| `PUT /api/admin/settings` | OWNER | replace business information (validated: 10-digit mobiles, 6-digit pincode, Google Maps https link, at most 8 highlights and 20 tips; blank = "To be updated") |
| `POST /api/admin/staff` | OWNER | create a MANAGER or CASHIER account (an OWNER can never be created through the API) |

No response ever contains a password, password hash or token.

### 4.3 Rules
| Rule | Implementation |
|---|---|
| Passwords | 8-72 characters (BCrypt limit), at least one letter and one digit; stored only as BCrypt (strength 12); the database rejects anything that is not a BCrypt hash |
| Wrong email vs wrong password | identical 401 "Incorrect email/phone or password." and similar timing (a dummy hash is checked for unknown accounts) |
| Account lockout | **5 failed logins: locked 15 minutes** (429 + `Retry-After`), even with the right password; a successful login resets the count |
| Per-IP limits | 30 login attempts / 15 min, 10 registrations / hour; password-change attempts limited per account |
| Duplicates | email (any case) or mobile already registered: 409 |
| Roles | `/api/admin/**` needs a staff role; finer per-endpoint `@PreAuthorize`; CUSTOMER and CASHIER get 403 on settings, MANAGER is read-only |
| First OWNER | `BOOTSTRAP_OWNER_EMAIL` + `BOOTSTRAP_OWNER_PASSWORD`: used once, only while **no OWNER exists**; never changes an existing account; password must meet the rules; neither value is ever logged. Remove both variables after the first start. There is no default admin account or password |
| Lockout storage | in memory (single server); a restart clears it. Fine for one shop server; noted for any future scaling |

## 5. Frontend integration (Phase 4.6)

### 5.1 How the website talks to the API
| Item | Setting |
|---|---|
| API base | `VITE_API_URL=/api` (relative). In development Vite forwards `/api` to `DEV_API_PROXY_TARGET` (default `http://localhost:8081`), so site and API share one origin: cookies and CSRF work with no CORS |
| Mock mode | `VITE_USE_MOCKS=false` by default. `true` still runs the Phase 1-3 sample data without a backend; the sample data is a separate chunk that the live site never downloads |
| Axios | `withCredentials: true`; reads `XSRF-TOKEN` and sends `X-XSRF-TOKEN`; fetches a CSRF cookie before the first POST/PUT/DELETE and retries once if the token was rotated; error messages and `fieldErrors` passed to forms |
| Tokens | **No token in JavaScript**: the old `localStorage` token interceptor is removed. The only cookie readable by page scripts is `XSRF-TOKEN`; storage holds only the Estimate List (`siva_estimate_v1`) |
| Demo notice | from the backend: shown while `settings.demoMode` is true (or in mock mode) |

### 5.2 Accounts in the website
| Route | Page |
|---|---|
| `/login` | email **or** mobile + password; generic error for wrong details; lockout message from the server; returns to the page the visitor came from (internal paths only) |
| `/register` | name, email, optional mobile, password + confirm; client checks mirror the server rules; always a customer account; notice that nothing is sold, booked or paid online and that the Estimate List stays in the browser |
| `/account` (protected) | view email, edit name/mobile, change password (signs out other devices), log out |

- On page load `AuthProvider` calls `GET /api/auth/session` (always 200; `user` is null for visitors, so a normal visit logs no errors).
- **Session expiry:** any API call answered with 401 while logged in clears the user; protected pages redirect to `/login` with "Your session has ended. Please log in again." and return afterwards.
- Header shows **Login / Signup** for visitors and **Hi, <first name>** (link to My Account) when logged in.
- Privacy Policy "Customer accounts" section now describes what is stored, the login cookie, and how to update or ask for deletion.
- Staff (OWNER/MANAGER/CASHIER) do not log in on the public website; the admin app comes in Phase 5.

## 6. Tests

| Command | What runs | Database |
|---|---|---|
| `mvn test` | unit + web-layer tests | none |
| `.\scripts\run-integration-tests.ps1` | the above + MySQL integration tests (`*IT`) | `siva_crackers_test` only |
| `npm run lint`, `npm run build` (in `frontend/`) | ESLint, production build | none |
| `frontend/scripts/regression-check.js` | browser regression of every public route against the live API (see the file header for how to run it) | whatever the running backend uses |

Integration-test safety:
1. `TEST_DB_URL` must be a **localhost** URL whose database name ends in **`_test`**, otherwise the run aborts.
2. Before cleaning, the tests ask MySQL which database they are connected to and abort unless it ends in `_test`.
3. The script loads **only** `TEST_DB_URL`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD` from `.env`, so local development values (e.g. `APP_COOKIE_SECURE=false`) cannot change test behaviour.
