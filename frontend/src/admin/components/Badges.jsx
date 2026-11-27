const tone = {
  green: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  amber: 'bg-amber-50 text-amber-800 ring-amber-200',
  red: 'bg-red-50 text-red-700 ring-red-200',
  slate: 'bg-slate-100 text-slate-600 ring-slate-200',
  violet: 'bg-violet-50 text-violet-700 ring-violet-200',
  blue: 'bg-sky-50 text-sky-700 ring-sky-200',
};

export function Badge({ color = 'slate', children }) {
  return (
    <span className={`inline-flex max-w-full items-center rounded-full sm:whitespace-nowrap px-2 py-0.5 text-xs font-semibold ring-1 ${tone[color]}`}>
      {children}
    </span>
  );
}

const STOCK = {
  IN_STOCK: ['green', 'In stock'],
  LOW_STOCK: ['amber', 'Few left'],
  OUT_OF_STOCK: ['red', 'Out of stock'],
};

/** Availability from the stock level (the same rule as the public website). */
export function StockBadge({ status, quantity }) {
  const [color, label] = STOCK[status] ?? ['slate', status];
  return (
    <Badge color={color}>
      {label}
      {quantity !== undefined && <span className="ml-1 font-normal">({quantity})</span>}
    </Badge>
  );
}

/** Visible / Hidden / Inactive, plus "Not on website" when its category or brand is inactive. */
export function VisibilityBadge({ active, visibleOnWebsite, publiclyVisible }) {
  if (!active) return <Badge color="slate">Inactive</Badge>;
  if (!visibleOnWebsite) return <Badge color="amber">Hidden</Badge>;
  if (!publiclyVisible) return <Badge color="amber">Not shown – category/brand inactive</Badge>;
  return <Badge color="green">Visible</Badge>;
}

export function ActiveBadge({ active }) {
  return active ? <Badge color="green">Active</Badge> : <Badge color="slate">Inactive</Badge>;
}

export function DemoBadge({ show }) {
  return show ? <Badge color="violet">Demo</Badge> : null;
}
