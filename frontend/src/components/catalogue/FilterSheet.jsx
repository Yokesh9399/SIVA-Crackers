import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';

/**
 * Mobile bottom sheet that holds the filter panel.
 * Filters apply instantly; the footer button just closes the sheet and shows the results.
 * Rendered in a portal so the sticky header cannot clip it.
 */
export default function FilterSheet({ open, onClose, onClearAll, resultCount, activeCount, children }) {
  useEffect(() => {
    if (!open) return undefined;
    const onKey = (e) => e.key === 'Escape' && onClose();
    document.addEventListener('keydown', onKey);
    const previous = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = previous;
    };
  }, [open, onClose]);

  return createPortal(
    <div className={`fixed inset-0 z-50 lg:hidden ${open ? '' : 'pointer-events-none'}`} inert={!open}>
      <div
        className={`absolute inset-0 bg-navy-950/60 transition-opacity duration-300 ${open ? 'opacity-100' : 'opacity-0'}`}
        onClick={onClose}
      />
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Filter products"
        className={`absolute inset-x-0 bottom-0 flex max-h-[88vh] flex-col rounded-t-3xl bg-cream shadow-2xl transition-transform duration-300 ${
          open ? 'translate-y-0' : 'translate-y-full'
        }`}
      >
        <div className="relative flex items-center justify-between border-b border-slate-200 px-5 pt-6 pb-4">
          <span className="absolute top-2.5 left-1/2 h-1.5 w-12 -translate-x-1/2 rounded-full bg-slate-300" aria-hidden="true" />
          <h2 className="text-lg font-bold">Filters {activeCount > 0 && <span className="text-orange-500">({activeCount})</span>}</h2>
          <button type="button" onClick={onClose} className="rounded-full p-2 hover:bg-slate-200" aria-label="Close filters">
            <X className="size-5" aria-hidden="true" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto overscroll-contain px-5 py-5">{children}</div>

        <div className="flex gap-3 border-t border-slate-200 bg-white px-5 py-4">
          <button
            type="button"
            onClick={onClearAll}
            disabled={activeCount === 0}
            className="btn flex-1 border border-slate-200 text-navy-900 hover:bg-slate-50"
          >
            Clear all
          </button>
          <button type="button" onClick={onClose} className="btn-primary flex-[2]">
            Show {resultCount ?? ''} {resultCount === 1 ? 'product' : 'products'}
          </button>
        </div>
      </div>
    </div>,
    document.body,
  );
}
