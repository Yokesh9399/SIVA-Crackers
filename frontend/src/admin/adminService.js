import apiClient from '../services/apiClient.js';
import { API_URL, USE_MOCKS } from '../config/env.js';

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

  // ----- staff (OWNER)
  listStaff: (filters = {}) => call(() => apiClient.get('/admin/staff', { params: params(filters) })),
  createStaff: (body) => call(() => apiClient.post('/admin/staff', body)),
  updateStaff: (id, body) => call(() => apiClient.put(`/admin/staff/${id}`, body)),
  setStaffActive: (id, active) => call(() => apiClient.patch(`/admin/staff/${id}/status`, { active })),
  resetStaffPassword: (id, newPassword) => call(() => apiClient.post(`/admin/staff/${id}/reset-password`, { newPassword })),
  auditLog: (filters = {}) => call(() => apiClient.get('/admin/audit', { params: params(filters) })),

  // ----- inventory
  listStock: (filters) => call(() => apiClient.get('/admin/inventory', { params: params(filters) })),
  stockAlerts: () => call(() => apiClient.get('/admin/inventory/alerts')),
  movementReasons: () => call(() => apiClient.get('/admin/inventory/reasons')),
  stockOf: (productId) => call(() => apiClient.get(`/admin/inventory/products/${productId}`)),
  listMovements: (filters) => call(() => apiClient.get('/admin/inventory/movements', { params: params(filters) })),
  recordMovement: (body) => call(() => apiClient.post('/admin/inventory/movements', body)),
  setThreshold: (productId, reorderLevel) =>
    call(() => apiClient.patch(`/admin/inventory/products/${productId}/threshold`, { reorderLevel })),

  // ----- suppliers
  listSuppliers: (filters = {}) => call(() => apiClient.get('/admin/suppliers', { params: params(filters) })),
  getSupplier: (id) => call(() => apiClient.get(`/admin/suppliers/${id}`)),
  createSupplier: (body) => call(() => apiClient.post('/admin/suppliers', body)),
  updateSupplier: (id, body) => call(() => apiClient.put(`/admin/suppliers/${id}`, body)),
  setSupplierActive: (id, active) => call(() => apiClient.patch(`/admin/suppliers/${id}/status`, { active })),

  // ----- expenses
  expenseCategories: () => call(() => apiClient.get('/admin/expense-categories')),
  createExpenseCategory: (body) => call(() => apiClient.post('/admin/expense-categories', body)),
  updateExpenseCategory: (id, body) => call(() => apiClient.put(`/admin/expense-categories/${id}`, body)),
  listExpenses: (filters) => call(() => apiClient.get('/admin/expenses', { params: params(filters) })),
  createExpense: (body) => call(() => apiClient.post('/admin/expenses', body)),
  updateExpense: (id, body) => call(() => apiClient.put(`/admin/expenses/${id}`, body)),
  cancelExpense: (id, reason) => call(() => apiClient.post(`/admin/expenses/${id}/cancel`, { reason })),
  monthlyExpenses: (year) => call(() => apiClient.get('/admin/expenses/summary/monthly', { params: params({ year }) })),

  // ----- shop customers (contacts only)
  listCustomers: (search) => call(() => apiClient.get('/admin/customers', { params: params({ search }) })),
  createCustomer: (body) => call(() => apiClient.post('/admin/customers', body)),
  updateCustomer: (id, body) => call(() => apiClient.put(`/admin/customers/${id}`, body)),
  deleteCustomer: (id) => call(() => apiClient.delete(`/admin/customers/${id}`)),

  // ----- in-store billing (staff only)
  paymentMethods: () => call(() => apiClient.get('/admin/payment-methods')),
  createPaymentMethod: (body) => call(() => apiClient.post('/admin/payment-methods', body)),
  updatePaymentMethod: (id, body) => call(() => apiClient.put(`/admin/payment-methods/${id}`, body)),
  listBills: (filters) => call(() => apiClient.get('/admin/bills', { params: params(filters) })),
  getBill: (id) => call(() => apiClient.get(`/admin/bills/${id}`)),
  createBill: (body) => call(() => apiClient.post('/admin/bills', body)),
  voidBill: (id, reason) => call(() => apiClient.post(`/admin/bills/${id}/void`, { reason })),
  markBillPaid: (id, body) => call(() => apiClient.patch(`/admin/bills/${id}/payment`, body)),
  customerBills: (id, page = 0) => call(() => apiClient.get(`/admin/customers/${id}/bills`, { params: { page } })),

  // ----- reports (JSON); CSV files are downloaded with reportCsvUrl()
  report: (name, filters) => call(() => apiClient.get(`/admin/reports/${name}`, { params: params(filters) })),

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

/** Same-origin link to a CSV report download (the browser sends the login cookie). */
export function reportCsvUrl(name, filters = {}) {
  const q = new URLSearchParams({ ...params(filters), format: 'csv' });
  return `${API_URL}/admin/reports/${name}?${q}`;
}

/** Same-origin link to a bill's PDF invoice (opens in the browser's PDF viewer, which can print it). */
export function invoiceUrl(billId, download = false) {
  return `${API_URL}/admin/bills/${billId}/invoice.pdf${download ? '?download=true' : ''}`;
}

/** Same-origin link to a report PDF download. */
export function reportPdfUrl(name, filters = {}) {
  const q = new URLSearchParams({ ...params(filters), format: 'pdf' });
  return `${API_URL}/admin/reports/${name}?${q}`;
}
