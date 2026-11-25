import { createContext } from 'react';

// The logged-in customer for the whole site.
// Provider: context/AuthContext.jsx. Consumer hook: hooks/useAuth.js.
// status: 'loading' | 'authenticated' | 'anonymous'
export const AuthContext = createContext({
  user: null,
  status: 'loading',
  sessionExpired: false,
  login: async () => {},
  register: async () => {},
  logout: async () => {},
  setUser: () => {},
});
