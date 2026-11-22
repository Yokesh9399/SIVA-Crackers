import { SearchX } from 'lucide-react';

/** Friendly "nothing found" box with an optional action. */
export default function EmptyState({ title = 'No products found', message, actionLabel, onAction, children }) {
  return (
    <div className="flex flex-col items-center rounded-3xl bg-white px-6 py-14 text-center shadow-sm ring-1 ring-slate-200/70">
      <span className="grid size-16 place-items-center rounded-2xl bg-gold-400/15 text-orange-500">
        <SearchX className="size-8" aria-hidden="true" />
      </span>
      <h2 className="mt-5 text-xl font-bold">{title}</h2>
      {message && <p className="mt-2 max-w-md text-sm text-slate-600">{message}</p>}
      {actionLabel && onAction && (
        <button type="button" onClick={onAction} className="btn-primary mt-6">
          {actionLabel}
        </button>
      )}
      {children}
    </div>
  );
}
