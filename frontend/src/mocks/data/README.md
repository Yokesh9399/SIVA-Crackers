# DEMONSTRATION DATA – NOT REAL BUSINESS INFORMATION

Everything in this folder is sample data for building and testing the website (Phases 1–3).

| File | What is demo |
|---|---|
| `products.json` | All 48 products, descriptions, pack sizes, MRPs, indicative prices and stock quantities are invented. Each entry has `"isDemo": true`. |
| `brands.json` | All 8 brand names are fictional. Each entry has `"isDemo": true`. |
| `categories.json` | Common product types, used as sample categories. Product counts come from the demo products. |
| `banners.json` | Sample hero slide text. |
| `settings.json` | Shop name and Eachanari, Coimbatore location are real. Every other business detail (phone, WhatsApp, email, hours, street address, licence) is `null` and shown as "To be updated". |

While `VITE_USE_MOCKS=true`, the website shows a "Demonstration data" notice on every page, and product cards and brand cards show a **Demo** tag.

In Phase 4 this data moves to the MySQL database as clearly marked seed data, and the owner replaces it with the real range from the admin dashboard.
