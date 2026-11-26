import { todayIst } from '../format.js';

/** From / to date inputs used by history, expense and report filters. */
export default function DateRange({ from, to, onChange, idPrefix = 'range' }) {
  const max = todayIst();
  return (
    <div className="grid grid-cols-2 gap-2">
      <label className="block text-xs font-medium text-slate-600" htmlFor={`${idPrefix}-from`}>
        From
        <input id={`${idPrefix}-from`} type="date" className="admin-input mt-1" value={from ?? ''} max={to || max} onChange={(e) => onChange({ from: e.target.value, to })} />
      </label>
      <label className="block text-xs font-medium text-slate-600" htmlFor={`${idPrefix}-to`}>
        To
        <input id={`${idPrefix}-to`} type="date" className="admin-input mt-1" value={to ?? ''} min={from || undefined} max={max} onChange={(e) => onChange({ from, to: e.target.value })} />
      </label>
    </div>
  );
}
