# Phase 6 – Inventory and business management: notes

**Status:** implemented and tested (backend, frontend components, development database); **not committed** (awaiting approval).
In-store billing, PDF invoices and customer purchase history were **not** built – their scope has not been approved (decision of 26 Nov 2026: "Not now").
API details: [API.md](API.md) §6–§8.

## Decisions (26 Nov 2026)

| Question | Decision |
|---|---|
| Role permissions (Phase 0 table vs. Phase 5 owner-only) | **Phase 0 table**: OWNER everything; MANAGER catalogue, stock, suppliers, expenses, shop customers, reports, dashboard (no staff, settings, homepage slides, activity log); CASHIER views stock and looks up customers |
| In-store billing / POS, billing PDF invoices, sales figures | **Not now** – nothing built; reports show no sales, revenue or profit |
| Shop customer records | **Yes, contacts only** (no purchase history) |

## Database – migration `V4__inventory_and_business.sql` (no existing table changed)

| Table | Purpose and rules |
|---|---|
| `stock_movements` | Every stock change: product (kept as name + code if the product is deleted later), type `STOCK_IN`/`STOCK_OUT`/`ADJUSTMENT`, reason, previous / change / new quantity, supplier, unit cost (stock in), note, staff member, time. Database checks: `new = previous + change`, never negative, direction matches type |
| `suppliers` | Name (unique, any case), contact person, phone (10 digits), email, address, notes, active. Never deleted |
| `expense_categories` | 7 labels created by the migration (Shop rent, Electricity, Transport, Packaging, Staff salary, Maintenance, Other); more can be added; deactivated instead of deleted |
| `expenses` | Date, category, amount (> 0, `DECIMAL(12,2)`), description, paid to, reference, notes, status `ACTIVE`/`CANCELLED` with reason and time, recorded by. Never deleted |
| `shop_customers` | Name, phone (unique) and/or email (one required), address, notes, created by. Staff-only |
| `audit_log` | Activity log: time, staff member, action, record, short details (never passwords) |

Applied to the test database (every test run) and to the **development database `siva_crackers`** on 26 Nov 2026 ("Successfully applied 1 migration … now at version v4"; existing demo data reused, nothing changed).

## Features

- **Stock** (`/admin/inventory`): current stock of every product with search, category, availability and sort filters; low-stock and out-of-stock alert cards and lists; per product: record stock in (with supplier and optional unit cost), stock out (sold at the counter, damaged, returned to supplier, expired, other) or adjustment to a counted quantity, with a live "after saving" preview; change the product's low-stock ("few left") level. Cashiers see everything read-only.
- **Stock history** (`/admin/stock-history`): every movement with before → after, reason, supplier, cost, note, who and when; filters by date range, type, product and supplier.
- **Consistency**: the product row is locked during a change (test: 20 simultaneous sales of 1 unit against 5 in stock → exactly 5 succeed, stock ends at 0); stock can never go below 0 (service and database); stock typed in the product form or quick update is recorded too (opening stock / product form change), so the history always adds up.
- **Suppliers** (`/admin/suppliers`): add, edit, view details with purchase totals (deliveries, units, recorded purchase value, last delivery), purchase history (stock history filtered by supplier), activate/deactivate, search. No supplier data is invented – the list starts empty.
- **Expenses** (`/admin/expenses`): record, edit, cancel with a reason (never deleted; cancelled ones stay visible and are excluded from totals), categories (add / deactivate), filters (dates, category, status, search, sort), total of the current view, monthly summary per year. Every create/edit/cancel is in the activity log with before/after amounts.
- **Customers** (`/admin/customers`): contact records – add, edit, delete (e.g. on request), search by name, mobile or email. Cashiers can search only.
- **Reports** (`/admin/reports`): daily / monthly / yearly summary (units in / out / adjusted, number of changes, recorded purchase value, expenses), stock movements, low stock, suppliers, expenses by category – each with date range, totals and **CSV download** (Excel-friendly, formulas neutralised). "Print / PDF" uses the browser's print to PDF (no server-side PDF; billing-related PDFs not built).
- **Dashboard**: adds total and active staff, active suppliers and this month's expenses; low-stock and out-of-stock cards link to the Stock page.

## Test results (26 Nov 2026)

| Check | Result |
|---|---|
| Backend `.\scripts\run-integration-tests.ps1` | **175 / 175** – 58 unit/web + 117 MySQL. New: `BusinessApiIT` 11, `StaffApiIT` 6, `CsvTest` 1, `TestDatabaseGuardTest` 2; `SchemaMigrationIT` now expects V1–V4 and 16 tables |
| `BusinessApiIT` covers | full role table on every new endpoint (401 / 403 / 200), role checked before body validation; stock in/out/adjust with before/after and staff name; history sums to current stock; never negative; invalid reasons/supplier/cost rejected; **20 parallel stock-outs**; product form changes recorded; product delete keeps history and is logged; alerts and threshold; suppliers (duplicate, phone, email normalised, purchase totals, inactive blocks deliveries); expenses (future date, zero / 3-decimal amounts, edit logged before/after, cancel needs reason, cancelled excluded and not editable, never deleted, monthly, duplicate category); customers (cashier read-only, phone or email required, duplicate phone, delete, no public route); reports (daily totals equal SQL, Indian date, no revenue/profit fields, CSV headers + BOM + formula neutralised, all CSVs, >1 year by day refused, reversed dates refused, unknown format refused); dashboard figures equal SQL |
| Frontend `npm test` | **47 / 47** (new `roles.test.jsx` 3, `phase6.test.jsx` 10) |
| `npm run lint` / `npm run build` | clean |

## Not done / limitations

- In-store billing, billing invoices (PDF), sales figures and customer purchase history: **awaiting your scope decision**.
- Report PDF export is the browser's "Print / Save as PDF", not a generated PDF file.
- Logged-in admin pages were not checked in a real browser (see PHASE_5_NOTES "Not verified").
