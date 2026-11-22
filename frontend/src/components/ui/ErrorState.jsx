import { AlertTriangle, RotateCcw } from 'lucide-react';

/** Friendly error box with an optional retry button. */
export default function ErrorState({ message = 'Something went wrong while loading this section.', onRetry }) {
  return (
    <div role="alert" className="flex flex-col items-center gap-3 rounded-2xl bg-white p-8 text-center ring-1 ring-red-100">
      <AlertTriangle className="size-8 text-red-500" aria-hidden="true" />
      <p className="text-sm text-slate-600">{message}</p>
      {onRetry && (
        <button type="button" onClick={onRetry} className="btn-primary">
          <RotateCcw className="size-4" aria-hidden="true" /> Try again
        </button>
      )}
    </div>
  );
}
