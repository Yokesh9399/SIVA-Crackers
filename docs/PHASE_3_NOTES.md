# Phase 3 – Product details and business pages

**Status:** Complete and audited.

## What was built

| Requirement | Implementation |
|---|---|
| **Product detail page** `/products/:slug` | Breadcrumbs (Home › All Products › Category › Product); gallery with large image, 3 thumbnails, prev/next arrows and a full-screen viewer (arrow keys, Escape); brand and category links; DEMO tag; pack size, unit and product code; indicative price with struck-through MRP and "₹X below MRP – indicative"; price disclaimer; stock status with "(demo stock level, for testing only)"; description; **Enquire on WhatsApp** (general enquiry only, disabled until the shop number is set); "Add to estimate list"; "not sold online – visit the shop" note; category-specific **safety information**; product details table; 4 **related products** (same category first, then same brand); unknown slug → 404 |
| **Price List** `/price-list` | All 48 products in 12 category tables (display order) with running No. 1–48, Brand, Pack, MRP and indicative Price (+ % off); instant search across product/brand/category with result count, clear button and empty state; category jump links; price disclaimer at top and bottom; demo-data note; **Print price list** button |
| **Print layout** | Site header, footer, notices, search and buttons hidden; a print header shows shop name, known location, title, print date, price disclaimer, "not an invoice or order" and the demo-data line; Brand and Pack columns always shown; rows never split across pages; plain black-on-white |
| **My Estimate List** `/estimate` | Kept exactly as agreed in Phase 0: browser-only (localStorage, stores only `{slug, qty}`), never sent anywhere, printable, clearly labelled "not an order, booking or reservation". Add from the product page with a quantity stepper; edit quantities, remove items, two-step "Clear list", indicative total and MRP total; card layout on phones, table on larger screens and in print; header badge shows the number of items; unknown/old entries and bad data in storage are ignored safely |
| **About Us** `/about` | Introduction from settings, Eachanari, Coimbatore location (street address "To be updated"), 4 business highlights, "plan your visit" steps, original brand identity (logo mark and colour palette), legal note. No invented history, years or claims |
| **Contact Us** `/contact` | Reuses the homepage contact block: address, phone, email and shop-hours placeholders, general WhatsApp enquiry option, area map labelled "exact shop location to be updated"; general-enquiry rules (information only, no orders); visit steps |
| **Safety Guidelines** `/safety` | 6 sections (before you begin, adult supervision, safe lighting practices, after lighting/misfires, clothing, care for others and the law), emergency precautions (burns, eye injuries, clothes on fire, fire) with tap-to-call 112 / 101 / 108, legal disclaimer, on-page index |
| **Legal pages** `/legal/disclaimer`, `/privacy`, `/terms` | Built because the footer already links to them. Draft text written for an information-only catalogue, marked "to be reviewed by SIVA Crackers and a legal adviser". Unknown slug → 404 |

## Decisions to note

1. **Login / Signup moved to Phase 4.** The Phase 0 plan listed a mock-only login UI in Phase 3, but your Phase 3 brief did not include it. Building a fake login now would be thrown away, so `/login` and `/register` show "coming in Phase 4" and will be built against the real Spring Boot JWT API. The links keep working.
2. **Product images.** Products have no photos yet, so the gallery shows three original illustration views (standard, close-up, night sky) with a note that photos will be added. Real photos (Phase 5 admin uploads) replace them automatically.
3. **Safety notes are per category** (added to `categories.json`), so all 48 products get relevant notes without duplicating text on every product.
4. **New API endpoints for Phase 4** (already called by the service layer): `GET /api/public/products/{slug}`, `/products/{slug}/related`, `/products/by-slugs?slugs=`, `/price-list`. API errors now carry their HTTP status so a 404 shows the "not found" page.

## Files

### New (26)
| File | Purpose |
|---|---|
| `src/pages/ProductDetailPage.jsx` | Product detail |
| `src/pages/PriceListPage.jsx` | Price list + print |
| `src/pages/EstimatePage.jsx` | My Estimate List |
| `src/pages/AboutPage.jsx`, `ContactPage.jsx`, `SafetyPage.jsx`, `LegalPage.jsx` | Business pages |
| `src/components/product/ProductGallery.jsx` | Gallery, thumbnails, full-screen viewer |
| `src/components/estimate/AddToEstimate.jsx` | Quantity + "Add to estimate list" |
| `src/components/business/VisitSteps.jsx` | "Plan your visit" steps (About, Contact) |
| `src/components/ui/Breadcrumbs.jsx` | Reusable breadcrumbs (light/dark) |
| `src/components/ui/PrintHeader.jsx` | Header shown only on paper |
| `src/components/ui/QuantityStepper.jsx` | − [n] + control |
| `src/context/EstimateContext.jsx`, `estimate-context.js`, `src/hooks/useEstimate.js` | Estimate list state + localStorage |
| `src/utils/storage.js` | Safe localStorage helpers |
| `src/content/safetyGuidelines.js`, `src/content/legalPages.js` | Page text kept out of components |
| `docs/PHASE_3_NOTES.md`, `docs/screenshots/phase3/*.jpg` (12) | Notes and screenshots |

### Changed
| File | Change |
|---|---|
| `src/mocks/data/categories.json` | `safetyNotes` per category |
| `src/mocks/data/products.json`, `settings.json` | Three unsupported phrases neutralised (audit) |
| `src/content/safetyGuidelines.js`, `src/pages/SafetyPage.jsx` | Emergency "general guidance, not medical advice" note (audit) |
| `src/mocks/mockApi.js` | `getProductBySlug` (404 for unknown), `getRelatedProducts`, `getProductsBySlugs`, `getPriceList` |
| `src/services/productService.js` | `getBySlug`, `getRelated`, `getBySlugs`, `getPriceList` |
| `src/services/apiClient.js` | Errors keep the HTTP status |
| `src/routes/AppRoutes.jsx` | 7 real routes; only Login/Register remain placeholders (Phase 4) |
| `src/App.jsx` | `EstimateProvider` |
| `src/components/layout/Header.jsx` | Estimate item-count badge; hidden when printing |
| `src/layouts/MainLayout.jsx` | Announcement, notices and footer hidden when printing |
| `src/components/ui/PageHeader.jsx` | Uses `Breadcrumbs`; hidden when printing |
| `src/components/product/ProductArt.jsx` | `variant` prop for the gallery views |
| `src/utils/format.js` | `formatUnit`, `formatDate` |
| `src/index.css` | Print page margins and white background |
| `README.md` | Phase 3 features |

## Testing – final audit (23 Nov 2026)

All results below come from **one consolidated re-run** after the final changes, so every count is exact. Automated browser scripts drove the real UI (typing, clicking, selects, keyboard) at 1440 px (desktop suite) and 375 px (mobile suite).

### Estimate List count reconciliation (14 vs 17)
The earlier draft said "14 + 3 pass" and the chat summary said "17". What actually ran before this audit:
- a dedicated Estimate List script with **14 formal checks** (all passed);
- **3 phone-card values** (₹1,465 → ₹2,140 → ₹1,870) that were measured and correct but were not written as formal pass/fail checks;
- an earlier product-page script that included **3 estimate checks**; one of them ("add qty 3") **failed** because of the fast-tap bug, which was then fixed and re-tested inside the 14.

So "17" mixed formal checks with informal measurements. The final suite replaces all of this with formal checks only: **Estimate List = 15 desktop checks (B14–B28) + 3 phone checks (M9–M11) = 18 checks, 18 passed.**

### Desktop suite – 72 / 72 passed

| Group | Passed | Checks |
|---|---|---|
| A. Product detail | 13 / 13 | A1 all **48** product routes (name, tab title, 4-level breadcrumb, safety, code, demo stock label, not-sold-online note) · A2 invalid slug → 404 · A3 4 related, excluding current · A4 related link opens product · A5 starts at top · A6 gallery resets · A7 next arrow · A8 viewer opens on current image · A9 viewer ArrowRight · A10 Escape closes + unlocks scroll · A11 breadcrumb links · A12 WhatsApp general-enquiry button (disabled until number set) · A13 no cart/buy/checkout/pay/order buttons |
| B. Estimate List | 15 / 15 | B14 corrupt/unknown stored entries ignored, qty capped at 999 · B15 "not an order, booking or reservation" notice · B16 clear asks to confirm · B17 confirmed clear empties list and storage · B18 two instant + taps = qty 3 · B19 add confirmation · B20 picker resets to 1 · B21 adding again accumulates · B22 header badge · B23 total ₹2,475 · B24 out-of-stock label kept · B25 qty change → ₹3,150 · B26 remove → ₹1,350 · B27 stored only as `{slug, qty}` · B28 no API/network request |
| C. Price List | 13 / 13 | C29 48 products / 12 tables in order · C30 serial 1–48 · C31 MRP & Price columns · C32 disclaimer top and bottom · C33 demo note · C34 12 jump links resolve · C35 search "rocket" → 4 · C36 brand "aruvi" → 7 · C37 category "gift" → 4 · C38 empty state · C39 clear restores 48 · C40 only Print / Clear-search buttons · C41 name links to detail |
| D. Print layout | 4 / 4 | D42 site chrome hidden · D43 toolbar/jump links/buttons hidden · D44 print header (shop, date, disclaimer, demo line) · D45 Brand and Pack columns shown. Also printed to PDF with headless Microsoft Edge: 4 pages |
| E. Business pages | 15 / 15 | E46–E48 About (intro, location + placeholder, 4 highlights, visit steps, colours, legal note, **no unsupported claims**) · E49–E51 Contact (4 placeholders, map labelled approximate, enquiry + no-orders notice) · E52–E55 Safety (8 sections, 112/101/108 links, "not medical advice" note, index anchors) · E56–E58 draft-review banner on all 3 legal pages · E59 unknown legal page → 404 · E60 Login says Phase 4 |
| F. Phase 1–2 regression | 11 / 11 | F61 home slides + 16 DEMO cards · F62 categories, brands, notices · F63 neutral "Buy in person" wording · F64 catalogue 48 / 12 per page · F65 instant search · F66 category + brand + price from URL · F67 price sort · F68 page 4, Next disabled · F69 category page · F70 8 unique brands · F71 unknown brand → 404 |
| G. Internal links | 1 / 1 | G72 all **82** unique internal links collected from 13 pages open without a 404 |

### Mobile suite (375 px) – 11 / 11 passed
M1 no horizontal overflow on **20 routes** · M2 menu opens full height · M3 menu closes with Escape · M4 filter sheet opens and locks scroll · M5 sheet closes and unlocks · M6 "Add to estimate list" on one line · M7 price-list toolbar not sticky on phones · M8 Brand/Pack shown under product name · M9 estimate cards (table hidden), total ₹1,465 · M10 + → ₹2,140 · M11 remove → ₹1,870 and storage updated

### Other checks
| Check | Result |
|---|---|
| Browser console | Fresh reload + 20 routes visited with `console.error`, uncaught-error and unhandled-rejection listeners: **0 errors**. The dev-tools log still lists older entries from mid-edit hot reloads earlier in the session (e.g. `estimateCount is not defined` between two sequential edits); their URLs carry old hot-reload timestamps and the current code was confirmed correct |
| Online ordering audit | Code search for cart / checkout / payment / order / booking / reservation wording, payment SDKs (Razorpay, Stripe, Paytm, PhonePe, UPI), `POST`/`PUT`/`PATCH`/`fetch`/`sendBeacon` and form actions: **none**. Every "order/booking/reservation" mention is a negation ("not an order…") or "sort order" |
| `npm run lint` | No errors or warnings |
| `npm run build` | Success (main JS 112 kB gzip; each Phase 3 page is its own 2–4 kB chunk) |

## Content and legal audit

| Item | Finding | Action |
|---|---|---|
| Invented business details | None. Only the shop name, family-owned status and Eachanari, Coimbatore location (from the brief) are stated; phone, WhatsApp, email, hours, street address and licence remain "To be updated" | – |
| Fake reviews, ratings, awards, certifications, years in business, guarantees | None found (code search) | – |
| Unsupported claims | **3 found and fixed:** "as required by law" (Buy-in-person highlight) → "All purchases are made in person at the shop counter."; demo descriptions "Our biggest flower pot" and "Our longest garland" → neutral wording | Fixed |
| Misleading legal approvals | None. No text says the website, WhatsApp or the shop is legally approved, licensed or cleared. "Buy only from licensed shops" is general safety advice, not a claim about SIVA Crackers | – |
| Legal pages | All three show an amber banner: "Draft text – to be reviewed by SIVA Crackers and a legal adviser before publication." | Confirmed |
| Map | Area map only, with the overlay "Area map · exact shop location to be updated"; the Google pin marks the Eachanari locality, not a shop | Confirmed |
| Safety presentation | General guidance with a legal disclaimer; added a note to the emergency section: "These first-aid steps are general guidance, not medical advice. For any serious injury, call 108 or 112 immediately and get medical help." | Added |

### Items that need official verification before going live
1. **Emergency numbers** – 112 (national emergency), 101 (fire), 108 (ambulance) are India's public helplines; confirm they are current for Coimbatore/Tamil Nadu.
2. **First-aid steps** (burns, eye injuries, clothes on fire, fire) – general guidance; should be reviewed against an official or medical source (e.g. a first-aid body or local health authority).
3. **Specific distances and times** in safety text (e.g. "at least 5 metres", "at least 10 metres" for aerial shots, "wait 5 minutes" for misfires) – common guidance, not taken from an official standard; check against manufacturers' instructions and any PESO/local-authority guidance.
4. **Local rules** – the site tells visitors to follow timings and rules set by local authorities but does not state them; add official timings only if the owner wants to and from an official source.
5. **Legal pages** – all three drafts need review by the owner and a legal adviser.
6. **Online sale position** – the site deliberately avoids any online sale, booking or WhatsApp ordering; WhatsApp enquiries remain "subject to legal verification" as agreed.

### Issues found and fixed during testing
1. **Quantity stepper lost fast taps** – two taps before React re-rendered counted as one. The stepper now tracks the latest value, so every tap counts.
2. **"Add to estimate list" wrapped onto two lines on phones** – the button now takes its own full-width line.
3. **Price list toolbar covered too much of a phone screen** – now sticky only on tablets and larger.
4. **Estimate list rows were very tall on phones** (narrow table column) – phones now get a compact card layout; the table stays for larger screens and printing.
5. **Estimate total row misaligned** when the Price column is hidden – one cell per column now.
6. **Unsupported claims in content** (final audit) – three phrases neutralised (see Content and legal audit).
7. **Emergency first-aid context** (final audit) – added the "general guidance, not medical advice" note.

## Screenshots (`docs/screenshots/phase3/`)

Desktop: `desktop-product-detail.jpg`, `desktop-price-list.jpg`, `desktop-estimate-list.jpg`, `desktop-about.jpg`, `desktop-contact.jpg`, `desktop-safety.jpg`
Mobile: `mobile-product-detail.jpg`, `mobile-product-detail-estimate.jpg`, `mobile-price-list.jpg`, `mobile-estimate-list.jpg`, `mobile-safety.jpg`
Print: `print-price-list.jpg`

## Not in this phase
Customer login/registration/profile (Phase 4, with the real API). Spring Boot backend and MySQL (Phase 4). Admin app (Phase 5).
