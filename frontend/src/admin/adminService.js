import apiClient from '../services/apiClient.js';
import { USE_MOCKS } from '../config/env.js';

/**
 * OWNER admin API (/api/admin/**). Every call is checked again on the server (role + CSRF).
 * Mock mode never talks to the admin API, so sample data can never change real business data.
 */
function guard() {
  if (USE_MOCKS) {
    const err = new Error('The admin dashboard needs the backend. It is switched off while the website runs on sample data.');
    err.status = 503;
    throw err;
  }
}

async function call(fn) {
  guard();
  const r = await fn();
  return r.data;
}

/** Drops empty filter values so URLs stay clean. */
function params(obj) {
  return Object.fromEntries(Object.entries(obj).filter(([, v]) => v !== undefined && v !== null && v !== ''));
}

export const adminService = {
  dashboard: () => call(() => apiClient.get('/admin/dashboard')),

  // ----- products
  listProducts: (filters) => call(() => apiClient.get('/admin/products', { params: params(filters) })),
  getProduct: (id) => call(() => apiClient.get(`/admin/products/${id}`)),
  createProduct: (body) => call(() => apiClient.post('/admin/products', body)),
  updateProduct: (id, body) => call(() => apiClient.put(`/admin/products/${id}`, body)),
  quickUpdateProduct: (id, changes) => call(() => apiClient.patch(`/admin/products/${id}`, changes)),
  deleteProduct: (id) => call(() => apiClient.delete(`/admin/products/${id}`)),

  // ----- categories
  listCategories: (search) => call(() => apiClient.get('/admin/categories', { params: params({ search }) })),
  createCategory: (body) => call(() => apiClient.post('/admin/categories', body)),
  updateCategory: (id, body) => call(() => apiClient.put(`/admin/categories/${id}`, body)),
  deleteCategory: (id) => call(() => apiClient.delete(`/admin/categories/${id}`)),

  // ----- brands
  listBrands: (search) => call(() => apiClient.get('/admin/brands', { params: params({ search }) })),
  createBrand: (body) => call(() => apiClient.post('/admin/brands', body)),
  updateBrand: (id, body) => call(() => apiClient.put(`/admin/brands/${id}`, body)),
  deleteBrand: (id) => call(() => apiClient.delete(`/admin/brands/${id}`)),

  // ----- homepage slides
  listBanners: () => call(() => apiClient.get('/admin/banners')),
  createBanner: (body) => call(() => apiClient.post('/admin/banners', body)),
  updateBanner: (id, body) => call(() => apiClient.put(`/admin/banners/${id}`, body)),
  deleteBanner: (id) => call(() => apiClient.delete(`/admin/banners/${id}`)),

  // ----- business settings (existing Phase 4 endpoint)
  getSettings: () => call(() => apiClient.get('/admin/settings')),
  updateSettings: (body) => call(() => apiClient.put('/admin/settings', body)),

  /** Uploads one image; folder = products | brands | categories. Returns the stored URL. */
  uploadImage: (folder, file) =>
    call(() => {
      const form = new FormData();
      form.append('file', file);
      return apiClient.post(`/admin/uploads/${folder}`, form, { headers: { 'Content-Type': 'multipart/form-data' } });
    }).then((d) => d.url),
};

/** Client-side pre-check (the server checks again): JPEG / PNG / WebP up to 2 MB. */
export const IMAGE_RULES = { types: ['image/jpeg', 'image/png', 'image/webp'], maxBytes: 2 * 1024 * 1024 };

export function checkImageFile(file) {
  if (!file) return 'Please choose an image.';
  if (!IMAGE_RULES.types.includes(file.type)) return 'Please choose a JPEG, PNG or WebP image.';
  if (file.size > IMAGE_RULES.maxBytes) return 'The image is larger than 2 MB. Please choose a smaller one.';
  return null;
}
