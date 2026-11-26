import apiClient from './apiClient.js';

// Customer accounts. The server keeps the login session in an HttpOnly cookie,
// so none of these calls return or store a token.

export const authService = {
  /** Who is logged in: the user, or null for visitors (never an error for visitors). */
  session() {
    return apiClient.get('/auth/session').then((r) => r.data.user);
  },

  /** The logged-in customer, or rejects with status 401. */
  me() {
    return apiClient.get('/auth/me').then((r) => r.data);
  },

  /** { identifier: email or mobile, password } */
  login(credentials) {
    return apiClient.post('/auth/login', credentials).then((r) => r.data);
  },

  /** Staff login (OWNER / MANAGER / CASHIER) for the admin dashboard. */
  staffLogin(credentials) {
    return apiClient.post('/auth/staff/login', credentials).then((r) => r.data);
  },

  /** { fullName, email, phone?, password } – always creates a customer account. */
  register(details) {
    return apiClient.post('/auth/register', details).then((r) => r.data);
  },

  logout() {
    return apiClient.post('/auth/logout');
  },

  /** { fullName, phone } */
  updateProfile(details) {
    return apiClient.put('/account/profile', details).then((r) => r.data);
  },

  /** { currentPassword, newPassword } – signs out other devices. */
  changePassword(passwords) {
    return apiClient.put('/account/password', passwords).then((r) => r.data);
  },
};
