import { useState } from 'react';
import { formatCurrency } from '../../utils/format.js';

const PRESETS = [
  { label: 'Under ₹200', min: null, max: 200 },
  { label: '₹200 – ₹500', min: 200, max: 500 },
  { label: '₹500 – ₹1,000', min: 500, max: 1000 },
  { label: 'Above ₹1,000', min: 1000, max: null },
];

const toText = (n) => (n == null ? '' : String(n));

/**
 * Minimum / maximum indicative price with Apply and Clear, plus quick presets.
 * `range` ({ min, max } across the catalogue) is shown as placeholder hints.
 */
export default function PriceFilter({ minPrice, maxPrice, range, onApply, idPrefix = 'price' }) {
  const [min, setMin] = useState(toText(minPrice));
  const [max, setMax] = useState(toText(maxPrice));
  const [synced, setSynced] = useState(`${minPrice}|${maxPrice}`);
  const [error, setError] = useState('');

  // Keep the inputs in step with the URL (presets, chips, "Clear all", Back button).
  const current = `${minPrice}|${maxPrice}`;
  if (current !== synced) {
    setSynced(current);
    setMin(toText(minPrice));
    setMax(toText(maxPrice));
    setError('');
  }

  const apply = (event) => {
    event.preventDefault();
    const lo = min === '' ? null : Number(min);
    const hi = max === '' ? null : Number(max);
    if ((lo != null && (!Number.isFinite(lo) || lo < 0)) || (hi != null && (!Number.isFinite(hi) || hi < 0))) {
      setError('Enter prices as positive numbers.');
      return;
    }
    if (lo != null && hi != null && lo > hi) {
      setError('Minimum price cannot be more than maximum price.');
      return;
    }
    setError('');
    onApply({ minPrice: lo, maxPrice: hi });
  };

  const clear = () => {
    setMin('');
    setMax('');
    setError('');
    onApply({ minPrice: null, maxPrice: null });
  };

  const hasValue = minPrice != null || maxPrice != null;
  const inputClass =
    'w-full rounded-lg border border-slate-200 bg-white py-2 pr-2 pl-7 text-sm text-navy-900 focus:border-orange-500 focus:ring-2 focus:ring-orange-500/20 focus:outline-none';

  return (
    <form onSubmit={apply} noValidate>
      <div className="grid grid-cols-2 gap-2">
        <div>
          <label htmlFor={`${idPrefix}-min`} className="mb-1 block text-xs font-medium text-slate-500">
            Minimum
          </label>
          <div className="relative">
            <span className="pointer-events-none absolute top-1/2 left-2.5 -translate-y-1/2 text-sm text-slate-400">₹</span>
            <input
              id={`${idPrefix}-min`}
              type="number"
              inputMode="numeric"
              min="0"
              value={min}
              onChange={(e) => setMin(e.target.value)}
              placeholder={range ? String(range.min) : '0'}
              className={inputClass}
            />
          </div>
        </div>
        <div>
          <label htmlFor={`${idPrefix}-max`} className="mb-1 block text-xs font-medium text-slate-500">
            Maximum
          </label>
          <div className="relative">
            <span className="pointer-events-none absolute top-1/2 left-2.5 -translate-y-1/2 text-sm text-slate-400">₹</span>
            <input
              id={`${idPrefix}-max`}
              type="number"
              inputMode="numeric"
              min="0"
              value={max}
              onChange={(e) => setMax(e.target.value)}
              placeholder={range ? String(range.max) : 'Any'}
              className={inputClass}
            />
          </div>
        </div>
      </div>

      {error && (
        <p role="alert" className="mt-2 text-xs text-red-600">
          {error}
        </p>
      )}

      <div className="mt-3 flex gap-2">
        <button type="submit" className="btn flex-1 bg-navy-900 py-2 text-white hover:bg-navy-800">
          Apply
        </button>
        <button
          type="button"
          onClick={clear}
          disabled={!hasValue && min === '' && max === ''}
          className="btn flex-1 border border-slate-200 py-2 text-navy-900 hover:bg-slate-50"
        >
          Clear
        </button>
      </div>

      <div className="mt-3 flex flex-wrap gap-1.5">
        {PRESETS.map((p) => {
          const active = minPrice === p.min && maxPrice === p.max;
          return (
            <button
              key={p.label}
              type="button"
              aria-pressed={active}
              onClick={() => onApply({ minPrice: p.min, maxPrice: p.max })}
              className={`rounded-full px-2.5 py-1 text-xs font-medium ring-1 transition ${
                active ? 'bg-orange-500 text-white ring-orange-500' : 'bg-white text-slate-600 ring-slate-200 hover:ring-orange-400'
              }`}
            >
              {p.label}
            </button>
          );
        })}
      </div>

      {range && (
        <p className="mt-2 text-[0.7rem] text-slate-400">
          Catalogue range: {formatCurrency(range.min)} – {formatCurrency(range.max)}
        </p>
      )}
    </form>
  );
}
