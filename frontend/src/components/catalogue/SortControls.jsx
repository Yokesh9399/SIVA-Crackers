import { ArrowUpDown } from 'lucide-react';
import { PAGE_SIZES, SORT_OPTIONS } from '../../hooks/useCatalogueFilters.js';

const selectClass =
  'w-full appearance-none rounded-full border border-slate-200 bg-white py-2.5 pr-8 pl-9 text-sm font-medium text-navy-900 shadow-sm focus:border-orange-500 focus:ring-2 focus:ring-orange-500/20 focus:outline-none';

const chevron = (
  <svg className="pointer-events-none absolute top-1/2 right-3 size-4 -translate-y-1/2 text-slate-400" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
    <path d="M5.3 7.3a1 1 0 0 1 1.4 0L10 10.6l3.3-3.3a1 1 0 1 1 1.4 1.4l-4 4a1 1 0 0 1-1.4 0l-4-4a1 1 0 0 1 0-1.4Z" />
  </svg>
);

export function SortSelect({ value, onChange, id = 'catalogue-sort' }) {
  return (
    <div className="relative min-w-0 flex-1 sm:w-56 sm:flex-none">
      <label htmlFor={id} className="sr-only">
        Sort products
      </label>
      <ArrowUpDown className="pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2 text-slate-400" aria-hidden="true" />
      <select id={id} value={value} onChange={(e) => onChange(e.target.value)} className={selectClass}>
        {SORT_OPTIONS.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      {chevron}
    </div>
  );
}

export function PageSizeSelect({ value, onChange, id = 'catalogue-size' }) {
  return (
    <div className="flex items-center gap-2 text-sm text-slate-500">
      <label htmlFor={id}>Per page</label>
      <div className="relative">
        <select
          id={id}
          value={value}
          onChange={(e) => onChange(Number(e.target.value))}
          className="appearance-none rounded-full border border-slate-200 bg-white py-1.5 pr-8 pl-3 text-sm font-medium text-navy-900 focus:border-orange-500 focus:outline-none"
        >
          {PAGE_SIZES.map((n) => (
            <option key={n} value={n}>
              {n}
            </option>
          ))}
        </select>
        {chevron}
      </div>
    </div>
  );
}
