import { useEffect, useRef, useState } from 'react';
import { Minus, Plus } from 'lucide-react';

/** − [ n ] + control. Typing is allowed; the value is clamped to 1…max on blur. */
export default function QuantityStepper({ value, onChange, max = 999, label = 'Quantity', size = 'md', id }) {
  const [draft, setDraft] = useState(String(value));
  const [synced, setSynced] = useState(value);
  if (value !== synced) {
    setSynced(value);
    setDraft(String(value));
  }

  // Latest committed number. +/− read this instead of the `value` prop so that very
  // fast repeated taps (before React re-renders) each count.
  const latest = useRef(value);
  useEffect(() => {
    latest.current = value;
  }, [value]);

  const clamp = (next) => Math.min(max, Math.max(1, Math.round(Number(next) || 1)));

  const commit = (next) => {
    const n = clamp(next);
    setDraft(String(n));
    if (n !== latest.current) {
      latest.current = n;
      onChange(n);
    }
  };

  const step = (delta) => commit(latest.current + delta);

  const small = size === 'sm';
  const btn = `grid place-items-center text-navy-900 transition hover:bg-slate-100 disabled:opacity-30 disabled:hover:bg-transparent ${small ? 'size-8' : 'size-10'}`;

  return (
    <div className="inline-flex items-center overflow-hidden rounded-full bg-white ring-1 ring-slate-200" role="group" aria-label={label}>
      <button type="button" className={btn} onClick={() => step(-1)} disabled={value <= 1} aria-label="Decrease quantity">
        <Minus className="size-4" aria-hidden="true" />
      </button>
      <input
        id={id}
        type="number"
        inputMode="numeric"
        min="1"
        max={max}
        value={draft}
        onChange={(e) => setDraft(e.target.value)}
        onBlur={() => commit(draft)}
        onKeyDown={(e) => e.key === 'Enter' && commit(draft)}
        aria-label={label}
        className={`border-x border-slate-200 text-center font-semibold text-navy-900 focus:outline-none [appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none ${small ? 'h-8 w-11 text-sm' : 'h-10 w-14'}`}
      />
      <button type="button" className={btn} onClick={() => step(1)} disabled={value >= max} aria-label="Increase quantity">
        <Plus className="size-4" aria-hidden="true" />
      </button>
    </div>
  );
}
