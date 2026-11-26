import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Search, X } from 'lucide-react';
import AdminPageHeader from '../components/AdminPageHeader.jsx';
import DateRange from '../components/DateRange.jsx';
import { Badge } from '../components/Badges.jsx';
import Pagination from '../../components/ui/Pagination.jsx';
import ErrorState from '../../components/ui/ErrorState.jsx';
import EmptyState from '../../components/ui/EmptyState.jsx';
import Skeleton from '../../components/ui/Skeleton.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import { adminService } from '../adminService.js';
import { fmtChange, fmtDateTime, fmtMoney } from '../format.js';

const TYPE_BADGE = { STOCK_IN: ['green', 'Stock in'], STOCK_OUT: ['red', 'Stock out'], ADJUSTMENT: ['blue', 'Adjustment'] };

/** /admin/stock-history – every stock change, newest first. */
export default function StockHistoryPage() {
  const [params, setParams] = useSearchParams();
  const filters = {
    from: params.get('from') ?? '',
    to: params.get('to') ?? '',
    type: params.get('type') ?? '',
    search: params.get('search') ?? '',
    productId: params.get('productId') ?? '',
    supplierId: params.get('supplierId') ?? '',
    page: Math.max(1, Number(params.get('page')) || 1),
    size: '25',
  };
  const productName = params.get('product');
  const supplierName = params.get('supplier');
  const [searchText, setSearchText] = useState(filters.search);

  const update = (changes) => {
    const next = new URLSearchParams(params);
    for (const [k, v] of Object.entries(changes)) {
      if (v === '' || v == null) next.delete(k);
      else next.set(k, String(v));
    }
    if (!('page' in changes)) next.delete('page');
    setParams(next, { replace: 'search' in changes });
  };
  useEffect(() => {
    if (searchText === filters.search) return undefined;
    const t = setTimeout(() => update({ search: searchText.trim() }), 350);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchText]);

  const list = useAsync(() => adminService.listMovements({ ...filters, page: filters.page - 1 }), [filters]);

  return (
    <>
      <AdminPageHeader
        title="Stock history"
        description="Every change to stock with the quantity before and after, who made it and why. Entries are never edited – mistakes are corrected with a new adjustment."
        crumbs={[{ label: 'Stock', to: '/admin/inventory' }, { label: 'Stock history' }]}
      />
      <section aria-label="Filters" className="admin-card mb-4 grid grid-cols-1 gap-3 p-4 md:grid-cols-2 lg:grid-cols-4">
        <div className="md:col-span-2">
          <DateRange from={filters.from} to={filters.to} onChange={(r) => update(r)} idPrefix="history" />
        </div>
        <label className="block text-xs font-medium text-slate-600">
          Type
          <select className="admin-input mt-1" value={filters.type} onChange={(e) => update({ type: e.target.value })}>
            <option value="">All changes</option>
            <option value="STOCK_IN">Stock in</option>
            <option value="STOCK_OUT">Stock out</option>
            <option value="ADJUSTMENT">Adjustments</option>
          </select>
        </label>
        <label className="relative block text-xs font-medium text-slate-600">
          Product
          <Search className="pointer-events-none absolute bottom-3 left-3 size-4 text-slate-400" aria-hidden="true" />
          <input type="search" className="admin-input mt-1 pl-9" placeholder="Name or code…" value={searchText} maxLength={100} onChange={(e) => setSearchText(e.target.value)} />
        </label>
        {(filters.productId || filters.supplierId) && (
          <div className="flex flex-wrap gap-2 md:col-span-2 lg:col-span-4">
            {filters.productId && (
              <button type="button" className="admin-btn-secondary py-1.5" onClick={() => update({ productId: '', product: '' })}>
                Product: {productName ?? `#${filters.productId}`} <X className="size-4" aria-label="remove filter" />
              </button>
            )}
            {filters.supplierId && (
              <button type="button" className="admin-btn-secondary py-1.5" onClick={() => update({ supplierId: '', supplier: '' })}>
                Supplier: {supplierName ?? `#${filters.supplierId}`} <X className="size-4" aria-label="remove filter" />
              </button>
            )}
          </div>
        )}
      </section>

      {list.error ? (
        <ErrorState message={list.error.message} onRetry={list.retry} />
      ) : !list.data ? (
        <div className="space-y-2">{Array.from({ length: 6 }, (_, i) => <Skeleton key={i} className="h-14" />)}</div>
      ) : list.data.totalElements === 0 ? (
        <EmptyState title="No stock changes found" message="Stock changes appear here once stock is received, sold, damaged or counted." />
      ) : (
        <>
          <p className="mb-2 text-sm text-slate-600" aria-live="polite">{list.data.totalElements} changes</p>
          <div className="admin-card hidden overflow-x-auto md:block">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-100 text-xs tracking-wide text-slate-500 uppercase">
                <tr>
                  <th scope="col" className="px-4 py-3">When</th>
                  <th scope="col" className="px-4 py-3">Product</th>
                  <th scope="col" className="px-4 py-3">Change</th>
                  <th scope="col" className="px-4 py-3 text-right">Before → After</th>
                  <th scope="col" className="px-4 py-3">Details</th>
                  <th scope="col" className="px-4 py-3">By</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {list.data.content.map((m) => {
                  const [color, label] = TYPE_BADGE[m.type];
                  return (
                    <tr key={m.id}>
                      <td className="px-4 py-3 whitespace-nowrap text-slate-600">{fmtDateTime(m.createdAt)}</td>
                      <td className="px-4 py-3">
                        {m.productName}
                        <span className="block text-xs text-slate-400">{m.productSku}{m.productId == null && ' · product deleted'}</span>
                      </td>
                      <td className="px-4 py-3">
                        <Badge color={color}>{label}</Badge>
                        <span className="block text-xs text-slate-500">{m.reasonLabel}</span>
                      </td>
                      <td className="px-4 py-3 text-right whitespace-nowrap">
                        {m.previousQuantity} → <strong>{m.newQuantity}</strong>
                        <span className={`block text-xs ${m.quantityChange > 0 ? 'text-emerald-700' : 'text-red-600'}`}>{fmtChange(m.quantityChange)}</span>
                      </td>
                      <td className="px-4 py-3 text-xs text-slate-600">
                        {m.supplier && <span className="block">Supplier: {m.supplier.name}</span>}
                        {m.unitCost != null && <span className="block">Cost: {fmtMoney(m.unitCost)} each</span>}
                        {m.note && <span className="block break-words">{m.note}</span>}
                      </td>
                      <td className="px-4 py-3 text-xs text-slate-600">{m.performedByName ?? '—'}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          <ul className="space-y-2 md:hidden">
            {list.data.content.map((m) => {
              const [color, label] = TYPE_BADGE[m.type];
              return (
                <li key={m.id} className="admin-card p-3 text-sm">
                  <div className="flex items-start justify-between gap-2">
                    <p className="min-w-0 font-medium break-words">{m.productName}</p>
                    <Badge color={color}>{label}</Badge>
                  </div>
                  <p className="mt-1">{m.previousQuantity} → <strong>{m.newQuantity}</strong> <span className="text-slate-500">({fmtChange(m.quantityChange)}, {m.reasonLabel})</span></p>
                  {m.supplier && <p className="text-xs text-slate-500">Supplier: {m.supplier.name}</p>}
                  {m.note && <p className="text-xs break-words text-slate-500">{m.note}</p>}
                  <p className="mt-1 text-xs text-slate-400">{fmtDateTime(m.createdAt)} · {m.performedByName ?? '—'}</p>
                </li>
              );
            })}
          </ul>
          <div className="mt-6">
            <Pagination page={filters.page} totalPages={list.data.totalPages} onChange={(page) => update({ page })} />
          </div>
        </>
      )}
    </>
  );
}
