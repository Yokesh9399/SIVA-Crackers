import axios from 'axios';
import { API_URL } from '../config/env.js';

// Shared Axios instance for every call to the Spring Boot API (used from Phase 4).
const apiClient = axios.create({
  baseURL: API_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// Attach the customer JWT (added in Phase 4) to every request when present.
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('siva_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Turn API errors into one readable message for the UI.
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const message =
      error.response?.data?.message ?? (error.request ? 'Unable to reach the server. Please try again.' : error.message);
    const wrapped = new Error(message);
    wrapped.status = error.response?.status; // e.g. 404 → page shows "not found"
    return Promise.reject(wrapped);
  },
);

export default apiClient;
