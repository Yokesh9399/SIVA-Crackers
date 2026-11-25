import axios from 'axios';
import { API_URL } from '../config/env.js';

/**
 * Shared Axios instance for every call to the Spring Boot API.
 *
 * Login sessions use an HttpOnly cookie set by the server. JavaScript never sees or stores the
 * login token (nothing in localStorage / sessionStorage); the browser simply sends the cookie.
 *
 * CSRF: the server sets a readable XSRF-TOKEN cookie; Axios copies it into the X-XSRF-TOKEN
 * header on every same-origin request. In development Vite proxies /api to the backend, so the
 * site and the API share one origin.
 */
const apiClient = axios.create({
  baseURL: API_URL,
  timeout: 15000,
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  headers: { 'Content-Type': 'application/json' },
});

const SAFE_METHODS = ['get', 'head', 'options'];

function hasCsrfCookie() {
  return document.cookie.split(';').some((c) => c.trim().startsWith('XSRF-TOKEN='));
}

let csrfRequest = null;

/** Asks the server for a CSRF cookie (once at a time). */
export function ensureCsrfToken(force = false) {
  if (!force && hasCsrfCookie()) return Promise.resolve();
  csrfRequest ??= apiClient.get('/auth/csrf').finally(() => {
    csrfRequest = null;
  });
  return csrfRequest;
}

// Before the first POST/PUT/DELETE, make sure a CSRF cookie exists.
apiClient.interceptors.request.use(async (config) => {
  const method = (config.method ?? 'get').toLowerCase();
  if (!SAFE_METHODS.includes(method) && !config.url?.endsWith('/auth/csrf')) {
    await ensureCsrfToken();
  }
  return config;
});

/** Fired when the server says the login session is no longer valid (expired, signed out elsewhere). */
export const SESSION_EXPIRED_EVENT = 'siva:session-expired';

// Requests where a 401 is an expected answer, not an expired session.
const AUTH_PROBES = ['/auth/me', '/auth/login', '/auth/register', '/auth/logout'];

// Turn API errors into one readable message for the UI.
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const { config, response } = error;
    const status = response?.status;

    // CSRF token missing or rotated (e.g. after login in another tab): fetch a new one and retry once.
    if (status === 403 && config && !config._csrfRetried && /security token/i.test(response?.data?.message ?? '')) {
      config._csrfRetried = true;
      await ensureCsrfToken(true);
      return apiClient(config);
    }

    if (status === 401 && config && !AUTH_PROBES.some((p) => config.url?.endsWith(p))) {
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
    }

    const message =
      response?.data?.message ?? (error.request ? 'Unable to reach the server. Please try again.' : error.message);
    const wrapped = new Error(message);
    wrapped.status = status; // e.g. 404 → page shows "not found"
    wrapped.fieldErrors = response?.data?.fieldErrors ?? []; // [{ field, message }] for forms
    return Promise.reject(wrapped);
  },
);

export default apiClient;
