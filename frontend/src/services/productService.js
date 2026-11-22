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

  /** Full product detail incl. safety notes. Rejects with error.status === 404 for unknown slugs. */
  getBySlug(slug) {
    if (USE_MOCKS) return mockApi.getProductBySlug(slug);
    return apiClient.get(`/public/products/${encodeURIComponent(slug)}`).then((r) => r.data);
  },

  getRelated(slug, limit = 4) {
    if (USE_MOCKS) return mockApi.getRelatedProducts(slug, limit);
    return apiClient.get(`/public/products/${encodeURIComponent(slug)}/related`, { params: { limit } }).then((r) => r.data);
  },

  /** Products for a list of slugs (Estimate List). Unknown slugs are skipped. */
  getBySlugs(slugs) {
    if (USE_MOCKS) return mockApi.getProductsBySlugs(slugs);
    if (!slugs.length) return Promise.resolve([]);
    return apiClient.get('/public/products/by-slugs', { params: { slugs: slugs.join(',') } }).then((r) => r.data);
  },

  /** [{ category, products[] }] for the Price List page. */
  getPriceList() {
    if (USE_MOCKS) return mockApi.getPriceList();
    return apiClient.get('/public/price-list').then((r) => r.data);
  },

  /** { min, max } indicative price across the catalogue, used as price filter hints. */
  getPriceRange() {
    if (USE_MOCKS) return mockApi.getPriceRange();
    return apiClient.get('/public/price-range').then((r) => r.data);
  },
};
