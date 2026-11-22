const STYLES = {
  IN_STOCK: { label: 'In stock', className: 'bg-emerald-50 text-emerald-700 ring-emerald-200' },
  LOW_STOCK: { label: 'Few left', className: 'bg-amber-50 text-amber-700 ring-amber-200' },
  OUT_OF_STOCK: { label: 'Out of stock', className: 'bg-slate-100 text-slate-500 ring-slate-200' },
};

/** Small pill showing stock availability at the shop. */
export default function StockBadge({ status }) {
  const style = STYLES[status] ?? STYLES.IN_STOCK;
  return (
    <span className={`inline-flex shrink-0 items-center rounded-full whitespace-nowrap px-2 py-0.5 text-[0.7rem] font-semibold ring-1 ${style.className}`}>
      {style.label}
    </span>
  );
}
