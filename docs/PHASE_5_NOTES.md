# Phase 5 – Admin dashboard: notes

**Status:** `95f99fe`, `5208911`, `bb26f5f` (pushed). **Browser-verified on 27 Nov 2026** – every admin page and workflow, all roles, at 1440 / 1366 / 768 / 390 / 320 px; see [PHASE_5_6_VERIFICATION.md](PHASE_5_6_VERIFICATION.md). Fixes from that check are not committed yet.
Reference: [PHASE_5_IMPLEMENTATION.md](PHASE_5_IMPLEMENTATION.md).

## Phase 5 final (26 Nov 2026)

- **Owner bootstrap verified** on the test database: with no OWNER, the backend created one from `BOOTSTRAP_OWNER_*` (log line "OWNER account created…"); the throwaway account was deleted afterwards.
- **Sidebar order** (only allowed sections shown): Dashboard, Products, Categories, Brands, Homepage Management, Business Settings, Staff Management, Inventory, Stock Movements, Suppliers, Billing / POS, Bill History, Customers, Expenses, Reports, Activity Log.
- **Cashier**: now also Billing / POS, Bill History and adding customers; starts on Billing / POS. Void is OWNER/MANAGER only.
- **Dashboard**: sales today and this month (finalized in-store bills only, no profit), total and active suppliers, recent bills, recent stock changes, recent expenses; stock value is **not** shown (no reliable cost for every product).
- Tests: backend 185/185, frontend 57/57 (`roles.test.jsx` checks the exact sidebar order, cashier menu and start page, `canVoid`).

## Phase 5 completion (26 Nov 2026, committed in `5208911`)

- **Role decision:** the Phase 0 role table replaces the owner-only rule (your answer on 26 Nov 2026). OWNER: everything. MANAGER: dashboard, products, categories, brands, uploads, stock, suppliers, expenses, shop customers, reports; reads business settings. CASHIER: views stock and stock history, looks up customers. Only the OWNER: staff, activity log, homepage slides, changing settings.
- **Enforced on the server twice:** URL rules in `SecurityConfig` (checked before the request body is read – so unauthorised users get 403, never validation details) and `@PreAuthorize` on every controller.
- **Staff management** (`/admin/staff`, OWNER): list with role, status, creation date and last login; search; filter by role and status; create MANAGER/CASHIER (never OWNER); edit name, email, mobile, role; activate / deactivate (with confirmation); reset password (temporary password given in person). Rules: no one can change their own role or deactivate themselves; an OWNER cannot be re-assigned or deactivated; role change, deactivation and password reset end that person's sessions immediately; deactivated accounts cannot log in; every action is in the **activity log** (`/admin/activity`, OWNER) – passwords never logged.
- **Admin for all staff roles:** staff login accepts OWNER, MANAGER and CASHIER; the menu shows only the sections of the person's role; other pages show "not available for your role"; cashiers start on the Stock page; website customers are refused.
- **Dashboard:** adds total / active staff, active suppliers, this month's expenses; low-stock and out-of-stock counts link to the Stock page.
- Tests: backend `StaffApiIT` (6) and role checks in `AdminApiIT` / `BusinessApiIT`; frontend `roles.test.jsx` (3) and staff tests in `phase6.test.jsx` (3). Totals: backend 175 / 175, frontend 47 / 47.

## Delivered

- OWNER-only admin at `/admin`: login, dashboard, products (list / view / add / edit / delete / quick switches), categories, brands, website content (homepage slides and text), business settings.
- Backend: admin endpoints for dashboard, products, categories, brands, banners and image uploads; existing settings endpoint reused. **No database migration** – the Phase 4 schema already had every field.
- Image uploads stored on disk (`APP_UPLOAD_DIR`, default `backend/uploads/`, git-ignored), checked by content, metadata (incl. GPS) removed, served read-only at `/uploads/**`.
- Public website: uploaded product photos, brand logos and category pictures appear automatically; missing images fall back to the existing illustrations / initials. All Phase 1–4 behaviour unchanged.
- Frontend test setup: Vitest 5 + Testing Library + jsdom (`npm test`).

## How the owner gets in

1. Create the OWNER once with `BOOTSTRAP_OWNER_EMAIL` / `BOOTSTRAP_OWNER_PASSWORD` in `backend/.env` (see backend README), start the backend, then remove both lines.
2. Start the backend (`backend/scripts/run-dev.ps1`) and the website (`npm run dev` in `frontend/`).
3. Open http://localhost:5173/admin and log in with the OWNER's email or mobile number and password.

## Test results (26 Nov 2026, final runs)

| Check | Result |
|---|---|
| Backend `.\scripts\run-integration-tests.ps1` | BUILD SUCCESS, **155 / 155** – 55 unit/web (new: `ImageSanitizerTest` 5, `SlugsTest` 1) + 100 MySQL (new: `AdminApiIT` **17**; Phase 4 suites unchanged and passing) |
| `AdminApiIT` covers | 401 for visitors / 403 for CUSTOMER, CASHIER, MANAGER / 200 for OWNER on every admin area; writes refused without OWNER role or CSRF token and nothing changes; dashboard figures equal live SQL counts and contain no sales/order fields; product create (slug from name, not demo, shown on the website), validation (required, price > 0, price ≤ MRP, slug format, unknown category, outside / `javascript:` image URLs), duplicates (code any case, slug) → 409 with field, edit (all fields saved in the database, website updated), quick hide (website 404), delete (404 afterwards, website 404); list filters, sort, paging, invalid values → 400, SQL-like search text harmless; `<script>` stored as text; uploads (random name, metadata removed, locked-down headers; HTML/SVG/empty/over-size/unknown-folder rejected; bad names 404); replaced and deleted product photos and deleted brand logos removed from disk; category CRUD incl. case-insensitive duplicate and public reflection; delete blocked for a category/brand with products (409, nothing changed); brand CRUD with logo, inactive brand disappears from the public Brands page; homepage slides (outside links, label without link, unknown colour rejected; active/inactive reflected publicly; deleted) |
| Frontend `npm test` | **34 / 34** (5 files): protected routes (visitor → login, loading, CUSTOMER/MANAGER/CASHIER refused, OWNER allowed), admin login (field checks, staff login call, server error, session-ended message, nothing in browser storage), dashboard (figures, distribution, recent products, demo notice, no sales figures, loading, error + retry, empty state), product list (rows, filters to API, delete confirmation – cancel and confirm, error), product form (rules, field errors, create, edit loads values, server duplicate shown at the field, no double submit), categories (counts, search, add dialog, server duplicate, blocked delete explanation, delete), brands (edit, empty state), image upload (type/size check, upload, preview, replace/remove), mock mode (admin never calls the API) |
| `npm run lint` / `npm run build` | clean; admin code only in separate lazily loaded files (main public bundle contains none) |
| Phase 4 browser regression `frontend/scripts/regression-check.js` (public site, test database with demo data) | **899 / 899** at 1440 px, **250 / 250** at 360 px, **250 / 250** at 320 px |
| Browser, not logged in | `/admin/*` redirects to `/admin/login`; login page `noindex`, no horizontal overflow; login form posts to the staff login and shows the server's message for a wrong account |
| Mock mode (`VITE_USE_MOCKS=true`) | admin shows "needs the backend", login disabled, **no** API calls |
| Secrets | 317 committable files: no password or JWT secret value from `backend/.env`; `.env` files, `backend/uploads/`, `target/`, `dist/`, `node_modules/` git-ignored; the test owner's credentials were generated for the test database only, never printed, and deleted afterwards |
| No online selling | only comments stating these features do not exist; no new cart/order/payment/booking code |

## Not verified

- Waiting 60 minutes for a session to expire (automated tests only).
- The real OWNER account on the development database does not exist yet (owner task below); the browser check used a throwaway OWNER on the test database, deleted afterwards.

## Issues found and fixed

- Two buttons named "Close" in dialogs (icon and footer) – the icon is now "Close dialog".
- Admin login page was not marked `noindex` (only pages inside the layout were) – now all `/admin` pages.
- Product edit form copied loaded data into state inside an effect (flagged by the React lint rules) – the form now mounts after loading.
- Logging out from the admin briefly returned to the dashboard – logout now finishes before leaving.
- A frontend test timed out once while the machine was busy after a Maven build (5 s default) – UI tests now allow 15 s; passes repeatedly.
- One backend test reused an existing brand name, so the duplicate check (409) fired before the check it meant to test – test corrected.

## Known limitations

- An image uploaded in a form that is then abandoned stays on disk (random name, not linked). Harmless; a periodic clean-up can be added.
- Demo items keep their "Demo" label when edited; remove demo data by deleting/hiding it or with `backend/scripts/remove-demo-data.sql`.
- Login lockout counts are in memory (single server), as in Phase 4.

## Remaining owner tasks

1. Copy the working `siva_app` password into `DB_PASSWORD` in `backend/.env` so the development database can be used.
2. Change the MySQL root and `siva_app` passwords that were shared in chat earlier.
3. Create the real OWNER account (bootstrap), then remove the bootstrap lines.
4. Add real products, categories, brands, photos and business details; then delete or hide the demo data.
5. Before going live: legal review of the policy pages, HTTPS hosting with `APP_COOKIE_SECURE=true`, a persistent backed-up `APP_UPLOAD_DIR`.
