import apiClient from './apiClient.js';
import { USE_MOCKS } from '../config/env.js';
// Sample data is loaded only in mock mode, so it is left out of the real website build.
const mockApi = () => import('../mocks/mockApi.js');

// Business information, announcement, disclaimers and hero banners.
// Edited by the owner in the admin app from Phase 5.

export const settingsService = {
  get() {
    if (USE_MOCKS) return mockApi().then((m) => m.getSettings());
    return apiClient.get('/public/settings').then((r) => r.data);
  },

  getBanners() {
    if (USE_MOCKS) return mockApi().then((m) => m.getBanners());
    return apiClient.get('/public/banners').then((r) => r.data);
  },
};
