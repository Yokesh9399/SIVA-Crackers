# Phase 5 & Phase 6 – browser verification report (27 Nov 2026)

Verified against commit `bb26f5f` plus the fixes listed below (not committed yet).

## Set-up

| Item | Value |
|---|---|
| Backend | packaged Spring Boot jar on **8081**, profile `dev`, **TEST database `siva_crackers_test`** (Flyway: "Successfully validated 5 migrations … up to date"), `GET /api/health` → `{"status":"UP"}` |
| Frontend | Vite dev server on **5176** (`/api` and `/uploads` proxied to 8081) |
| Browser | the Claude desktop app's built-in browser (Chromium), viewport emulation 1440 / 1366 / 768 / 390 / 320 px |
| Accounts | No OWNER existed in either database, so (your choice) a **throwaway OWNER was bootstrapped on the test database** plus a test MANAGER and CASHIER created in the UI, and one website CUSTOMER registered through the public API. Test passwords were generated for this run only and never written to the repository. All test accounts and records were removed afterwards (see "Clean-up"). The development database `siva_crackers` was **not touched** (still at V4; V5 applies on its next start). |
| Evidence | every step below was done in the browser (clicks/typing) or with `fetch` from the logged-in page (same cookies and CSRF token as the UI), and checked against MySQL with read-only queries |

## Phase 5 – results

| # | Workflow | Result | Evidence |
|---|---|---|---|
| 5.1 | Owner bootstrap | PASS | test DB had 0 OWNER → backend log "OWNER account created from the BOOTSTRAP_OWNER_* settings" |
| 5.1 | Unauthenticated `/admin` | PASS | redirected to `/admin/login`; `/api/admin/*` → 401 |
| 5.1 | Invalid login | PASS | "Incorrect email/phone or password." |
| 5.1 | Owner login (email) / manager login (mobile) | PASS | dashboard / start page opened |
| 5.1 | Logout | PASS after fix **B2** | redirected to login; API 401; **old session token now refused (was accepted before the fix)** |
| 5.1 | Cookie security | PASS | `SIVA_SESSION`: HttpOnly, SameSite=Strict, Path=/api, Max-Age=3600; no token in local/session storage |
| 5.1 | CSRF | PASS | login, uploads, bills, stock without `X-XSRF-TOKEN` → 403 |
| 5.1 | Deactivated account | PASS | deactivated cashier → 401 (generic message) |
| 5.1 | Session expiry | NOT VERIFIED by waiting (60 min); covered by `JwtServiceTest` / `AuthApiIT` |
| 5.2 | Dashboard | PASS | figures equal MySQL; low/out-of-stock counts moved 5→4 / 3→4 after stock changes; recent bills / stock changes / expenses; no profit; demo notice |
| 5.3 | Products | PASS | add (validation: price > MRP, pack size), edit (price 75.50, HTML in description shown as text), hide (public API 404) / show, search, photo upload (random name, served with `nosniff`, shown on public page); product + photo + opening stock + `PRODUCT_CREATED` in MySQL |
| 5.3 | Upload rejections | PASS | HTML renamed .png, SVG, PDF, empty → 400; unknown folder → 404; no CSRF → 403; 2.5 MB → 413 from the backend and "larger than 2 MB" in the form (Vite's dev proxy drops the connection instead of passing the 413 – development server only) |
| 5.4 | Categories | PASS | create, duplicate name → field error, search, rename, deactivate (its products disappear from the website, admin explains why), delete blocked while products use it |
| 5.5 | Brands | PASS | create, search, rename, deactivate (gone from public brands) |
| 5.6 | Homepage slides | PASS | add (outside link refused), shown on the public homepage with button link, order changed, switched off (gone from website), deleted |
| 5.7 | Business settings | PASS | invalid phone refused; opening hours saved, survived reload, shown in public settings; reverted |
| 5.8 | Staff | PASS | list, search, create manager (UI) / cashier, OWNER role refused, weak password and duplicate email refused, edit mobile, reset password (old one refused, new works, sessions ended), deactivate (login refused), owner cannot demote or deactivate self |
| 5.8 | Role permissions | PASS | MANAGER: 12 sections, staff/audit/settings-write/slides/payment-method changes → 403, can void; CASHIER: 5 sections, billing + add customer OK; void, stock change, customer edit/delete, products, suppliers, expenses, reports, uploads → 403; CUSTOMER: all admin APIs 403, staff login refused, `role: OWNER` at sign-up ignored; direct URLs show "not available for your role" |
| 5.9 | Activity log | PASS | product, bill, customer, expense (before/after), supplier, staff entries with staff name |

## Phase 6 – results

| # | Workflow | Result | Evidence |
|---|---|---|---|
| 6.1 | Stock in / out / adjust | PASS | 10→30 (supplier, cost 50.25), 30→27 damaged, 27→25 count; live "after saving" preview; history chain unbroken and sums to stock (SQL) |
| 6.1 | Validation | PASS | negative / zero / non-number quantity, unknown product, out > stock (409), cost on non-purchase, manual BILL_SALE, inactive supplier → refused |
| 6.1 | Concurrency | PASS | 10 parallel stock-outs of 5 against 25 → exactly 5 accepted, 5 × 409, stock 0 |
| 6.1 | Last updated / by | PASS after fix **B1** | list showed it; single-product endpoint returned null before the fix |
| 6.2 | Low-stock alerts | PASS | threshold changed 10→5; status Out → In stock after restock; alerts and dashboard follow |
| 6.3 | Suppliers | PASS | add (phone/email validation), created date, details with purchase totals (1 delivery, 20 units, ₹1,005.00), edit, search, deactivate (no new deliveries) |
| 6.4 | Billing / POS | PASS after fixes **B3, B4** | search, add, +/−, remove, out-of-stock disabled, new customer from the bill, 10 % discount (25 % refused), cash received 500 → change 67.10, saved once on double-click (`SC-2026-000001`, 481.00 − 48.10 = 432.90) |
| 6.4 | Critical billing rules | PASS | same request key → same bill, stock once; failing line → no stock change at all; out of stock → 409; discount > 20 %, received < total, unknown method → 400; client price ignored; no CSRF → 403; PUT/DELETE bill → 405; void twice → 409; cashier void → 403 |
| 6.4 | History, mark paid, void | PASS after fix **B5** | filters (search by mobile, pending), totals = SQL, pending UPI bill marked paid, void with reason (stock +1 via BILL_VOID, audit) |
| 6.5 | PDF invoice | PASS (text) | content checked: number, date, staff, customer, lines, prices, subtotal, discount, total, payment, status, received/change; VOIDED banner; no GST/tax/licence; only verified area/city/state. **Visual print preview NOT VERIFIED** (the in-app browser cannot display PDFs) |
| 6.6 | Customers | PASS | add (from bill), edit, search by email, purchase history total 502.90 = finalized bills |
| 6.7 | Expenses | PASS | add, 3-decimal and zero refused, future date refused, edit (before/after logged), cancel with reason (kept, not counted), category and date filters, 0.10 + 0.20 = 0.30 exactly, monthly summary |
| 6.8 | Reports | PASS | summary by day / month / year, billing, product sales (search, sort), stock movements, low stock, suppliers, expenses – all equal to SQL; JSON, CSV (UTF-8 BOM) and PDF for all 7; bad format / reversed dates → 400 |

## Responsive layout

19 admin pages × 5 widths (1440, 1366, 768, 390, 320 px): no horizontal page overflow and no text spilling out of its box after fixes **B6, B7**. Checked by script on every page plus screenshots of the POS, the bill form and the mobile bill bar.

## Bugs found and fixed

| # | Bug | Root cause | Fix | Test |
|---|---|---|---|---|
| B1 | Single-product stock row had no "last updated / by" | `stockOf` / `setThreshold` built the row without the latest movement | load latest movement for single rows | `BusinessApiIT.alerts` |
| B2 | **Old session token still accepted after logout** | logout only deleted the cookie (stateless JWT) | logout increments the account's token version (ends this account's sessions on all devices, like a password change) | `AuthApiIT.logout` |
| B3 | **POS product list failed ("Validation failed")** | POS sent `sort=name`; the API accepts `name,asc` | send `name,asc` | `verificationFixes.test.jsx` |
| B4 | **New customer from the bill was not saved/attached** | dialog form rendered inside the bill form; its submit also reached the bill form | dialogs render on `<body>` (portal) and stop submit events | `verificationFixes.test.jsx` (fails without the fix) |
| B5 | "Mark as paid" defaulted to Cash for a UPI bill | default was the first active method | default to the bill's own method | `verificationFixes.test.jsx` |
| B6 | Dashboard overflowed on phones (390 / 320 px) | grid items without `min-w-0`; 2-column stat cards too narrow for amounts | `min-w-0`, 1 column below 400 px, smaller figures, wrapping | browser re-check |
| B7 | Long badge overflowed at 320 px | badges never wrapped | wrap below `sm` | browser re-check |
| B8 | After logout, the next person was sent to the previous person's last page (cashier → Staff "not available") | login returned to the saved path regardless of role | `startPageFor(role, path)` | `verificationFixes.test.jsx` |
| B9 | "1 products", "1 items", "1 bills" | fixed plural | `plural()` helper | `verificationFixes.test.jsx` |
| B10 | Page titles differed from the sidebar (Stock / Stock history / Website Content / Staff / Activity log / Bill history) | old names | titles and breadcrumbs match the sidebar | – |
| B11 | Product-sales "Revenue" (632.00) looked different from billing revenue (578.40) | product lines are before bill discounts | column renamed "Sales before discount" + note; CSV/PDF header too | – |
| B12 | Activity log filter had no Bills / Payment methods; expense amounts like "₹0.2" | missing options; unscaled amount | options added; two decimals | `BusinessApiIT.expenses` |
| B13 | POS on phones: total hidden below a long product list | layout | sticky "N items · ₹total · View bill" bar | `verificationFixes.test.jsx` |
| – | Supplier details had no dates | – | "Added" / "Last changed" shown | – |

## Re-check after the fixes (rebuilt backend)

| Check | Result |
|---|---|
| Backend `.\scriptsun-integration-tests.ps1` | **185 / 185** – 58 unit/web + 127 MySQL (BUILD SUCCESS) |
| Frontend `npm test` | **65 / 65** (new `verificationFixes.test.jsx` 8; the dialog test fails when the fix is removed) |
| `npm run lint` (0 warnings) / `npm run build` | clean |
| Logout on the rebuilt backend | old session token → **401** (was 200) |
| Single stock row | `lastMovementAt` and `lastMovementBy` filled |
| Login redirect | owner returns to the requested Staff page; cashier lands on Billing / POS |
| Mobile menu (narrow pane) | opens, scrolls, logs out |
| Browser console | no JavaScript errors or warnings; only the 401/403 responses caused on purpose by the permission tests |
| Secrets scan | no `.env` values, test passwords or JWT secrets in committable files; `.env`, `uploads/`, `target/`, `dist/` ignored |

## Clean-up

All test accounts (owner, manager, cashier, website customer) and test records were removed from the test database (0 left); the integration-test run also resets it. The throwaway credentials file and test uploads were deleted. Development database `siva_crackers`: unchanged (0 staff accounts, 48 demo products, schema V4).

## Not verified

- Waiting for a session to expire (60 minutes) – covered by automated tests only.
- Visual print preview of the PDF invoice and report PDFs (the in-app browser downloads PDFs instead of showing them); the PDF text was checked.
- The development database: not used, by your choice.
