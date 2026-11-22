import { X } from 'lucide-react';
import { formatCurrency } from '../../utils/format.js';

function Chip({ label, onRemove }) {
  return (
    <li>
      <button
        type="button"
        onClick={onRemove}
        className="group inline-flex items-center gap-1.5 rounded-full bg-gold-400/20 py-1 pr-2 pl-3 text-xs font-medium text-navy-900 ring-1 ring-gold-400/60 transition hover:bg-gold-400/35"
        aria-label={`Remove filter: ${label}`}
      >
        {label}
        <X className="size-3.5 opacity-60 group-hover:opacity-100" aria-hidden="true" />
      </button>
    </li>
  );
}

function priceLabel(min, max) {
  if (min != null && max != null) return `${formatCurrency(min)} – ${formatCurrency(max)}`;
  if (min != null) return `From ${formatCurrency(min)}`;
  return `Up to ${formatCurrency(max)}`;
}

/** Removable chips for every active filter, plus "Clear all". */
export default function ActiveFilterChips({ filters, categories, brands, update, clearAll, hide = {} }) {
  const nameOf = (items, slug) => items?.find((i) => i.slug === slug)?.name ?? slug;
  const chips = [];

  if (filters.search) chips.push({ key: 'search', label: `“${filters.search}”`, remove: () => update({ search: '' }) });
  if (!hide.categories) {
    filters.categories.forEach((slug) =>
      chips.push({
        key: `c-${slug}`,
        label: nameOf(categories, slug),
        remove: () => update({ categories: filters.categories.filter((s) => s !== slug) }),
      }),
    );
  }
  if (!hide.brands) {
    filters.brands.forEach((slug) =>
      chips.push({
        key: `b-${slug}`,
        label: nameOf(brands, slug),
        remove: () => update({ brands: filters.brands.filter((s) => s !== slug) }),
      }),
    );
  }
  if (filters.minPrice != null || filters.maxPrice != null) {
    chips.push({ key: 'price', label: priceLabel(filters.minPrice, filters.maxPrice), remove: () => update({ minPrice: null, maxPrice: null }) });
  }
  if (filters.inStock) chips.push({ key: 'stock', label: 'In stock only', remove: () => update({ inStock: false }) });

  if (!chips.length) return null;

  return (
    <div className="flex flex-wrap items-center gap-2" aria-label="Active filters">
      <ul className="flex flex-wrap gap-2">
        {chips.map((c) => (
          <Chip key={c.key} label={c.label} onRemove={c.remove} />
        ))}
      </ul>
      <button type="button" onClick={clearAll} className="text-xs font-semibold text-orange-600 hover:underline">
        Clear all
      </button>
    </div>
  );
}
