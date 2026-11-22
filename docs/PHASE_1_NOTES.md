# Phase 1 – React setup and homepage UI

**Status:** Complete, awaiting approval. Phase 2 not started.

## How to run

```powershell
cd frontend
npm install        # first time only
npm run dev        # http://localhost:5173
```

Other scripts: `npm run lint`, `npm run build` (production build into `frontend/dist`), `npm run preview`.

## Stack versions installed

React 19.3, React Router 7.18, Vite 8.3, Tailwind CSS 4.3 (via `@tailwindcss/vite`), Lucide React 1.77, Axios 1.21, ESLint 10.

## Decisions applied from Phase 0 approval

| Decision | How it is implemented |
|---|---|
| WhatsApp general enquiries | `Enquire` button on every product card, a quick tile, and a "Send a general enquiry" button in the contact block. Product detail pages get the same button in Phase 3. |
| No WhatsApp ordering | The message contains product name, brand, category, pack size and indicative price only, and ends with "General enquiry only. Not an order." No quantities, totals or delivery wording. Nothing is sent automatically; WhatsApp opens and the customer chooses to send. |
| Legal verification switch | `whatsappEnquiryEnabled` in settings hides every WhatsApp button at once if needed. |
| Placeholders | `whatsappNumber`, `phone`, `email`, hours and street address are `null`. The UI shows "To be updated" and the WhatsApp/Call buttons stay disabled until real numbers are entered. |
| Estimate List | Header icon and menu entry link to `/estimate`. The feature itself is built in Phase 3 as planned. |
| Separate admin app | Not started (Phase 5). `frontend/` contains only the public site. |

## Files created

### Project configuration (`frontend/`)
| File | Purpose |
|---|---|
| `package.json` | Dependencies and npm scripts |
| `vite.config.js` | Vite with the React and Tailwind plugins, dev server on port 5173 |
| `eslint.config.js` | Lint rules for React hooks and fast refresh |
| `index.html` | Page shell, SEO description, Google Fonts (Poppins, Inter) |
| `.env`, `.env.example` | `VITE_API_URL` and `VITE_USE_MOCKS` (true until Phase 4) |
| `.gitignore` | Excludes `node_modules`, `dist`, `.env` |
| `public/favicon.svg` | Original starburst icon |
| `../.claude/launch.json` | Lets Claude start the dev server in the preview pane |

### Application entry
| File | Purpose |
|---|---|
| `src/main.jsx` | Mounts React |
| `src/App.jsx` | Router plus `SettingsProvider` |
| `src/index.css` | Tailwind import, **brand tokens** (navy, gold, orange, cream, fonts, animations), button styles, reduced-motion support |
| `src/config/env.js` | Reads environment variables in one place |
| `src/routes/AppRoutes.jsx` | All routes. Home is real; later-phase pages use a "coming soon" page so no link is broken; unknown URLs show 404 |
| `src/layouts/MainLayout.jsx` | Announcement bar, header, page, legal notice, footer; skip-to-content link; scroll to top on navigation |

### Data layer (no data hard-coded in components)
| File | Purpose |
|---|---|
| `src/mocks/data/*.json` | Demo data: 12 categories, 8 **fictional** brands, 48 products, 3 banners, shop settings |
| `src/mocks/mockApi.js` | Fake API returning the same JSON shapes the Spring Boot API will return, including search, filters, sorting and pagination (ready for Phase 2) |
| `src/services/apiClient.js` | Axios instance with base URL, JWT header and error messages (used from Phase 4) |
| `src/services/productService.js` | Products (mock or real API, switched by `VITE_USE_MOCKS`) |
| `src/services/catalogService.js` | Categories and brands |
| `src/services/settingsService.js` | Shop settings and hero banners |
| `src/hooks/useAsync.js` | Loading / error / data / retry for any service call |
| `src/context/SettingsContext.jsx`, `settings-context.js`, `src/hooks/useSettings.js` | Loads shop settings once and shares them |

### Utilities
| File | Purpose |
|---|---|
| `src/utils/format.js` | ₹ formatting (en-IN), discount %, address joining, `tel:` links |
| `src/utils/whatsapp.js` | Builds `wa.me` links and the general-enquiry message text (with the legal note in comments) |
| `src/utils/constants.js` | Navigation links, legal links, placeholder text |

### Layout components (`src/components/layout/`)
| File | Purpose |
|---|---|
| `Logo.jsx` | Original SVG starburst logo + "SIVA Crackers / Eachanari · Coimbatore" wordmark |
| `AnnouncementBar.jsx` | Dismissible top strip, text from settings |
| `Header.jsx` | Sticky header: logo, search (submits to `/products?search=`), Estimate icon, Login / Signup, desktop nav with active underline |
| `MobileMenu.jsx` | Slide-in drawer with **Menu / Categories / Brands** tabs, Escape to close, scroll lock |
| `LegalNotice.jsx` | Legal notice above the footer on every page |
| `Footer.jsx` | Quick links, categories, shop details with placeholders, legal links, copyright |

### Product components (`src/components/product/`)
| File | Purpose |
|---|---|
| `ProductCard.jsx` | Image, discount badge, pack size, brand, stock, name, category, price, View details, Enquire |
| `ProductArt.jsx` | 12 original SVG illustrations (one per category) used until real photos are uploaded |
| `PriceTag.jsx` | Indicative price with `*` and struck-through MRP |
| `StockBadge.jsx` | In stock / Few left / Out of stock |
| `WhatsAppEnquiryButton.jsx` | Enquiry button with the rules above |

### Homepage components (`src/components/home/`)
| File | Purpose |
|---|---|
| `HeroCarousel.jsx` | 3-slide auto-playing banner, arrows, dots, pauses on hover/focus |
| `FireworksBackdrop.jsx` | Animated SVG firework bursts and twinkling stars |
| `QuickTiles.jsx` | Price list, Call, WhatsApp enquiry, Safety tips, Visit us |
| `CategoryGrid.jsx` | Shop by category |
| `ProductRow.jsx` | Featured products and New arrivals (swipe row on phones, grid on larger screens) |
| `BrandStrip.jsx` | Scrolling popular-brands band |
| `AboutBlock.jsx` | About text and 4 highlight cards (from settings) |
| `SafetyBlock.jsx` | Safety tips band |
| `ContactBlock.jsx` | Contact details and area map (labelled approximate) |

### Shared UI (`src/components/ui/`)
`SectionHeading`, `Skeleton` (+ `ProductCardSkeleton`), `ErrorState` (with retry), `PageLoader`, `Reveal` (fade-up on scroll), `PlaceholderValue` ("To be updated").

### Pages (`src/pages/`)
`HomePage.jsx`, `ComingSoonPage.jsx` (temporary, for Phase 2–3 routes), `NotFoundPage.jsx`.

## Testing performed

| Check | Result |
|---|---|
| `npm run lint` | Pass, no warnings |
| `npm run build` | Pass (JS 126 kB gzip, CSS 9 kB gzip) |
| Desktop render of every homepage section | Pass |
| Mobile (375 px) render | Pass, after fixes below |
| Hero carousel next / dots / autoplay | Pass |
| Header search → `/products?search=rocket` | Pass |
| All 49 homepage links resolve to a defined route | Pass |
| Unknown URL → 404 page | Pass |
| Mobile menu open, tabs, Escape close, scroll lock | Pass |
| Mock API: search, sort, category filter, price filter, in-stock filter, pagination | Pass |
| WhatsApp message text, number normalisation, invalid/missing number | Pass |
| WhatsApp buttons with a temporary test number (then reverted to `null`) | 18 links, open in new tab |
| Browser console after fresh load | No errors |

### Bugs found and fixed during testing
1. **Mobile menu only as tall as the header** – the header's blur effect trapped the fixed drawer. Fixed by rendering the drawer through a React portal.
2. **Page wider than the phone screen** – hidden screen-reader labels inside cards escaped the swipe row. Fixed by making each card a positioning container.
3. **Hero text overflowing on phones** – slides were absolutely positioned. Now stacked in a grid so the tallest slide sets the height.
4. **Login button cut off on phones** – tightened header spacing and logo size.
5. **Hero text touching the arrows on laptops** – added side padding.
6. **Stock badge wrapping** next to long brand names – made it no-wrap.

## Final checks before Phase 1 sign-off

| Check | Result |
|---|---|
| Popular Brands duplicates | Intentional: the marquee renders the 8 brands twice so the loop has no gap. The copy is `aria-hidden` and not keyboard-focusable, so screen readers and keyboards see 8 brands. Visitors with "reduce motion" turned on now get a single, non-duplicated swipe row. The mobile menu and future Brands page use the data directly (8 entries). |
| Demo data labelling | Site-wide "Demonstration data" notice under the header; **Demo** tag on every product card and brand card; `"isDemo": true` on all 48 products and 8 brands; brand descriptions say "Fictional brand used for demonstration only"; `src/mocks/data/README.md` documents what is demo. The notice is controlled by `IS_DEMO_DATA` (on while `VITE_USE_MOCKS=true`). |
| Navigation links | All 49 unique internal links on the homepage (header, mobile menu targets, tiles, hero, categories, products, brands, footer, legal) clicked in the browser; each reached its route and rendered the expected page heading. No 404s. |
| No ordering / checkout / payment | Code search for cart, checkout, payment, order, booking, delivery, UPI and payment-gateway names: only matches are the legal disclaimers and the "Not an order" enquiry text. The only forms are the two search boxes. No POST/PUT requests. No payment packages in `package.json`. |
| Lint / build / mobile width | Pass; page width on a 375 px phone is exactly 375 px. |

## Not covered yet (by plan)
- Real catalogue, filters, pagination UI (Phase 2)
- Product detail, Price List, About, Contact, Safety, Legal, Login/Register, Estimate List pages (Phase 3)
- Automated unit tests: a test runner (Vitest) is not part of the agreed stack. It can be added in Phase 7 if you approve.
