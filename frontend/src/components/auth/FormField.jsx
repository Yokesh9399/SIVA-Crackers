import { useId, useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';

const inputClass =
  'w-full rounded-lg border bg-white px-3 py-2.5 text-sm text-navy-900 placeholder:text-slate-400 focus:ring-2 focus:outline-none';

/** Labelled input with an inline error message. type="password" gets a show/hide button. */
export default function FormField({ label, error, hint, type = 'text', optional = false, ...inputProps }) {
  const id = useId();
  const [visible, setVisible] = useState(false);
  const isPassword = type === 'password';
  const describedBy = [error && `${id}-error`, hint && `${id}-hint`].filter(Boolean).join(' ') || undefined;

  return (
    <div>
      <label htmlFor={id} className="mb-1 block text-sm font-medium text-navy-900">
        {label} {optional && <span className="font-normal text-slate-400">(optional)</span>}
      </label>
      <div className="relative">
        <input
          id={id}
          type={isPassword && visible ? 'text' : type}
          aria-invalid={error ? 'true' : undefined}
          aria-describedby={describedBy}
          className={`${inputClass} ${isPassword ? 'pr-10' : ''} ${
            error
              ? 'border-red-400 focus:border-red-500 focus:ring-red-500/20'
              : 'border-slate-200 focus:border-orange-500 focus:ring-orange-500/20'
          }`}
          {...inputProps}
        />
        {isPassword && (
          <button
            type="button"
            onClick={() => setVisible((v) => !v)}
            className="absolute top-1/2 right-2 -translate-y-1/2 rounded p-1 text-slate-400 hover:text-navy-900"
            aria-label={visible ? 'Hide password' : 'Show password'}
          >
            {visible ? <EyeOff className="size-4" aria-hidden="true" /> : <Eye className="size-4" aria-hidden="true" />}
          </button>
        )}
      </div>
      {hint && !error && (
        <p id={`${id}-hint`} className="mt-1 text-xs text-slate-500">
          {hint}
        </p>
      )}
      {error && (
        <p id={`${id}-error`} className="mt-1 text-xs font-medium text-red-600">
          {error}
        </p>
      )}
    </div>
  );
}

/** Coloured message box above a form (errors, success, notices). */
export function FormMessage({ tone = 'error', children }) {
  if (!children) return null;
  const tones = {
    error: 'bg-red-50 text-red-700 ring-red-100',
    success: 'bg-emerald-50 text-emerald-800 ring-emerald-100',
    info: 'bg-amber-50 text-amber-900 ring-amber-100',
  };
  return (
    <p role={tone === 'error' ? 'alert' : 'status'} className={`rounded-lg px-3 py-2 text-sm ring-1 ${tones[tone]}`}>
      {children}
    </p>
  );
}
