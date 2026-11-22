import { formatCurrency } from '../../utils/format.js';

/**
 * Indicative price with MRP struck through when it is higher.
 * The asterisk links the price to the disclaimer shown on the page.
 */
export default function PriceTag({ price, mrp, size = 'md' }) {
  const big = size === 'lg';
  const showMrp = mrp && price && mrp > price;
  return (
    <div className="flex flex-wrap items-baseline gap-x-2">
      <span className={`font-display font-bold text-navy-900 ${big ? 'text-3xl' : 'text-lg'}`}>
        {formatCurrency(price)}
        <span className="align-super text-xs text-orange-500" title="Indicative price">
          *
        </span>
      </span>
      {showMrp && (
        <span className={`text-slate-400 line-through ${big ? 'text-base' : 'text-xs'}`}>
          <span className="sr-only">MRP </span>
          {formatCurrency(mrp)}
        </span>
      )}
    </div>
  );
}
