import { Check } from 'lucide-react';
import PriceFilter from './PriceFilter.jsx';

function Section({ title, children, action }) {
  return (
    <fieldset className="border-b border-slate-200 py-5 first:pt-0 last:border-0 last:pb-0">
      <div className="mb-3 flex items-center justify-between">
        <legend className="font-display text-sm font-semibold text-navy-900">{title}</legend>
        {action}
      </div>
      {children}
    </fieldset>
  );
}

/**
 * All catalogue filters: categories, brands, price and availability.
 * Used in the desktop sidebar and inside the mobile filter sheet.
 * `hide` lets category/brand pages hide the filter that is already fixed by the page.
 */
export default function FilterPanel({ filters, categories, brands, priceRange, update, toggleIn, hide = {}, idPrefix = 'filters' }) {
  return (
    <div>
      {!hide.categories && (
        <Section
          title="Categories"
          action={
            filters.categories.length > 0 && (
              <button type="button" onClick={() => update({ categories: [] })} className="text-xs font-medium text-orange-600 hover:underline">
                Clear
              </button>
            )
          }
        >
          <ul className="space-y-1">
            {categories?.map((c) => {
              const checked = filters.categories.includes(c.slug);
              return (
                <li key={c.id}>
                  <label className="flex cursor-pointer items-center gap-2.5 rounded-lg px-2 py-1.5 text-sm hover:bg-slate-50">
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={() => toggleIn('categories', c.slug)}
                      className="peer sr-only"
                    />
                    <span
                      aria-hidden="true"
                      className={`grid size-4.5 shrink-0 place-items-center rounded border transition peer-focus-visible:ring-2 peer-focus-visible:ring-orange-500 ${
                        checked ? 'border-orange-500 bg-orange-500 text-white' : 'border-slate-300 bg-white'
                      }`}
                    >
                      {checked && <Check className="size-3" strokeWidth={3} />}
                    </span>
                    <span className={`flex-1 ${checked ? 'font-semibold text-navy-900' : 'text-slate-600'}`}>{c.name}</span>
                    <span className="text-xs text-slate-400">{c.productCount}</span>
                  </label>
                </li>
              );
            })}
          </ul>
        </Section>
      )}

      {!hide.brands && (
        <Section
          title="Brands"
          action={
            filters.brands.length > 0 && (
              <button type="button" onClick={() => update({ brands: [] })} className="text-xs font-medium text-orange-600 hover:underline">
                Clear
              </button>
            )
          }
        >
          <div className="flex flex-wrap gap-2">
            {brands?.map((b) => {
              const active = filters.brands.includes(b.slug);
              return (
                <button
                  key={b.id}
                  type="button"
                  aria-pressed={active}
                  onClick={() => toggleIn('brands', b.slug)}
                  className={`inline-flex items-center gap-1 rounded-full px-3 py-1.5 text-xs font-medium ring-1 transition ${
                    active
                      ? 'bg-navy-900 text-white ring-navy-900'
                      : 'bg-white text-slate-600 ring-slate-200 hover:ring-navy-900/40'
                  }`}
                >
                  {active && <Check className="size-3" strokeWidth={3} aria-hidden="true" />}
                  {b.name}
                  <span className={active ? 'text-white/60' : 'text-slate-400'}>({b.productCount})</span>
                </button>
              );
            })}
          </div>
        </Section>
      )}

      <Section title="Indicative price">
        <PriceFilter
          minPrice={filters.minPrice}
          maxPrice={filters.maxPrice}
          range={priceRange}
          onApply={update}
          idPrefix={`${idPrefix}-price`}
        />
      </Section>

      <Section title="Availability">
        <label className="flex cursor-pointer items-center justify-between gap-3 text-sm text-slate-600">
          Hide out-of-stock items
          <input
            type="checkbox"
            role="switch"
            checked={filters.inStock}
            onChange={(e) => update({ inStock: e.target.checked })}
            className="peer sr-only"
          />
          <span
            aria-hidden="true"
            className="relative h-6 w-11 shrink-0 rounded-full bg-slate-300 transition peer-checked:bg-orange-500 peer-focus-visible:ring-2 peer-focus-visible:ring-orange-500 peer-focus-visible:ring-offset-2 after:absolute after:top-0.5 after:left-0.5 after:size-5 after:rounded-full after:bg-white after:shadow after:transition peer-checked:after:translate-x-5"
          />
        </label>
      </Section>
    </div>
  );
}
