import apiClient from './apiClient.js';
import { USE_MOCKS } from '../config/env.js';
// Sample data is loaded only in mock mode, so it is left out of the real website build.
const mockApi = () => import('../mocks/mockApi.js');

// Categories and brands share the same simple read-only pattern.

export const categoryService = {
  getAll() {
    if (USE_MOCKS) return mockApi().then((m) => m.getCategories());
    return apiClient.get('/public/categories').then((r) => r.data);
  },
};

export const brandService = {
  getAll() {
    if (USE_MOCKS) return mockApi().then((m) => m.getBrands());
    return apiClient.get('/public/brands').then((r) => r.data);
  },
};
