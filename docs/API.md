# SIVA Crackers – REST API reference

Every endpoint in this file exists in `backend/src/main/java` (91 endpoints: 89 under `/api`, plus `/uploads/**` and the health check). Base URL in development: `http://localhost:8081` (the website at `http://localhost:5173` forwards `/api` and `/uploads` to it). A Postman collection is in [`docs/postman/`](postman/).

The public website is information-only. There are **no** order, cart, checkout, payment, booking, reservation, sales or invoice endpoints.

## 1. Conventions

| Topic | Rule |
|---|---|
| Format | JSON (UTF-8). Prices and amounts are decimal numbers in INR; dates `yyyy-MM-dd` (Indian calendar date); instants ISO-8601 UTC (`2026-11-26T04:30:00Z`) |
| Login | `SIVA_SESSION` cookie: HttpOnly, Secure (except local dev), SameSite=Strict, Path=`/api`, 60 minutes. Set by the login/register endpoints – never returned in a body |
| CSRF | Every POST/PUT/PATCH/DELETE needs the `X-XSRF-TOKEN` header equal to the `XSRF-TOKEN` cookie (from `GET /api/auth/csrf`). Missing/wrong → 403 |
| Roles | `CUSTOMER` (website account), `OWNER`, `MANAGER`, `CASHIER` (staff). Admin rules are checked twice on the server: by URL before the request body is read, and on each controller method |
| Paging | `?page=` (0-based) `&size=`; responses `{content, page, size, totalElements, totalPages}` (`totalPages` ≥ 1) |
| Errors | `{timestamp, status, error, message, path, fieldErrors:[{field, message}]}`. Field messages are short predicates, e.g. `{"field":"price","message":"must not be more than the MRP"}`. Internal details, SQL and stack traces are never returned; rejected input is not echoed |
| Common statuses | 200 OK · 201 Created · 204 No Content · 400 validation · 401 not logged in / session ended · 403 wrong role or missing CSRF token · 404 not found · 409 conflict (duplicate, not allowed in this state, stock would go negative) · 413 upload too large · 429 too many attempts (`Retry-After` header) · 500 generic message only |

### Role table (admin)

| Area | OWNER | MANAGER | CASHIER |
|---|---|---|---|
| Dashboard, products, categories, brands, image uploads | ✔ | ✔ | – |
| Stock: view stock, alerts, history | ✔ | ✔ | ✔ (view) |
| Stock: record movements, change low-stock level | ✔ | ✔ | – |
| Suppliers, expenses, expense categories, reports | ✔ | ✔ | – |
| Shop customers: view / search | ✔ | ✔ | ✔ |
| Shop customers: add, edit, delete | ✔ | ✔ | – |
| Business settings: read | ✔ | ✔ | – |
| Business settings: change; homepage slides; staff; activity log | ✔ | – | – |

## 2. Health

| Method | Path | Auth | Purpose / response |
|---|---|---|---|
| GET | `/api/health` | none | `{"status":"UP"}` |

## 3. Public catalogue (no login)

| Method | Path | Parameters | Response / errors |
|---|---|---|---|
| GET | `/api/public/products` | `search` (≤100 chars), `categories`, `brands` (comma-separated slugs, ≤48 each), `minPrice`, `maxPrice` (≥0, min ≤ max), `inStock` (true/false), `sort` = `default`\|`name,asc`\|`name,desc`\|`price,asc`\|`price,desc`\|`newest`, `page`, `size` = 12\|24\|48 | Page of `ProductSummary {id, name, slug, sku, description, packSize, unit, mrp, price, stockStatus (IN_STOCK\|LOW_STOCK\|OUT_OF_STOCK), featured, addedOn, images[{url, alt}], isDemo, category{id,name,slug}, brand{id,name,slug}}`. 400 for invalid values |
| GET | `/api/public/products/featured` | `limit` 1–12 (default 8) | `ProductSummary[]` |
| GET | `/api/public/products/new-arrivals` | `limit` 1–12 (default 8) | `ProductSummary[]` |
| GET | `/api/public/products/by-slugs` | `slugs` (comma-separated, ≤200) | `ProductSummary[]`; unknown slugs skipped (Estimate List) |
| GET | `/api/public/products/{slug}` | slug format `a-z0-9-` | `ProductDetail` (summary + category description + category safety notes). 404 unknown / hidden |
| GET | `/api/public/products/{slug}/related` | `limit` 1–12 (default 4) | `ProductSummary[]` (same category first, then same brand) |
| GET | `/api/public/price-list` | – | `[{category, products[]}]` for every visible product |
| GET | `/api/public/price-range` | – | `{min, max}` of visible prices (null when none) |
| GET | `/api/public/categories` | – | Active categories with `productCount` (visible products) |
| GET | `/api/public/categories/{slug}` | – | One category incl. safety notes; 404 if unknown/inactive |
| GET | `/api/public/brands` | – | Active brands with `productCount` |
| GET | `/api/public/brands/{slug}` | – | One brand; 404 if unknown/inactive |
| GET | `/api/public/settings` | – | Business information `{shopName, tagline, address{…}, phone, alternatePhone, whatsappNumber, whatsappEnquiryEnabled, email, businessHours, mapEmbedUrl, mapIsApproximate, licenceDetails, announcement{active,text}, aboutText, highlights[], priceDisclaimer, legalNotice, whatsappNotice, safetyTips[], demoMode}`. Unknown details are `null` |
| GET | `/api/public/settings/contact` | – | Contact subset |
| GET | `/api/public/settings/safety-tips` | – | `string[]` |
| GET | `/api/public/banners` | – | Active homepage slides |
| GET | `/uploads/{products\|brands\|categories}/{32-hex}.{jpg\|png\|webp}` | – | Uploaded image (long cache, `Content-Security-Policy: sandbox`, `nosniff`). 404 for any other name |

"Visible" = product active and shown on the website, and its category and brand active.

## 4. Authentication and customer account

| Method | Path | Auth | Body | Success | Errors |
|---|---|---|---|---|---|
| GET | `/api/auth/csrf` | none | – | 200 `{headerName:"X-XSRF-TOKEN"}` + `XSRF-TOKEN` cookie | – |
| GET | `/api/auth/session` | none | – | 200 `{user}` – the logged-in user or `null` (never 401) | – |
| GET | `/api/auth/me` | logged in | – | 200 `{id, fullName, email, phone, role, createdAt}` | 401 |
| POST | `/api/auth/register` | none + CSRF | `{fullName*, email*, phone, password*}` | 201 user + session cookie. **Always role CUSTOMER** (a `role` field is ignored) | 400 (email format, mobile `[6-9]` + 9 digits, password 8–72 chars with a letter and a digit), 409 email/mobile already registered, 429 (10 registrations/hour per IP) |
| POST | `/api/auth/login` | none + CSRF | `{identifier* (email or mobile), password*}` | 200 user + cookie (customers only) | 401 `Incorrect email/phone or password.` (same for unknown account), 429 after 5 failures (locked 15 min) or 30 attempts/15 min per IP |
| POST | `/api/auth/staff/login` | none + CSRF | same | 200 (OWNER/MANAGER/CASHIER only) | same; deactivated accounts 401 |
| POST | `/api/auth/logout` | none + CSRF | – | 204, cookie deleted | – |
| GET | `/api/account/profile` | logged in | – | 200 user | 401 |
| PUT | `/api/account/profile` | logged in + CSRF | `{fullName*, phone}` | 200 user | 400, 409 mobile used |
| PUT | `/api/account/password` | logged in + CSRF | `{currentPassword*, newPassword*}` | 200 user + **new** cookie; all other sessions end | 400 wrong current / same as old / weak, 429 |

## 5. Admin – catalogue (OWNER, MANAGER)

| Method | Path | Body / parameters | Success | Errors |
|---|---|---|---|---|
| GET | `/api/admin/dashboard` | – | `{totalProducts, activeProducts, inactiveProducts, visibleProducts, hiddenProducts, featuredProducts, lowStockProducts, outOfStockProducts, demoProducts, totalCategories, activeCategories, totalBrands, activeBrands, totalStaff, activeStaff, activeSuppliers, expensesThisMonth, productsByCategory[], recentProducts[]}` – live counts, no sales figures | – |
| GET | `/api/admin/products` | `search` (name/code), `categoryId`, `brandId`, `availability` = IN_STOCK\|LOW_STOCK\|OUT_OF_STOCK, `visibility` = VISIBLE\|HIDDEN\|INACTIVE, `sort` (as public), `page`, `size` = 10\|25\|50 | Page of rows (any status) | 400 |
| GET | `/api/admin/products/{id}` | – | Full product incl. stock, flags, images, timestamps | 404 |
| POST | `/api/admin/products` | `{name*, slug (empty = from name), sku*, categoryId*, brandId*, description, packSize*, unit* BOX\|PACKET\|PIECE\|BUNDLE, mrp*, price*, stockQuantity*, reorderLevel*, featured*, visibleOnWebsite*, active*, images*[{url, alt}] (≤6, uploaded here only)}` | 201. Opening stock recorded as a stock movement | 400 (prices > 0, ≤ 2 decimals, price ≤ MRP, slug format, unknown category/brand, outside image URL), 409 code or slug used |
| PUT | `/api/admin/products/{id}` | same | 200. A changed stock quantity is recorded as a stock movement; replaced photos removed from disk | 400, 404, 409 |
| PATCH | `/api/admin/products/{id}` | any of `{visibleOnWebsite, active, featured, stockQuantity}` | 200 | 400, 404 |
| DELETE | `/api/admin/products/{id}` | – | 204; stock history kept (product reference cleared), photos removed, activity logged | 404 |
| GET | `/api/admin/categories` | `search` | All categories with total and visible product counts | – |
| GET | `/api/admin/categories/{id}` | – | 200 | 404 |
| POST / PUT | `/api/admin/categories` · `/{id}` | `{name*, slug, description, imageUrl (uploaded), displayOrder* 0–9999, active*, safetyNotes*[] (≤10 × 300)}` | 201 / 200 | 400, 404, 409 name/slug used (any letter case) |
| DELETE | `/api/admin/categories/{id}` | – | 204 | 409 while products use it (message explains: move products or mark inactive) |
| GET, GET /{id}, POST, PUT /{id}, DELETE /{id} | `/api/admin/brands` | `{name*, slug, description, logoUrl (uploaded), displayOrder*, active*}` | as categories | as categories |
| POST | `/api/admin/uploads/{products\|brands\|categories}` | multipart `file` | 201 `{url}` | 400 not JPEG/PNG/WebP (detected from content; SVG/HTML refused), empty, damaged, > 8000 px; 413 > 2 MB; 404 unknown folder. Location/camera metadata is removed |

## 6. Admin – stock (view: all staff; changes: OWNER, MANAGER)

| Method | Path | Body / parameters | Success | Errors |
|---|---|---|---|---|
| GET | `/api/admin/inventory` | same filters as admin products | Page of `{productId, name, sku, category, brand, stockQuantity, reorderLevel, stockStatus, active, visibleOnWebsite, imageUrl}` | 400 |
| GET | `/api/admin/inventory/alerts` | – | `{lowStockCount, outOfStockCount, lowStock[], outOfStock[]}` (active products) | – |
| GET | `/api/admin/inventory/reasons` | – | `[{code, type, label}]` usable reasons | – |
| GET | `/api/admin/inventory/products/{productId}` | – | One stock row | 404 |
| GET | `/api/admin/inventory/movements` | `from`, `to` (dates, ≤5 years), `type` = STOCK_IN\|STOCK_OUT\|ADJUSTMENT, `reason`, `productId`, `supplierId`, `search`, `page`, `size` = 25\|50\|100 | Page of `{id, productId, productName, productSku, type, reason, reasonLabel, previousQuantity, quantityChange, newQuantity, supplier{id,name}, unitCost, note, performedByName, createdAt}`, newest first | 400 |
| POST | `/api/admin/inventory/movements` | `{productId*, reason*, quantity*, supplierId, unitCost, note}` – reasons: in `PURCHASE`, `CUSTOMER_RETURN`, `OTHER_IN`; out `SOLD_AT_SHOP`, `DAMAGED`, `RETURNED_TO_SUPPLIER`, `EXPIRED`, `OTHER_OUT`; adjustment `STOCK_COUNT`, `OTHER_ADJUSTMENT`. In/out: `quantity` = units moved (≥1). Adjustment: `quantity` = correct quantity on hand | 201 movement (product row locked during the change) | 400 (quantity, reason set automatically, supplier only for stock in/returns, unit cost only for purchases, inactive supplier), 409 stock would go below 0 |
| PATCH | `/api/admin/inventory/products/{productId}/threshold` | `{reorderLevel*}` 0–100000 | 200 stock row (the website's "Few left" follows it) | 400, 404 |

## 7. Admin – suppliers, expenses, customers (OWNER, MANAGER; customer look-up also CASHIER)

| Method | Path | Body / parameters | Success | Errors |
|---|---|---|---|---|
| GET | `/api/admin/suppliers` | `search`, `active` | Suppliers (active first) | – |
| GET | `/api/admin/suppliers/{id}` | – | `{supplier, deliveries, unitsReceived, purchaseValue, lastDeliveryAt}`; history: `GET /api/admin/inventory/movements?supplierId=` | 404 |
| POST / PUT | `/api/admin/suppliers` · `/{id}` | `{name*, contactPerson, phone, email, address, notes}` | 201 / 200 | 400, 409 name used |
| PATCH | `/api/admin/suppliers/{id}/status` | `{active*}` | 200 (suppliers are never deleted) | 404 |
| GET | `/api/admin/expense-categories` | – | Categories (7 created by migration V4) | – |
| POST / PUT | `/api/admin/expense-categories` · `/{id}` | `{name*, displayOrder*, active*}` | 201 / 200 | 400, 409 |
| GET | `/api/admin/expenses` | `from`, `to`, `categoryId`, `status` = ACTIVE\|CANCELLED, `search`, `sort` = date,desc\|date,asc\|amount,desc\|amount,asc, `page`, `size` = 25\|50\|100 | `{page, activeCount, activeTotal}` (totals over all pages, cancelled excluded) | 400 |
| GET | `/api/admin/expenses/{id}` | – | Expense | 404 |
| POST / PUT | `/api/admin/expenses` · `/{id}` | `{categoryId*, expenseDate* (not future), amount* > 0 (≤2 decimals), description*, paidTo, reference, notes}` | 201 / 200; recorder saved; changes logged with before/after | 400, 404, 409 editing a cancelled expense |
| POST | `/api/admin/expenses/{id}/cancel` | `{reason*}` | 200 (kept, marked cancelled, no longer counted) | 400, 409 already cancelled |
| GET | `/api/admin/expenses/summary/monthly` | `year` (default current) | `{year, months[{month, count, total}], yearTotal}` | 400 |
| GET | `/api/admin/customers` | `search` (name, mobile, email) | Up to 200 contacts – all staff | – |
| GET | `/api/admin/customers/{id}` | – | Contact – all staff | 404 |
| POST / PUT | `/api/admin/customers` · `/{id}` | `{fullName*, phone, email (one of them required), address, notes}` | 201 / 200 | 400, 409 mobile already saved |
| DELETE | `/api/admin/customers/{id}` | – | 204 (permanent; logged) | 404 |

## 8. Admin – reports (OWNER, MANAGER)

JSON by default; add `format=csv` for a CSV download (UTF-8 with BOM, spreadsheet formulas neutralised, at most 10,000 rows). Periods default to the last 30 days, `from` ≤ `to`, at most 5 years. No sales, revenue or profit figures exist; "purchase value" is only the unit cost staff entered on stock received.

| Method | Path | Parameters | Response |
|---|---|---|---|
| GET | `/api/admin/reports/summary` | `from`, `to`, `groupBy` = DAY (≤1 year)\|MONTH\|YEAR, `format` | `{from, to, groupBy, rows[{period, stockInUnits, stockOutUnits, adjustmentUnits, movements, purchaseValue, expenseCount, expenseTotal}], totals}` |
| GET | `/api/admin/reports/stock-movements` | `from`, `to`, `type`, `reason`, `supplierId`, `productId`, `search`, `page`, `size`, `format` | Page of movements / CSV |
| GET | `/api/admin/reports/low-stock` | `format` | Same as inventory alerts / CSV |
| GET | `/api/admin/reports/suppliers` | `from`, `to`, `format` | `[{supplierId, name, active, deliveries, unitsReceived, purchaseValue, lastDeliveryAt}]` |
| GET | `/api/admin/reports/expenses` | `from`, `to`, `format` | `{from, to, byCategory[{categoryId, category, count, total}], count, total}`; CSV adds every expense line |

`format` other than `json`/`csv` → 400.

## 9. Admin – owner only

| Method | Path | Body / parameters | Success | Errors |
|---|---|---|---|---|
| GET | `/api/admin/staff` | `search`, `role` = OWNER\|MANAGER\|CASHIER, `active` | `[{id, fullName, email, phone, role, active, createdAt, lastLoginAt}]` (website customers excluded) | 400 |
| GET | `/api/admin/staff/{id}` | – | Staff member | 404 |
| POST | `/api/admin/staff` | `{fullName*, email*, phone, password*, role* MANAGER\|CASHIER}` | 201 | 400 (role OWNER refused, weak password), 409 email/mobile used |
| PUT | `/api/admin/staff/{id}` | `{fullName*, email*, phone, role*}` | 200; a role change ends that person's sessions | 400 own role / OWNER role / new OWNER, 409 |
| PATCH | `/api/admin/staff/{id}/status` | `{active*}` | 200; deactivating ends sessions and blocks login | 400 own account or an OWNER |
| POST | `/api/admin/staff/{id}/reset-password` | `{newPassword*}` | 204; all sessions end; logged without the password | 400 weak / OWNER account |
| GET | `/api/admin/audit` | `entityType`, `page`, `size` = 25\|50\|100 | Page of `{id, occurredAt, actorName, action, entityType, entityId, details}` newest first | 400 |
| GET | `/api/admin/settings` | – | Editable business settings (also MANAGER) | – |
| PUT | `/api/admin/settings` | full settings object (see `AdminSettingsDtos`): mobiles `[6-9]` + 9 digits, 6-digit pincode, Google Maps https link, ≤8 highlights, ≤20 tips | 200 | 400 |
| GET, POST, PUT /{id}, DELETE /{id} | `/api/admin/banners` | `{eyebrow, title*, subtitle, ctaLabel, ctaLink (internal path, needs label), theme* gold\|orange\|blue, displayOrder*, active*}` | 200/201/204 | 400 outside link / button text without link, 404 |

Activity log actions: `STAFF_CREATED`, `STAFF_UPDATED`, `STAFF_ACTIVATED`, `STAFF_DEACTIVATED`, `STAFF_PASSWORD_RESET`, `PRODUCT_DELETED`, `CATEGORY_DELETED`, `BRAND_DELETED`, `LOW_STOCK_LEVEL_CHANGED`, `SUPPLIER_CREATED/UPDATED/ACTIVATED/DEACTIVATED`, `EXPENSE_CREATED/UPDATED/CANCELLED`, `EXPENSE_CATEGORY_CREATED/UPDATED`, `CUSTOMER_CREATED/UPDATED/DELETED`. Stock changes are recorded in the stock history instead.
