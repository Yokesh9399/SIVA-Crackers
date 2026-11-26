import { createContext } from 'react';

// Success / error notifications for the admin. Provider: ToastProvider.jsx. Hook: hooks/useToast.js.
export const ToastContext = createContext({ success: () => {}, error: () => {} });
