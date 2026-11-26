import { useId } from 'react';

/** Label + control + hint/error, shared by the admin text area, select and switch. */
function Wrap({ id, label, optional, hint, error, children }) {
  return (
    <div>
      <label htmlFor={id} className="mb-1 block text-sm font-medium text-navy-900">
        {label} {optional && <span className="font-normal text-slate-400">(optional)</span>}
      </label>
      {children}
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

function describedBy(id, hint, error) {
  return [error && `${id}-error`, hint && !error && `${id}-hint`].filter(Boolean).join(' ') || undefined;
}

export function TextAreaField({ label, optional, hint, error, rows = 4, ...props }) {
  const id = useId();
  return (
    <Wrap id={id} label={label} optional={optional} hint={hint} error={error}>
      <textarea
        id={id}
        rows={rows}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={describedBy(id, hint, error)}
        className={`admin-input ${error ? 'border-red-400' : ''}`}
        {...props}
      />
    </Wrap>
  );
}

export function SelectField({ label, optional, hint, error, children, ...props }) {
  const id = useId();
  return (
    <Wrap id={id} label={label} optional={optional} hint={hint} error={error}>
      <select
        id={id}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={describedBy(id, hint, error)}
        className={`admin-input ${error ? 'border-red-400' : ''}`}
        {...props}
      >
        {children}
      </select>
    </Wrap>
  );
}

/** On/off switch with a short explanation, e.g. "Show on website". */
export function SwitchField({ label, description, checked, onChange, disabled }) {
  const id = useId();
  return (
    <label htmlFor={id} className="flex cursor-pointer items-start gap-3 rounded-xl p-2 hover:bg-slate-50">
      <span className="relative mt-0.5 inline-flex shrink-0">
        <input
          id={id}
          type="checkbox"
          role="switch"
          className="peer sr-only"
          checked={checked}
          disabled={disabled}
          onChange={(e) => onChange(e.target.checked)}
        />
        <span className="h-6 w-11 rounded-full bg-slate-300 transition peer-checked:bg-emerald-500 peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-orange-500 peer-disabled:opacity-50" />
        <span className="absolute top-0.5 left-0.5 size-5 rounded-full bg-white shadow transition peer-checked:translate-x-5" />
      </span>
      <span className="text-sm">
        <span className="block font-medium text-navy-900">{label}</span>
        {description && <span className="block text-xs text-slate-500">{description}</span>}
      </span>
    </label>
  );
}
