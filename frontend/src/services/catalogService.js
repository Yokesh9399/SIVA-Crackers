import apiClient from './apiClient.js';
import { USE_MOCKS } from '../config/env.js';
import * as mockApi from '../mocks/mockApi.js';

// Categories and brands share the same simple read-only pattern.

export const categoryService = {
  getAll() {
    if (USE_MOCKS) return mockApi.getCategories();
    return apiClient.get('/public/categories').then((r) => r.data);
  },
};

export const brandService = {
  getAll() {
    if (USE_MOCKS) return mockApi.getBrands();
    return apiClient.get('/public/brands').then((r) => r.data);
  },
};
