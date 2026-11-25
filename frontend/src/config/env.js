// Central place for environment configuration.
// Values come from .env files (see .env.example). Vite only exposes keys that start with VITE_.

// Relative by default: in development Vite proxies /api to the Spring Boot backend (vite.config.js),
// and in production the site and the API are served from the same origin.
export const API_URL = import.meta.env.VITE_API_URL || '/api';

// true = run on the local sample data in src/mocks (Phases 1–3, no backend needed).
// Default false from Phase 4: the site reads everything from the backend.
export const USE_MOCKS = import.meta.env.VITE_USE_MOCKS === 'true';

// Whether demonstration data is shown is decided by the backend (settings.demoMode), see hooks/useDemoMode.js.
