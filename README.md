# SIVA Crackers – Product Catalogue & Business Management Platform

Informational product catalogue and (later) shop management system for **SIVA Crackers**, a family-owned crackers retail shop in Eachanari, Coimbatore, Tamil Nadu.

> **Legal notice:** the public website is an information catalogue only. It does not sell, book, accept payment for, or deliver firecrackers online. All purchases happen in person at the shop. WhatsApp is used for general enquiries only.

> **Demo data:** products, brands, prices and stock levels in this repository are demonstration samples, not the shop's real range or prices. They are marked `isDemo` and shown with a **DEMO** tag.

## Tech stack

| Part | Technology |
|---|---|
| Public website | React 19, Vite, Tailwind CSS 4, React Router, Lucide React, Axios |
| Admin dashboard | React (Phase 5) |
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

## Project structure

| Folder | Contents | Status |
|---|---|---|
| `docs/` | Phase plans, notes and screenshots | Phases 0–2 |
| `frontend/` | Public website | Phases 1–2 done |
| `admin/` | Admin dashboard | Phase 5 |
| `backend/` | REST API (Spring Boot + MySQL) | Phase 4 |

## Run the website

```powershell
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. Other scripts: `npm run lint`, `npm run build`, `npm run preview`.

The site currently runs on local demo data (`VITE_USE_MOCKS=true`, see `frontend/.env.example`). From Phase 4 it will call the Spring Boot API.

## Documentation

- [Phase 0 – Plan](docs/PHASE_0_PLAN.md) (features, architecture, database, API, roadmap)
- [Phase 1 – Notes](docs/PHASE_1_NOTES.md)
- [Phase 2 – Notes](docs/PHASE_2_NOTES.md)
