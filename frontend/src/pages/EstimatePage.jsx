import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ClipboardList, Info, Printer, Trash2, X } from 'lucide-react';
import PageHeader from '../components/ui/PageHeader.jsx';
import PrintHeader from '../components/ui/PrintHeader.jsx';
import QuantityStepper from '../components/ui/QuantityStepper.jsx';
import EmptyState from '../components/ui/EmptyState.jsx';
import ErrorState from '../components/ui/ErrorState.jsx';
import DemoTag from '../components/ui/DemoTag.jsx';
import StockBadge from '../components/product/StockBadge.jsx';
import { useAsync } from '../hooks/useAsync.js';
import { useEstimate } from '../hooks/useEstimate.js';
import { useDocumentTitle } from '../hooks/useDocumentTitle.js';
import { productService } from '../services/productService.js';
import { formatCurrency } from '../utils/format.js';

const NOT_AN_ORDER =
  'This list is for your own planning only. It is saved on this device and is never sent to the shop. It is not an order, booking or reservation, and nothing is paid online.';

/** /estimate – browser-only, printable planning list. */
export default function EstimatePage() {
  useDocumentTitle('My Estimate List');
  const { items, setQty, remove, clear } = useEstimate();
  const [confirmClear, setConfirmClear] = useState(false);
  const slugs = items.map((i) => i.slug);
  const products = useAsync(() => productService.getBySlugs(slugs), [slugs.join(',')]);

  const bySlug = new Map((products.data ?? []).map((p) => [p.slug, p]));
  const rows = items.map((i) => ({ ...i, product: bySlug.get(i.slug) })).filter((r) => r.product);
  const missing = products.data ? items.length - rows.length : 0;
  const totalPrice = rows.reduce((sum, r) => sum + r.product.price * r.qty, 0);
  const totalMrp = rows.reduce((sum, r) => sum + r.product.mrp * r.qty, 0);
  const totalUnits = rows.reduce((sum, r) => sum + r.qty, 0);
  const firstLoad = products.loading && !products.data;

  return (
    <>
      <PageHeader
        title="My Estimate List"
        subtitle="Plan your visit: note the items you are interested in and see an indicative total. Print it or show it at the shop."
        crumbs={[{ label: 'My Estimate List' }]}
      />

      <section className="container-page py-8 sm:py-10">
        <PrintHeader title="My Estimate List" note="Personal planning list – not an order or booking." />

        <p className="mb-6 flex gap-3 rounded-2xl bg-violet-50 p-4 text-sm text-violet-900 ring-1 ring-violet-200 print:hidden" role="note">
          <Info className="mt-0.5 size-5 shrink-0" aria-hidden="true" />
          {NOT_AN_ORDER}
        </p>

        {items.length === 0 ? (
          <EmptyState
            title="Your estimate list is empty"
            message="Open any product and choose “Add to estimate list” to start planning. Your list stays on this device only."
          >
            <Link to="/products" className="btn-primary mt-6">
              <ClipboardList className="size-4" aria-hidden="true" /> Browse products
            </Link>
          </EmptyState>
        ) : products.error ? (
          <ErrorState message="We could not load the products in your list." onRetry={products.retry} />
        ) : firstLoad ? (
          <p className="py-10 text-center text-slate-500">Loading your list…</p>
        ) : (
          <>
            {missing > 0 && (
              <p className="mb-4 text-sm text-amber-700 print:hidden">
                {missing} item(s) in your list are no longer in the catalogue and are not shown.
              </p>
            )}

            {/* Phones: compact cards */}
            <ul className="space-y-3 sm:hidden print:hidden" aria-label="Items in your estimate list">
              {rows.map(({ slug, qty, product }) => (
                <li key={slug} className="rounded-2xl bg-white p-4 shadow-sm ring-1 ring-slate-200/70">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <Link to={`/products/${slug}`} className="font-semibold text-navy-900 hover:text-orange-600">
                        {product.name}
                      </Link>
                      <p className="mt-0.5 text-xs text-slate-500">
                        {product.brand.name} · {product.packSize} · {formatCurrency(product.price)} each
                      </p>
                      <div className="mt-1.5 flex flex-wrap gap-1.5">
                        <StockBadge status={product.stockStatus} />
                        <DemoTag show={product.isDemo} />
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() => remove(slug)}
                      className="-mt-1 -mr-1 rounded-full p-2 text-slate-400 hover:bg-red-50 hover:text-red-600"
                      aria-label={`Remove ${product.name} from estimate list`}
                    >
                      <X className="size-4" aria-hidden="true" />
                    </button>
                  </div>
                  <div className="mt-3 flex items-center justify-between border-t border-slate-100 pt-3">
                    <QuantityStepper value={qty} onChange={(n) => setQty(slug, n)} label={`Quantity of ${product.name}`} size="sm" />
                    <span className="font-display text-lg font-bold text-navy-900">{formatCurrency(product.price * qty)}*</span>
                  </div>
                </li>
              ))}
              <li className="flex items-center justify-between rounded-2xl bg-navy-900 p-4 text-white">
                <span className="text-sm">
                  Indicative total
                  <span className="block text-xs text-white/60">
                    {rows.length} products · {totalUnits} units
                  </span>
                </span>
                <span className="font-display text-2xl font-extrabold text-gold-400">{formatCurrency(totalPrice)}*</span>
              </li>
            </ul>

            {/* Tablet / desktop / print: table */}
            <div className="hidden overflow-hidden rounded-3xl bg-white shadow-sm ring-1 ring-slate-200/70 sm:block print:block print:rounded-none print:shadow-none print:ring-0">
              <table className="w-full text-sm">
                <caption className="sr-only">Items in your estimate list</caption>
                <thead className="bg-navy-900 text-left text-xs tracking-wider text-white uppercase print:bg-white print:text-black">
                  <tr>
                    <th scope="col" className="px-4 py-3">Product</th>
                    <th scope="col" className="hidden px-4 py-3 text-right sm:table-cell print:table-cell">Price*</th>
                    <th scope="col" className="px-4 py-3 text-center">Qty</th>
                    <th scope="col" className="px-4 py-3 text-right">Amount*</th>
                    <th scope="col" className="w-10 px-2 py-3 print:hidden">
                      <span className="sr-only">Remove</span>
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {rows.map(({ slug, qty, product }) => (
                    <tr key={slug} className="align-middle break-inside-avoid">
                      <td className="px-4 py-3">
                        <Link to={`/products/${slug}`} className="font-semibold text-navy-900 hover:text-orange-600">
                          {product.name}
                        </Link>
                        <div className="mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-slate-500">
                          <span>
                            {product.brand.name} · {product.packSize}
                          </span>
                          <span className="sm:hidden print:hidden">· {formatCurrency(product.price)} each</span>
                          <span className="print:hidden">
                            <StockBadge status={product.stockStatus} />
                          </span>
                          <span className="print:hidden">
                            <DemoTag show={product.isDemo} />
                          </span>
                        </div>
                      </td>
                      <td className="hidden px-4 py-3 text-right whitespace-nowrap sm:table-cell print:table-cell">{formatCurrency(product.price)}</td>
                      <td className="px-4 py-3 text-center">
                        <span className="print:hidden">
                          <QuantityStepper value={qty} onChange={(n) => setQty(slug, n)} label={`Quantity of ${product.name}`} size="sm" />
                        </span>
                        <span className="hidden print:inline">{qty}</span>
                      </td>
                      <td className="px-4 py-3 text-right font-semibold whitespace-nowrap text-navy-900">{formatCurrency(product.price * qty)}</td>
                      <td className="px-2 py-3 print:hidden">
                        <button
                          type="button"
                          onClick={() => remove(slug)}
                          className="rounded-full p-2 text-slate-400 hover:bg-red-50 hover:text-red-600"
                          aria-label={`Remove ${product.name} from estimate list`}
                        >
                          <X className="size-4" aria-hidden="true" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
                <tfoot className="border-t-2 border-navy-900/10 bg-cream print:bg-white">
                  {/* one cell per column so it lines up whether or not the Price column is visible */}
                  <tr>
                    <th scope="row" className="px-4 py-4 text-left text-sm font-semibold text-navy-900">
                      Indicative total
                      <span className="block text-xs font-normal text-slate-500">
                        {rows.length} {rows.length === 1 ? 'product' : 'products'}
                        {totalMrp > totalPrice && <> · MRP total {formatCurrency(totalMrp)}</>}
                      </span>
                    </th>
                    <td className="hidden sm:table-cell print:table-cell" />
                    <td className="px-4 py-4 text-center text-sm font-semibold text-navy-900">{totalUnits}</td>
                    <td className="px-4 py-4 text-right font-display text-xl font-extrabold whitespace-nowrap text-navy-900">
                      {formatCurrency(totalPrice)}*
                    </td>
                    <td className="print:hidden" />
                  </tr>
                </tfoot>
              </table>
            </div>

            <p className="mt-3 text-xs text-slate-500">
              * Indicative prices only. Final prices, availability and any offers are confirmed at the shop. {NOT_AN_ORDER}
            </p>

            <div className="mt-6 flex flex-wrap gap-3 print:hidden">
              <button type="button" onClick={() => window.print()} className="btn-primary">
                <Printer className="size-4" aria-hidden="true" /> Print / save as PDF
              </button>
              <Link to="/products" className="btn border border-slate-200 bg-white text-navy-900 hover:bg-slate-50">
                Add more products
              </Link>
              {confirmClear ? (
                <span className="flex items-center gap-2 text-sm">
                  Remove all items?
                  <button
                    type="button"
                    onClick={() => {
                      clear();
                      setConfirmClear(false);
                    }}
                    className="btn bg-red-600 py-2 text-white hover:bg-red-700"
                  >
                    Yes, clear list
                  </button>
                  <button type="button" onClick={() => setConfirmClear(false)} className="btn py-2 text-navy-900 hover:bg-slate-100">
                    Cancel
                  </button>
                </span>
              ) : (
                <button type="button" onClick={() => setConfirmClear(true)} className="btn text-red-600 hover:bg-red-50">
                  <Trash2 className="size-4" aria-hidden="true" /> Clear list
                </button>
              )}
            </div>
          </>
        )}
      </section>
    </>
  );
}
