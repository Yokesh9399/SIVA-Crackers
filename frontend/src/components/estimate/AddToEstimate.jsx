import { useState } from 'react';
import { Link } from 'react-router-dom';
import { CheckCircle2, ClipboardPlus } from 'lucide-react';
import QuantityStepper from '../ui/QuantityStepper.jsx';
import { useEstimate } from '../../hooks/useEstimate.js';

/**
 * "Add to my estimate list" on the product detail page.
 * Browser-only planning aid – clearly labelled as NOT an order.
 */
export default function AddToEstimate({ product }) {
  const { add, qtyOf } = useEstimate();
  const [qty, setQty] = useState(1);
  const [justAdded, setJustAdded] = useState(false);
  const inList = qtyOf(product.slug);

  const onAdd = () => {
    add(product.slug, qty);
    setJustAdded(true);
    setQty(1);
  };

  return (
    <div className="rounded-2xl bg-white p-4 ring-1 ring-slate-200/70">
      <p className="text-sm font-semibold text-navy-900">Planning your visit?</p>
      <p className="mt-0.5 text-xs text-slate-500">
        Add this item to your personal estimate list to see an indicative total. It stays on this device only – it is not an order.
      </p>
      <div className="mt-3 flex flex-wrap items-center gap-3">
        <QuantityStepper value={qty} onChange={setQty} label={`Quantity of ${product.name}`} id="estimate-qty" />
        <button type="button" onClick={onAdd} className="btn min-w-[12rem] flex-1 bg-navy-900 whitespace-nowrap text-white hover:bg-navy-800 sm:flex-none">
          <ClipboardPlus className="size-4" aria-hidden="true" />
          Add to estimate list
        </button>
      </div>
      {inList > 0 && (
        <p className="mt-3 flex items-center gap-1.5 text-xs text-emerald-700" role="status">
          <CheckCircle2 className="size-4" aria-hidden="true" />
          {justAdded ? 'Added. ' : ''}
          {inList} in your estimate list ·{' '}
          <Link to="/estimate" className="font-semibold underline">
            View list
          </Link>
        </p>
      )}
    </div>
  );
}
