# Phase 2 – Product catalogue and filtering

**Status:** Complete, awaiting approval. Phase 3 not started.

## What was built

| Requirement | Implementation |
|---|---|
| All Products page | `/products` with navy page header, breadcrumbs and the shared `CatalogueView` |
| Product card fields | Image (original illustration), name, brand, category, pack size, indicative price, struck-through MRP, "% off MRP" badge (only when MRP > price), stock status, View details, Enquire (WhatsApp, general enquiry only), **DEMO** tag |
| Search | Matches product name, brand name **and** category name; multi-word (every word must match); instant (350 ms after typing stops) or immediate on Enter; clear (×) button; result count "Showing 1–12 of 48 products for 'rocket'"; empty-results message with a "Clear all filters" action |
| Category filtering | Checkbox list of all 12 categories with product counts (multi-select = OR). Dedicated pages `/categories` and `/categories/:slug` |
| Brand filtering | Brand chips (multi-select = OR) with counts. Dedicated pages `/brands` (each brand listed once) and `/brands/:slug` |
| Price filtering | Minimum / maximum inputs with Apply and Clear, validation (positive numbers, min ≤ max), quick presets (Under ₹200, ₹200–₹500, ₹500–₹1,000, Above ₹1,000), catalogue range hint |
| Extra filter | "Hide out-of-stock items" switch (planned in Phase 0) |
| Combined filters | Different filter types combine with AND; each removable as a chip; "Clear all" keeps the chosen sort and page size |
| Sorting | Default (by category order), Name A–Z, Name Z–A, Price low–high, Price high–low, Newest first |
| Pagination | Page numbers with "…" for long ranges, Previous / Next (disabled at the ends), 12 / 24 / 48 per page, "Showing X–Y of Z", scrolls back to the top of the results |
| Desktop layout | Sticky filter sidebar + toolbar (search, sort) + 3–4 column grid |
| Mobile layout | Compact search, Filters button with active-count badge, bottom-sheet filters ("Show N products"), sort dropdown, 2-column cards, no horizontal overflow |
| URL state | Every filter is in the query string, e.g. `/products?search=flower&category=flower-pots&min=200&max=400&stock=in&sort=name,desc&size=24&page=2`. Shareable, survives reload, browser Back/Forward work. Invalid values fall back to defaults |
| Category / brand pages | Catalogue locked to that category/brand (its own filter hidden, other filters still work), header with description and a quick switcher to other categories/brands, unknown slug → 404 |
| Product detail link | Every card links to `/products/:slug` (Phase 3 page, currently "coming soon") |
| Legal | No cart, checkout, payment or ordering. Price disclaimer, information-only notice and DEMO note under every listing. WhatsApp stays general enquiry only |

## Files

### New
| File | Purpose |
|---|---|
| `src/hooks/useCatalogueFilters.js` | Reads/validates filters from the URL, updates them (resetting to page 1), clear all, builds API parameters; sort options and page sizes |
| `src/hooks/useDocumentTitle.js` | Browser tab titles |
| `src/components/catalogue/CatalogueView.jsx` | The complete catalogue (used by 3 pages) |
| `src/components/catalogue/CatalogueSearch.jsx` | Instant search box with clear button |
| `src/components/catalogue/FilterPanel.jsx` | Categories, brands, price, availability |
| `src/components/catalogue/PriceFilter.jsx` | Min/max with Apply/Clear, validation, presets |
| `src/components/catalogue/ActiveFilterChips.jsx` | Removable chips + Clear all |
| `src/components/catalogue/SortControls.jsx` | Sort and per-page selects |
| `src/components/catalogue/FilterSheet.jsx` | Mobile bottom sheet (portal, Escape to close, scroll lock) |
| `src/components/catalogue/ScopeSwitcher.jsx` | Category/brand quick switcher on scoped pages |
| `src/components/ui/Pagination.jsx` | Page numbers, Prev/Next |
| `src/components/ui/EmptyState.jsx` | No-results message |
| `src/components/ui/PageHeader.jsx` | Navy page banner with breadcrumbs (reusable for Phase 3 pages) |
| `src/utils/pagination.js` | Page-number list with gaps, "X–Y of Z" calculation |
| `src/pages/ProductsPage.jsx`, `CategoriesPage.jsx`, `CategoryProductsPage.jsx`, `BrandsPage.jsx`, `BrandProductsPage.jsx` | The five catalogue routes |
| `docs/screenshots/phase2/*.jpg` | Desktop and mobile screenshots |

### Changed (no visual change to the homepage)
| File | Change |
|---|---|
| `src/mocks/mockApi.js` | Search across name/brand/category, multi-word; filters by category/brand **slugs**; default sort; `getPriceRange()` |
| `src/services/productService.js` | `getPriceRange()`; `toQuery` drops empty/false values |
| `src/routes/AppRoutes.jsx` | Five real routes replace "coming soon" placeholders |
| `src/components/home/CategoryGrid.jsx` | Optional `showHeading` prop so the Categories page can reuse the grid |
| `src/components/product/ProductCard.jsx` | Stock badge wraps below the brand on narrow cards; compact buttons on phones |

## Decision to note for Phase 4

The API filter parameters are now **slugs** instead of numeric ids: `GET /api/public/products?categories=rockets,aerial-shots&brands=minnal-pyro&minPrice=&maxPrice=&inStock=&sort=&page=&size=`. Slugs give readable, shareable URLs and the backend can resolve them with one indexed lookup. Section 9.1 of the Phase 0 plan used `categoryIds`/`brandIds`; the Spring Boot controller will follow this slug version.

## Testing

Automated browser scripts drove the real controls (typing, clicking, selects) at 1440 px and 375 px.

| Area | Checks | Result |
|---|---|---|
| Search | name ("rocket" → 4), brand ("minnal" → 4), category ("aerial" → 4), multi-word ("gold sparklers" → 1), clear button, URL updated | Pass |
| Empty results | "xyzzz" → message + "0 products found" | Pass |
| Category filter | Sparklers + Rockets → 8 | Pass |
| Brand filter | + Minnal Pyro → Baby Rocket, Colour Rocket | Pass |
| Price filter | + ₹200–₹300 → Colour Rocket; min > max → validation message; preset "Above ₹1,000" → 6, all ≥ ₹1,000 | Pass |
| Availability | Hide out-of-stock → 45 (3 demo items are out of stock) | Pass |
| Chips | 4 chips shown, remove one, Clear all → 48 and clean URL | Pass |
| Sorting | A–Z, Z–A, price ↑ (first ₹30), price ↓ (first ₹3,000), default (category order) | Pass |
| Pagination | Page 2 (13–24), Next (25–36), Prev, page 1 Prev disabled, page 4 Next disabled, 24 per page → 2 pages, browser Back | Pass |
| URL restore | Full query string restores search box, chips, price inputs, sort, page size and results | Pass |
| Bad input | `page=99` → "This page has no products" + first-page button; bogus sort/size/page/min → defaults | Pass |
| Category page | `/categories/rockets` → 4 rockets, category filter hidden, brand filter shown, 12 switcher links, tab title | Pass |
| Brand page | `/brands/aruvi-fireworks` → 7 Aruvi products, brand filter hidden | Pass |
| Unknown slugs | `/categories/not-a-real-category`, `/brands/nope` → 404 | Pass |
| Listings | `/categories` 12 tiles; `/brands` 8 unique brands, 8 DEMO tags | Pass |
| Legal | No cart/buy/checkout text; disclaimer present; enquiry buttons on every card | Pass |
| Mobile | Width exactly 375 px on catalogue, category page and homepage; filter sheet opens, filters live, "Show 4 products", locks/unlocks page scroll, closes | Pass |
| Homepage regression | 3 slides, 16 cards, category links, no overflow | Pass |
| `npm run lint` / `npm run build` | No errors or warnings | Pass |

### Issues found and fixed during testing
1. The results-loading marker (`aria-busy`) was only on the product grid, so screen readers and tests got no loading signal for empty/error states. Moved to the always-present results wrapper.
2. On 2-column phone cards the brand name was cut off and "View details" wrapped. The stock badge now wraps under the brand and buttons are compact on phones.
3. The mobile sheet's drag handle was off-centre. Centred.

Some early test runs reported false failures because the test script read the page before React Router had applied the URL change; the final checks wait for the list to change and all pass.

## Final pre-commit checks (23 Nov 2026)

| Check | Result |
|---|---|
| Code review of all modified and new files | Clean. Two internal helpers (`SORTERS`, `toQuery`) were exported without need and are now private. No debug code, `console.log`, TODO or test helpers in `src/` |
| `.gitignore` | `frontend/node_modules/`, `frontend/dist/` and `frontend/.env` confirmed ignored. Only source, docs and 4 screenshots (≈170 KB) are committed |
| Secrets scan | No keys, tokens or passwords in committed files (matches were only planning text and the runtime code that reads a login token) |
| Phase 1 regression | Announcement bar, demo notice, 7 nav links, carousel next, 5 quick tiles, 12 category tiles, 16 product cards with DEMO tags, 8 brands for screen readers, WhatsApp buttons disabled until a number is set, safety tips, legal notice, footer placeholders, map, no console errors; mobile width 375 px, menu full height with 8 brands, closes with Escape | Pass |
| Catalogue smoke test after tidy-up | `/products?search=rocket&sort=price,desc` → 4 products, Whistling Rocket first | Pass |
| `npm run lint` | No errors or warnings | Pass |
| `npm run build` | Success (main JS 110 kB gzip, catalogue chunk 7.3 kB gzip, CSS 11 kB gzip) | Pass |

## Screenshots

- `docs/screenshots/phase2/desktop-all-products.jpg`
- `docs/screenshots/phase2/desktop-filters-applied.jpg`
- `docs/screenshots/phase2/mobile-product-list.jpg`
- `docs/screenshots/phase2/mobile-filter-sheet.jpg`

## Not in this phase
Product detail page, Price List, business pages, accounts and Estimate List (Phase 3). Backend integration (Phase 4).
