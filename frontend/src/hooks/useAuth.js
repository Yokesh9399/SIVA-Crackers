import { useContext } from 'react';
import { AuthContext } from '../context/auth-context.js';

/** Returns { user, status, sessionExpired, login, register, logout, setUser }. */
export function useAuth() {
  return useContext(AuthContext);
}
