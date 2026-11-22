import { useContext } from 'react';
import { EstimateContext } from '../context/estimate-context.js';

/** { items, count, qtyOf, add, setQty, remove, clear } – see context/EstimateContext.jsx */
export function useEstimate() {
  const context = useContext(EstimateContext);
  if (!context) throw new Error('useEstimate must be used inside <EstimateProvider>');
  return context;
}
