import { createContext } from 'react';

// "My Estimate List" – a browser-only planning list. Provider: EstimateContext.jsx.
export const EstimateContext = createContext(null);

export const ESTIMATE_STORAGE_KEY = 'siva_estimate_v1';
export const MAX_QTY = 999;
