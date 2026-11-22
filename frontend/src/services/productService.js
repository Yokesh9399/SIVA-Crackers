import apiClient from './apiClient.js';
import { USE_MOCKS } from '../config/env.js';
import * as mockApi from '../mocks/mockApi.js';

// Converts array filters to the comma-separated form Spring expects (?categoryIds=1,2).
function toQuery(params) {
  return Object.fromEntries(
    Object.entries(params)
      .filter(([, v]) => v !== undefined && v !== null && v !== '' && !(Array.isArray(v) && v.length === 0))
      .map(([k, v]) => [k, Array.isArray(v) ? v.join(',') : v]),
  );
}

export const productService = {
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
};
