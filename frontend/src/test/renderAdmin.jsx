import { render } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { vi } from 'vitest';
import { AuthContext } from '../context/auth-context.js';
import { ToastProvider } from '../admin/context/ToastProvider.jsx';

export const OWNER = { id: 1, fullName: 'Shop Owner', email: 'owner@example.test', role: 'OWNER' };

/** Shows the current path so tests can check redirects. */
function LocationProbe() {
  const location = useLocation();
  return <div data-testid="location">{location.pathname + location.search}</div>;
}

/**
 * Renders admin UI with a router, toasts and a controllable auth state.
 * `routes` maps paths to elements; `at` is the starting URL.
 */
export function renderAdmin(routes, { at = '/', auth = {} } = {}) {
  const value = {
    user: OWNER,
    status: 'authenticated',
    sessionExpired: false,
    login: vi.fn(),
    staffLogin: vi.fn(),
    register: vi.fn(),
    logout: vi.fn().mockResolvedValue(),
    setUser: vi.fn(),
    ...auth,
  };
  const utils = render(
    <AuthContext.Provider value={value}>
      <ToastProvider>
        <MemoryRouter initialEntries={[at]}>
          <Routes>
            {Object.entries(routes).map(([path, element]) => (
              <Route key={path} path={path} element={element} />
            ))}
          </Routes>
          <LocationProbe />
        </MemoryRouter>
      </ToastProvider>
    </AuthContext.Provider>,
  );
  return { ...utils, auth: value };
}
