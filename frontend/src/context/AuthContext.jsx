import { useCallback, useEffect, useMemo, useState } from 'react';
import { AuthContext } from './auth-context.js';
import { authService } from '../services/authService.js';
import { SESSION_EXPIRED_EVENT } from '../services/apiClient.js';
import { USE_MOCKS } from '../config/env.js';

/**
 * Knows who is logged in. On page load it asks the server (GET /api/auth/session); the browser sends
 * the HttpOnly session cookie automatically. Nothing about the session is stored in JavaScript
 * storage – only the user's display details are kept in memory.
 */
export function AuthProvider({ children }) {
  const [user, setUserState] = useState(null);
  const [status, setStatus] = useState(USE_MOCKS ? 'anonymous' : 'loading');
  const [sessionExpired, setSessionExpired] = useState(false);

  const setUser = useCallback((next) => {
    setUserState(next);
    setStatus(next ? 'authenticated' : 'anonymous');
    if (next) setSessionExpired(false);
  }, []);

  useEffect(() => {
    if (USE_MOCKS) return undefined; // accounts need the backend
    let cancelled = false;
    authService.session().then(
      (me) => !cancelled && setUser(me),
      () => !cancelled && setUser(null),
    );
    return () => {
      cancelled = true;
    };
  }, [setUser]);

  // Any API call answered with 401 while logged in => the session has ended (expired or signed out elsewhere).
  useEffect(() => {
    const onExpired = () => {
      setUserState((current) => {
        if (current) setSessionExpired(true);
        return null;
      });
      setStatus('anonymous');
    };
    window.addEventListener(SESSION_EXPIRED_EVENT, onExpired);
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, onExpired);
  }, []);

  const login = useCallback(async (credentials) => setUser(await authService.login(credentials)), [setUser]);
  const staffLogin = useCallback(async (credentials) => setUser(await authService.staffLogin(credentials)), [setUser]);
  const register = useCallback(async (details) => setUser(await authService.register(details)), [setUser]);
  const logout = useCallback(async () => {
    try {
      await authService.logout();
    } finally {
      setUser(null);
    }
  }, [setUser]);

  const value = useMemo(
    () => ({ user, status, sessionExpired, login, staffLogin, register, logout, setUser }),
    [user, status, sessionExpired, login, staffLogin, register, logout, setUser],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
