# Phase 5 – Admin dashboard: implementation reference

The shop owner manages the catalogue and homepage content in a protected admin area at **`/admin`**.
There are deliberately **no** orders, sales, payments, bookings, carts or checkout anywhere – the website stays an information catalogue.

## 1. Architecture

| Decision | Reason |
|---|---|
| Admin lives inside `frontend/` under `/admin` (lazy-loaded chunks, own layout) instead of a second app in `admin/` (Phase 0 idea) | The Phase 5 brief asks not to create a new project; since Phase 4 the login is an HttpOnly cookie on the site's own origin, so one app reuses the API client, CSRF handling, auth context and theme. Visitors never download admin code: it is split into separate files loaded only when `/admin` is opened |
| **OWNER only** for every Phase 5 endpoint (`@PreAuthorize("hasRole('OWNER')")` on each controller) | Brief: only the OWNER manages the catalogue. MANAGER/CASHIER/CUSTOMER get 403 from the API and a "No access" page in the browser. Existing Phase 4 rule kept: MANAGER may read `/api/admin/settings` |
| **No database migration** | The Phase 4 schema already has every field (product code, MRP, price, stock, "few left" level, featured/visible/active, images; category picture, brand logo, display order, active; banners) |
| Images on the server's disk (`APP_UPLOAD_DIR`, default `backend/uploads/`, git-ignored), served at `/uploads/**` | As planned in Phase 0; no cloud account needed. Storage is one class (`ImageStorage`) so it can move to cloud storage later |
| Business details reuse the Phase 4 `GET/PUT /api/admin/settings` | No duplicate API |

## 2. Admin pages

| Route | Page |
|---|---|
| `/admin/login` | Staff login (email or mobile + password). Only the OWNER gets in |
| `/admin` | Dashboard: total / active / inactive / hidden / featured / few-left / out-of-stock products, categories and brands (with active counts), products per category (bar list), 5 most recent products, quick actions, demo-data notice. All figures counted live from the database |
| `/admin/products` | Search (name or code), filter by category, brand, availability (in stock / few left / out of stock) and visibility (visible / hidden / inactive), sort (category order, newest, name, price), 10/25/50 per page; table on tablets and up, cards on phones; View / Edit / Delete (confirmation) |
| `/admin/products/new`, `/admin/products/:id/edit` | Name, product code, web address (auto from name), category, brand, description, pack size, unit, MRP, price, stock, "few left" level, show on website / active / featured, up to 6 photos with descriptions and order |
| `/admin/products/:id` | Read-only details, photos, quick switches (show on website, active, featured), link to the public page, delete |
| `/admin/categories` | Search, product counts (total and on website), add/edit dialog (name, web address, description, picture, safety notes, display order, active), delete (blocked with an explanation while products use it) |
| `/admin/brands` | Same as categories, with a logo |
| `/admin/content` | Homepage slides (add/edit/delete, internal links only) and homepage text (tagline, announcement bar, about text, highlights) |
| `/admin/settings` | Shop name, address, map link, phones, WhatsApp, email, hours, licence, notices, safety tips. Empty fields show "To be updated" on the website – nothing is invented |

Common behaviour: sidebar on large screens, slide-in menu on phones/tablets, owner name in the top bar, breadcrumbs, loading skeletons, empty states, error boxes with "Try again", success/error notifications, confirmation dialogs (focus kept inside, Escape closes, fits small screens), buttons disabled while saving (no double submission), field-level error messages from both the browser checks and the server, `noindex` on all admin pages, expired sessions send the owner back to the login page with a message and return afterwards.

Public website changes (small, backwards compatible): uploaded brand logos appear on the Brands page and homepage brand strip, uploaded category pictures on the homepage category grid, and any missing or broken image falls back to the existing illustration / initials.

## 3. API (all under `/api/admin`, OWNER only, CSRF token required for changes)

| Method + path | Purpose |
|---|---|
| `GET /dashboard` | Figures listed above; no revenue / sales / order fields |
| `GET /products?search=&categoryId=&brandId=&availability=IN_STOCK\|LOW_STOCK\|OUT_OF_STOCK&visibility=VISIBLE\|HIDDEN\|INACTIVE&sort=default\|newest\|name,asc\|name,desc\|price,asc\|price,desc&page=0&size=10\|25\|50` | Paged list `{content, page, size, totalElements, totalPages}` of all products, any status |
| `GET /products/{id}` | Full product |
| `POST /products` | Create (201). New products are never demo data |
| `PUT /products/{id}` | Full update. Replaced photos are deleted from disk after the change is saved (only if nothing else uses them) |
| `PATCH /products/{id}` | Quick change: any of `visibleOnWebsite`, `active`, `featured`, `stockQuantity` |
| `DELETE /products/{id}` | Delete (204) with its photo list; photo files removed afterwards. Nothing else refers to products (Estimate Lists live only in visitors' browsers and skip missing items) |
| `GET /categories?search=` · `GET /categories/{id}` · `POST` · `PUT /{id}` · `DELETE /{id}` | Category management. Delete → **409** with an explanation while products use it (the database also refuses: `ON DELETE RESTRICT`) |
| `GET /brands?search=` · `GET /brands/{id}` · `POST` · `PUT /{id}` · `DELETE /{id}` | Brand management, same rules |
| `GET /banners` · `POST` · `PUT /{id}` · `DELETE /{id}` | Homepage slides; links must be pages of this site (`/…`, also enforced by the database) and need both button text and link |
| `POST /uploads/{products\|brands\|categories}` (multipart field `file`) | Upload one image → 201 `{url}` |
| `GET /settings` · `PUT /settings` | Existing Phase 4 endpoints (OWNER/MANAGER read, OWNER write) |
| `GET /uploads/{folder}/{file}` (public) | Serves uploaded images |

Errors use the existing format `{status, error, message, path, fieldErrors[]}`. Duplicates (product code, web address, category/brand name – any letter case) → **409** naming the field. Validation → **400** with field errors (required fields, prices above 0 with at most 2 decimals, price ≤ MRP, web address format, known category/brand, at most 6 photos, only images uploaded through the admin). Unknown id → 404. Too large upload → 413. Database errors are never shown to users.

## 4. Image handling

| Check | Where |
|---|---|
| Type decided from the file's **content** (JPEG / PNG / WebP magic bytes and structure), never its name or the browser's claim; SVG, HTML and anything else rejected | `ImageSanitizer` (server) |
| Size ≤ 2 MB (Spring multipart limit + check), dimensions ≤ 8000 × 8000 | server; the browser pre-checks type and size for a quicker message |
| **Metadata removed**: EXIF/XMP (incl. GPS location), IPTC and comments in JPEG, text/EXIF/time chunks in PNG, EXIF/XMP chunks in WebP. Picture data is copied unchanged | `ImageSanitizer` |
| Random file names (32 hex characters), never reused; server folder never revealed | `ImageStorage` |
| Saved product/brand/category records may only point to images uploaded through the admin into the matching folder (`/uploads/products/…` for products, etc.) – no outside URLs, no `javascript:` | admin services |
| Served with `Content-Security-Policy: default-src 'none'; sandbox`, `X-Content-Type-Options: nosniff`, `Content-Disposition: inline`, long cache (names are unique) | `ImageController` |
| Unused files are deleted after a replacement or delete has been saved, only when no product, brand or category still uses them | `ImageCleanup` |

Known limitation: an image uploaded in a form that is then abandoned (never saved) stays on disk. It is harmless (random name, not linked anywhere); a periodic clean-up can be added later.

## 5. Security summary

- Every admin request is authorised on the server (URL rule: staff roles; method rule: OWNER). The browser checks are only for convenience.
- Same login cookie as Phase 4 (HttpOnly, Secure, SameSite=Strict, `/api`), CSRF token on every change, 5-failure lockout, no tokens in JavaScript storage.
- All database access through JPA / prepared statements; search text is a bound parameter with LIKE wildcards escaped (test: `' OR 1=1 --` returns nothing).
- Text is stored as entered and shown by React as text, never as HTML (test: `<script>` in a description is returned verbatim and not executed).
- Admin pages are `noindex`. Mock mode (`VITE_USE_MOCKS=true`) disables the admin completely – it never calls the admin API, so sample data cannot change real data.
- No new environment secrets. `APP_UPLOAD_DIR` is the only new (optional) setting.

## 6. Tests

| Command | What |
|---|---|
| `backend\scripts\run-integration-tests.ps1` | unit + MySQL tests (incl. `AdminApiIT`, `ImageSanitizerTest`, `SlugsTest`) |
| `cd frontend; npm test` | Vitest + Testing Library component tests (`src/admin/__tests__`) – no backend needed |
| `cd frontend; npm run lint; npm run build` | lint and production build |
| `frontend/scripts/regression-check.js` | browser regression of the public website (see file header) |

Results: see [PHASE_5_NOTES.md](PHASE_5_NOTES.md).
