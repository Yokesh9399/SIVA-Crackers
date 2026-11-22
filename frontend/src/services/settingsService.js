import apiClient from './apiClient.js';
import { USE_MOCKS } from '../config/env.js';
import * as mockApi from '../mocks/mockApi.js';

// Business information, announcement, disclaimers and hero banners.
// Edited by the owner in the admin app from Phase 5.

export const settingsService = {
  get() {
    if (USE_MOCKS) return mockApi.getSettings();
    return apiClient.get('/public/settings').then((r) => r.data);
  },

  getBanners() {
    if (USE_MOCKS) return mockApi.getBanners();
    return apiClient.get('/public/banners').then((r) => r.data);
  },
};
