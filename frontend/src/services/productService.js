import apiClient from './apiClient.js';
import { USE_MOCKS } from '../config/env.js';
import * as mockApi from '../mocks/mockApi.js';

// Converts array filters to the comma-separated form Spring expects (?categories=sparklers,rockets)
// and drops empty values so the URL stays clean.
function toQuery(params) {
  return Object.fromEntries(
    Object.entries(params)
      .filter(([, v]) => v !== undefined && v !== null && v !== '' && v !== false && !(Array.isArray(v) && v.length === 0))
      .map(([k, v]) => [k, Array.isArray(v) ? v.join(',') : v]),
  );
}

export const productService = {
  /**
   * Paged catalogue. Returns { content, page, size, totalElements, totalPages }.
   * Phase 4 endpoint: GET /api/public/products
   */
  getProducts(params) {
    if (USE_MOCKS) return mockApi.getProducts(params);
    return apiClient.get('/public/products', { params: toQuery(params) }).then((r) => r.data);
  },

  getFeatured(limit = 8) {
    if (USE_MOCKS) return mockApi.getFeaturedProducts(limit);
    return apiClient.get('/public/products/featured', { params: { limit } }).then((r) => r.data);
  },

  getNewArrivals(limit = 8) {
    if (USE_MOCKS) return mockApi.getNewArrivals(limit);
    return apiClient.get('/public/products/new-arrivals', { params: { limit } }).then((r) => r.data);
  },

  /** { min, max } indicative price across the catalogue, used as price filter hints. */
  getPriceRange() {
    if (USE_MOCKS) return mockApi.getPriceRange();
    return apiClient.get('/public/price-range').then((r) => r.data);
  },
};
