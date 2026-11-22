import { ChevronLeft, ChevronRight } from 'lucide-react';
import { getPageItems } from '../../utils/pagination.js';

/** Previous / page numbers / Next. `page` is 1-based. */
export default function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null;
  const items = getPageItems(page, totalPages);
  const base = 'grid h-10 min-w-10 place-items-center rounded-full px-3 text-sm font-semibold transition';

  return (
    <nav aria-label="Pagination" className="flex items-center justify-center gap-1.5">
      <button
        type="button"
        onClick={() => onChange(page - 1)}
        disabled={page <= 1}
        className={`${base} gap-1 text-navy-900 ring-1 ring-slate-200 hover:bg-white disabled:cursor-not-allowed disabled:opacity-40 sm:flex`}
        aria-label="Previous page"
      >
        <ChevronLeft className="size-4" aria-hidden="true" />
        <span className="sr-only sm:not-sr-only">Prev</span>
      </button>

      <ul className="flex items-center gap-1.5">
        {items.map((item) =>
          typeof item === 'string' ? (
            <li key={item} className="px-1 text-slate-400" aria-hidden="true">
              …
            </li>
          ) : (
            <li key={item}>
              <button
                type="button"
                onClick={() => onChange(item)}
                aria-current={item === page ? 'page' : undefined}
                aria-label={`Page ${item}`}
                className={`${base} ${
                  item === page ? 'bg-navy-900 text-gold-400 shadow' : 'text-navy-900 hover:bg-white hover:ring-1 hover:ring-slate-200'
                }`}
              >
                {item}
              </button>
            </li>
          ),
        )}
      </ul>

      <button
        type="button"
        onClick={() => onChange(page + 1)}
        disabled={page >= totalPages}
        className={`${base} gap-1 text-navy-900 ring-1 ring-slate-200 hover:bg-white disabled:cursor-not-allowed disabled:opacity-40 sm:flex`}
        aria-label="Next page"
      >
        <span className="sr-only sm:not-sr-only">Next</span>
        <ChevronRight className="size-4" aria-hidden="true" />
      </button>
    </nav>
  );
}
