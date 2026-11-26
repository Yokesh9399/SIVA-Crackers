import { useCallback, useMemo, useRef, useState } from 'react';
import { CheckCircle2, AlertTriangle, X } from 'lucide-react';
import { ToastContext } from './toast-context.js';

/** Small notifications in the corner (bottom on phones). Success messages close themselves; errors stay until closed. */
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const nextId = useRef(1);

  const dismiss = useCallback((id) => setToasts((list) => list.filter((t) => t.id !== id)), []);

  const show = useCallback(
    (tone, message) => {
      const id = nextId.current++;
      setToasts((list) => [...list.slice(-3), { id, tone, message }]);
      if (tone === 'success') setTimeout(() => dismiss(id), 4000);
    },
    [dismiss],
  );

  const value = useMemo(() => ({ success: (m) => show('success', m), error: (m) => show('error', m) }), [show]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="pointer-events-none fixed inset-x-0 bottom-0 z-[60] flex flex-col items-center gap-2 p-4 sm:inset-x-auto sm:right-0 sm:items-end">
        {toasts.map((t) => (
          <div
            key={t.id}
            role={t.tone === 'error' ? 'alert' : 'status'}
            className={`pointer-events-auto flex w-full max-w-sm items-start gap-3 rounded-xl px-4 py-3 text-sm shadow-lg ring-1 ${
              t.tone === 'error' ? 'bg-red-50 text-red-800 ring-red-200' : 'bg-emerald-50 text-emerald-900 ring-emerald-200'
            }`}
          >
            {t.tone === 'error' ? (
              <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
            ) : (
              <CheckCircle2 className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
            )}
            <p className="flex-1">{t.message}</p>
            <button type="button" onClick={() => dismiss(t.id)} className="-m-1 rounded p-1 opacity-70 hover:opacity-100" aria-label="Close notification">
              <X className="size-4" aria-hidden="true" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}
