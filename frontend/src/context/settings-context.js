import { createContext } from 'react';

// Holds business information loaded once for the whole site.
// Provider: context/SettingsContext.jsx. Consumer hook: hooks/useSettings.js.
export const SettingsContext = createContext({ settings: null, loading: true, error: null });
