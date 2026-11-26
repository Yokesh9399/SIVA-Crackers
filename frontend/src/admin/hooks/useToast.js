import { useContext } from 'react';
import { ToastContext } from '../context/toast-context.js';

/** Returns { success(message), error(message) }. */
export function useToast() {
  return useContext(ToastContext);
}
