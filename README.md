# SIVA Crackers – Product Catalogue & Business Management Platform

Informational product catalogue and (later) shop management system for **SIVA Crackers**, a family-owned crackers retail shop in Eachanari, Coimbatore, Tamil Nadu.

> **Legal notice:** the public website is an information catalogue only. It does not sell, book, accept payment for, or deliver firecrackers online. All purchases happen in person at the shop. WhatsApp is used for general enquiries only.

> **Demo data:** products, brands, prices and stock levels in this repository are demonstration samples, not the shop's real range or prices. They are marked `isDemo` and shown with a **DEMO** tag.

## Tech stack

| Part | Technology |
|---|---|
| Public website | React 19, Vite, Tailwind CSS 4, React Router, Lucide React, Axios |
| Admin dashboard | React, inside the website at `/admin` (Phase 5) |
| Backend | Java, Spring Boot, Spring Data JPA, Spring Security + JWT, Maven (Phase 4) |
| Database | MySQL (Phase 4) |

## Features so far

### Phase 1 – Homepage
- Announcement bar, sticky header with search, desktop navigation and a mobile menu with Menu / Categories / Brands tabs
- Hero banner carousel, quick-action tiles, shop by category, featured products, new arrivals, popular brands, about, safety tips, contact with area map
- Reusable product card: original category illustrations, indicative price with MRP, stock status, DEMO tag, WhatsApp **general enquiry** button
- Legal notice and demonstration-data notice on every page; business details shown as "To be updated" until provided

### Phase 2 – Product catalogue and filtering
- **All Products** page plus **Categories**, **category product**, **Brands** and **brand product** pages
- Instant search across product, brand and category names, with clear button, result count and empty-results message
- Filters: categories (multi-select), brand chips (multi-select), minimum/maximum price with presets, hide out-of-stock
- Removable filter chips and "Clear all"
- Sorting: default, name A–Z / Z–A, price low–high / high–low, newest
- Pagination with page numbers, Previous/Next and 12 / 24 / 48 per page
- All filters stored in the URL (shareable links, reload and Back button work)
- Desktop filter sidebar; mobile filter bottom sheet; no horizontal overflow on phones

Screenshots: [`docs/screenshots/phase2/`](docs/screenshots/phase2/)

### Phase 3 – Product details and business pages
- **Product detail** pages for every product: image gallery with full-screen viewer, brand/category links, pack size, indicative price and MRP, demo-labelled stock status, category safety information, product details, related products, breadcrumbs and a WhatsApp **general enquiry** button
- **Price List**: all products by category with MRP and indicative price, instant search, category jump links, price disclaimer and a **print-friendly layout**
- **My Estimate List**: a personal planning list saved only in the visitor's browser, with quantities and an indicative total; printable; clearly **not an order or booking**
- **About Us**, **Contact Us** (placeholders for unconfirmed details, approximate area map), **Safety Guidelines** (adult supervision, safe lighting, emergency first aid and helplines) and draft **Legal** pages (disclaimer, privacy, terms)
- Login / Signup were added in Phase 4 together with the secure backend

Screenshots: [`docs/screenshots/phase3/`](docs/screenshots/phase3/)

**Testing:** Phase 3 final audit – 72/72 desktop and 11/11 mobile automated browser checks passed (including Phase 1–2 regression, 82 internal links, 20 routes without horizontal overflow, print layout); lint and production build pass; no online ordering, checkout, payment, booking or reservation code. See [Phase 3 notes](docs/PHASE_3_NOTES.md).

### Phase 4 – Secure backend, accounts and API integration
- **Spring Boot 4.1 + MySQL backend** with Flyway migrations (10 tables, database-level checks), clearly labelled demo catalogue seeding, and a read-only public API for products, categories, brands, price list and business information
- **Customer accounts**: sign up, log in with email or mobile, My Account (details, password), log out. Login sessions use an **HttpOnly, Secure, SameSite=Strict cookie** (never stored in JavaScript), CSRF protection, BCrypt passwords, 15-minute lock after 5 failed logins, per-IP limits
- **Roles** OWNER / MANAGER / CASHIER / CUSTOMER: public sign-up only creates customers; the first OWNER is created once from environment settings; staff accounts only by the OWNER (admin app in Phase 5)
- **Website connected to the API** (Vite proxy in development); the demo-data notice follows the backend; Estimate List still stays in the browser only

**Testing:** 132/132 backend tests (49 unit/web + 83 against a MySQL test database); browser regression 899/899 desktop checks and phone checks at 375 px and 320 px; secrets and no-online-selling scans clean. See [Phase 4 notes](docs/PHASE_4_NOTES.md) and the [implementation reference](docs/PHASE_4_IMPLEMENTATION.md).

### Phase 5 – Admin dashboard
- Staff admin at **`/admin`** (owner, manager, cashier – each sees only their sections): live dashboard (catalogue, stock, suppliers, expenses, in-store sales, recent bills / stock changes / expenses), products (search, filters, sort, paging, add, edit, delete with confirmation, quick show/hide), categories and brands (product counts; deleting is blocked while products use them), homepage slides and text, business settings
- **Image uploads** for product photos, brand logos and category pictures – checked on the server by content, location/camera metadata removed – shown on the public website automatically
- Every admin action is authorised again by the server (OWNER role + CSRF); the admin is switched off in mock mode

**Testing:** 155/155 backend tests (incl. 17 new admin API tests on MySQL), 34/34 new frontend component tests (Vitest), public-site regression 899/899 desktop and 250/250 at 360 px and 320 px. The logged-in admin was not clicked through in a browser – see [Phase 5 notes](docs/PHASE_5_NOTES.md).

### Phase 6 – Inventory and business management (internal)
- **Stock**: current stock, record stock in / out / counted adjustments with reason, supplier and cost, full **stock history** (before → after, who, when), low-stock and out-of-stock alerts, per-product low-stock level; stock can never go negative and simultaneous changes are safe
- **Suppliers** with purchase history, **expenses** (cancel with reason, never deleted, monthly summary), **shop customer contacts**, **reports** (daily / monthly / yearly summary, stock, low stock, suppliers, expenses) with **CSV export**
- **Staff management** and an **activity log** (Phase 5 completion); roles follow the Phase 0 table: owner, manager, cashier
- **In-store billing / POS** (approved 26 Nov 2026, admin only): bills for customers buying at the shop counter, stock deducted exactly once, bill-level discount up to 20%, cash / UPI / card at the shop (no online payment), pending payments, void with reason (owner/manager), bill history, **PDF invoices** (no invented GST or licence), customer purchase history
- Reports add **billing** and **product sales**, with **CSV and PDF** export; revenue counts finalized bills only; no profit is shown (no reliable cost per sale). Nothing is sold online

**Testing:** 185/185 backend tests (127 on MySQL), 57/57 frontend component tests. The logged-in admin was **not** checked in a browser. See [Phase 6 notes](docs/PHASE_6_NOTES.md) and the [API reference](docs/API.md).

> **Before going live:** legal pages are drafts that need review by the owner and a legal adviser, and the safety/first-aid content and emergency numbers should be checked against official sources (listed in the Phase 3 notes).

## Project structure

| Folder | Contents | Status |
|---|---|---|
| `docs/` | Phase plans and notes, [API reference](docs/API.md), [Postman collection](docs/postman/) | Phases 0–6 |
| `frontend/` | Public website and the admin dashboard (`src/admin/`, served at `/admin`) | Phases 1–6 |
| `backend/` | REST API (Spring Boot 4.1 + MySQL) | Phases 4–6; see [backend/README.md](backend/README.md) |

## Run the website

```powershell
cd frontend
npm install
npm run dev
```

Open http://localhost:5173 (admin: http://localhost:5173/admin). Other scripts: `npm test` (component tests), `npm run lint`, `npm run build`, `npm run preview`.

The site reads everything from the Spring Boot API: start the backend first (`backend/scripts/run-dev.ps1`, port 8081); Vite forwards `/api` to it. To run without a backend on the Phase 1–3 sample data, set `VITE_USE_MOCKS=true` in `frontend/.env` (see `frontend/.env.example`). Customer Login / Signup / My Account work only with the backend.

## Documentation

- [Phase 0 – Plan](docs/PHASE_0_PLAN.md) (features, architecture, database, API, roadmap)
- [Phase 1 – Notes](docs/PHASE_1_NOTES.md)
- [Phase 2 – Notes](docs/PHASE_2_NOTES.md)
- [Phase 3 – Notes](docs/PHASE_3_NOTES.md)
- [Phase 4 – Plan](docs/PHASE_4_PLAN.md), [Notes](docs/PHASE_4_NOTES.md), [Implementation reference](docs/PHASE_4_IMPLEMENTATION.md)
- [Phase 5 – Notes](docs/PHASE_5_NOTES.md), [Implementation reference](docs/PHASE_5_IMPLEMENTATION.md)
- [Phase 6 – Notes](docs/PHASE_6_NOTES.md)
- [API reference](docs/API.md) and [Postman collection](docs/postman/)
