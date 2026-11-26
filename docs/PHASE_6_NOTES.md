# Phase 6 – Inventory, business management and in-store billing: notes

**Status (26 Nov 2026):** implemented and tested (backend, database, frontend); **not committed** (awaiting your approval).
**Not verified:** the logged-in admin in a real browser (see "Not verified" below).
API details: [API.md](API.md) §6–§8 and §7a (billing).

## Decisions

| Date | Question | Decision |
|---|---|---|
| 26 Nov 2026 | Role permissions | **Phase 0 table**: OWNER everything; MANAGER catalogue, stock, suppliers, expenses, shop customers, reports, dashboard; CASHIER billing, view stock, customer look-up |
| 26 Nov 2026 | In-store billing / POS | **Approved** – only inside the protected admin, for sales made in person at the shop. No payment gateway, no payment links, no card/UPI details collected |
| 26 Nov 2026 | Cashiers adding customers | **Yes, while billing** (POST only; edit/delete stay with OWNER/MANAGER) |
| 26 Nov 2026 | Who may void a bill | **OWNER and MANAGER** |
| 26 Nov 2026 | Discounts | **One bill-level discount** (amount or %), at most **20% of the subtotal**; unit prices are always the product's current price |
| 26 Nov 2026 | Profit | **Not calculated** – a reliable cost is not recorded for every sale. Revenue (finalized bills) and expenses are shown separately |

## Database

`V4__inventory_and_business.sql` (stock movements, suppliers, expense categories, expenses, shop customers, audit log) – unchanged.

`V5__in_store_billing.sql` (new; no existing data changed):

| Table | Purpose and rules |
|---|---|
| `payment_methods` | Code (unique, `^[A-Z][A-Z0-9_]*$`), label (unique), order, active. Created: Cash, UPI (paid at the shop), Card (paid at the shop), Other (in-store). Owner can add / rename / switch off |
| `bill_counters` | One row per year; gives bill numbers `SC-YYYY-NNNNNN` without gaps from races |
| `bills` | Bill number (unique), request key (unique – makes saving idempotent), status FINALIZED/VOIDED, customer (+ name/mobile snapshot), subtotal, discount, total, payment method, payment status PAID/PENDING, amount received, notes, created by, void time/by/reason. Database checks: total = subtotal − discount; discount ≤ 20% of subtotal; void fields consistent |
| `bill_items` | Line number, product (+ name, code, pack, unit snapshot), unit price, quantity > 0, line total = price × quantity |
| `stock_movements.bill_id` | Links every `BILL_SALE` / `BILL_VOID` stock movement to its bill |

Applied to the test database on every test run. The **development database** gets V5 automatically the next time the backend starts with `backend/.env` (not done in this session).

## Features

- **Billing / POS** (`/admin/billing`, all staff; cashiers start here): search active products (price and stock shown, out-of-stock can't be added), cart with − / + / typed quantity capped at stock, remove; subtotal, discount (none / ₹ / %, max 20%), total in exact paise; pick a customer, add a new one, or walk-in; payment method (active ones only), paid now / pending, amount received with change; notes. Saving is protected against double clicks and retries (same request key → the saved bill, never a second one). After saving: bill number, **print invoice**, **download PDF**, new bill.
- **Bill history** (`/admin/bills`, all staff): filters (dates, search by bill no./name/mobile, payment method, payment status, bill status), totals (sales from finalized bills, count, pending amount, voided count), table on desktop / cards on phones, pagination. Bill detail: lines, totals, payment, invoice print/PDF, **mark as paid** (pending bills), **void with a reason** (owner/manager – returns every item to stock, bill kept as VOIDED, no longer counted).
- **Stock is deducted exactly once**: product rows locked in id order for the whole transaction, one `BILL_SALE` movement per line; voiding writes `BILL_VOID` movements. These reasons can't be chosen by hand.
- **PDF invoice** (OpenPDF, A5): shop name and only the address/phone/email saved in Business Settings, bill number, date (India time), billed by, customer or "Walk-in customer", lines, subtotal/discount/total, payment, status, received/change, "VOIDED" banner. **No GST, tax, licence or invented address.**
- **Customers**: purchase history per customer (bills, total purchases from finalized bills, voided shown separately).
- **Reports** (`/admin/reports`): new **Billing** (revenue, discounts, pending, voided, by payment method) and **Product sales** (bills, units, revenue; search; sort by revenue or units) tabs; summary now has bills / revenue / discounts per day / month / year. Every report: date range, cards, tables, **CSV** and **PDF** download, print. The page states: revenue = finalized bills only, expenses separate, no profit.
- **Dashboard**: sales today / this month (finalized bills), total and active suppliers, recent bills, recent stock changes, recent expenses, "New bill" quick action.
- **Inventory**: price, **last updated** time and **staff member** per product. **Suppliers**: created date.
- **Business Settings** (owner): in-store payment methods (add, rename, reorder, switch off).
- **Activity log**: adds `PRODUCT_CREATED`, `PRODUCT_UPDATED`, `BILL_CREATED`, `BILL_PAID`, `BILL_VOIDED`, `PAYMENT_METHOD_CREATED/UPDATED` (staff, product, stock, bills, expenses and suppliers are all audited; stock changes also live in the stock history).
- Stock, suppliers, expenses, customer contacts and reports from the first part of Phase 6 are unchanged.

## Test results (26 Nov 2026)

| Check | Result |
|---|---|
| Backend `.\scripts\run-integration-tests.ps1` | **185 / 185** – 58 unit/web + 127 MySQL (new `BillingApiIT` 10; `SchemaMigrationIT` expects V1–V5 and 20 tables) |
| `BillingApiIT` covers | role table (cashier creates, can't void or manage methods; customers/visitors refused); bill with repeated lines merged, totals 466.50 − 46.65 = 419.85, change 80.15, stock and movements; validation (discount > 20%, received < total, inactive method/product, unknown product); idempotent replay; **10 parallel bills against 3 in stock → 3 saved, 7 refused (409), unique numbers, stock 0**; void restores stock once and is refused twice; pending → paid; **invoice PDF text** (number, Rs. amounts, Cash, Paid, no GST/tax); customer purchase history; billing/product-sales reports and dashboard equal SQL, voided excluded |
| Frontend `npm test` | **57 / 57** (new `billing.test.jsx` 10: paise arithmetic and 20% cap; POS stock caps, active-only products and methods, one save on double click, request key reused on retry, stock conflict message, cashier adds customer; bill history totals, cashier can't void, owner void needs reason, filters; billing report revenue/no profit with CSV + PDF links) |
| `npm run lint` / `npm run build` | clean |
| Owner bootstrap (test database) | verified: started with no OWNER → "OWNER account created from the BOOTSTRAP_OWNER_* settings"; throwaway account deleted afterwards (0 rows left), its password file removed, clipboard cleared; password never printed |

## Not verified

- **Logged-in admin in a real browser** – the 15 workflows (POS, bill history, void, invoice print/PDF, customers, reports, dashboard, inventory, suppliers, settings, staff, roles) and layouts at desktop, tablet and mobile widths. **NOT VERIFIED**: the assistant was not allowed to read the generated test password, the browser pane ignored a simulated paste, and the manual login did not reach the test backend; you chose to skip. The behaviour is covered by the API and component tests above, but nothing was checked by eye.
- V5 on the development database (applies at the next backend start).

## Known limitations

- Bills can't be edited after saving (void and re-bill instead); partial returns aren't supported.
- One discount per bill; no per-line discounts.
- Invoice PDF uses the built-in Helvetica font: amounts show as "Rs." (no ₹ glyph) and Tamil text is not supported.
