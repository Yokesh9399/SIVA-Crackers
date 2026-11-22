// Central place for environment configuration.
// Values come from .env files (see .env.example). Vite only exposes keys that start with VITE_.

export const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api';

// Phases 1–3 run on local demo data. Set VITE_USE_MOCKS=false once the backend exists (Phase 4).
export const USE_MOCKS = (import.meta.env.VITE_USE_MOCKS ?? 'true') !== 'false';

// While running on mock data, every product, brand, price and stock level is a sample.
// Controls the site-wide "Demonstration data" notice.
export const IS_DEMO_DATA = USE_MOCKS;
