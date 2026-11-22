import { useDeferredValue, useState } from 'react';
import { Link } from 'react-router-dom';
import { Info, Printer, Search, X } from 'lucide-react';
import PageHeader from '../components/ui/PageHeader.jsx';
import PrintHeader from '../components/ui/PrintHeader.jsx';
import EmptyState from '../components/ui/EmptyState.jsx';
import ErrorState from '../components/ui/ErrorState.jsx';
import Skeleton from '../components/ui/Skeleton.jsx';
import { useAsync } from '../hooks/useAsync.js';
import { useDocumentTitle } from '../hooks/useDocumentTitle.js';
import { useSettings } from '../hooks/useSettings.js';
import { productService } from '../services/productService.js';
import { discountPercent, formatCurrency } from '../utils/format.js';
import { IS_DEMO_DATA } from '../config/env.js';

/** Every word must match the product, brand or category name (same rule as the catalogue search). */
function matches(product, words) {
  if (!words.length) return true;
  const text = `${product.name} ${product.brand.name} ${product.category.name}`.toLowerCase();
  return words.every((w) => text.includes(w));
}

/** /price-list – category-wise indicative prices, searchable and print-friendly. */
export default function PriceListPage() {
  useDocumentTitle('Price List');
  const { settings } = useSettings();
  const list = useAsync(() => productService.getPriceList(), []);
  const [query, setQuery] = useState('');
  const deferredQuery = useDeferredValue(query);

  const words = deferredQuery.trim().toLowerCase().split(/\s+/).filter(Boolean);
  const groups = (list.data ?? [])
    .map((g) => ({ ...g, products: g.products.filter((p) => matches(p, words)) }))
    .filter((g) => g.products.length > 0);
  const total = groups.reduce((n, g) => n + g.products.length, 0);
  const allCount = (list.data ?? []).reduce((n, g) => n + g.products.length, 0);
  // Running serial number across categories: first row number of each group.
  const firstSerial = groups.map((_, i) => groups.slice(0, i).reduce((n, g) => n + g.products.length, 0) + 1);

  const disclaimer = settings?.priceDisclaimer ?? 'Prices shown are indicative only and may change without notice.';

  return (
    <>
      <PageHeader
        title="Price List"
        subtitle="Category-wise indicative prices for our full range. Search, jump to a category, or print the list."
        crumbs={[{ label: 'Price List' }]}
      />

      <section className="container-page py-8 sm:py-10">
        <PrintHeader title="Price List" note={words.length ? `Filtered by search: “${deferredQuery.trim()}”` : undefined} />

        <p className="mb-6 flex gap-3 rounded-2xl bg-gold-400/15 p-4 text-sm text-navy-900 ring-1 ring-gold-400/40 print:hidden" role="note">
          <Info className="mt-0.5 size-5 shrink-0 text-orange-600" aria-hidden="true" />
          <span>
            <strong>Price disclaimer:</strong> {disclaimer} This list is for information only – firecrackers are not sold, booked or delivered
            online. {IS_DEMO_DATA && <strong className="text-violet-700">All items and prices shown are demonstration data.</strong>}
          </span>
        </p>

        {/* Toolbar (screen only) */}
        {/* sticky only from tablet up – on phones it would cover too much of the list */}
        <div className="z-20 mb-6 bg-cream/95 py-3 backdrop-blur sm:sticky sm:top-28 lg:top-32 print:hidden">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
            <div className="relative flex-1">
              <label htmlFor="price-search" className="sr-only">
                Search the price list
              </label>
              <Search className="pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2 text-slate-400" aria-hidden="true" />
              <input
                id="price-search"
                type="search"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Search product, brand or category…"
                autoComplete="off"
                className="w-full rounded-full border border-slate-200 bg-white py-2.5 pr-10 pl-10 text-sm text-navy-900 shadow-sm placeholder:text-slate-400 focus:border-orange-500 focus:ring-2 focus:ring-orange-500/20 focus:outline-none [&::-webkit-search-cancel-button]:hidden"
              />
              {query && (
                <button
                  type="button"
                  onClick={() => setQuery('')}
                  className="absolute top-1/2 right-2 -translate-y-1/2 rounded-full p-1.5 text-slate-400 hover:bg-slate-100 hover:text-navy-900"
                  aria-label="Clear search"
                >
                  <X className="size-4" aria-hidden="true" />
                </button>
              )}
            </div>
            <button type="button" onClick={() => window.print()} className="btn-primary shrink-0">
              <Printer className="size-4" aria-hidden="true" /> Print price list
            </button>
          </div>
          {list.data && (
            <p className="mt-2 text-xs text-slate-500" aria-live="polite">
              {words.length ? `${total} of ${allCount} products match “${deferredQuery.trim()}”` : `${allCount} products in ${groups.length} categories`}
            </p>
          )}
        </div>

        {/* Category jump links */}
        {groups.length > 1 && (
          <nav aria-label="Jump to category" className="mb-8 flex flex-wrap gap-2 print:hidden">
            {groups.map((g) => (
              <a
                key={g.category.id}
                href={`#cat-${g.category.slug}`}
                className="rounded-full bg-white px-3 py-1.5 text-xs font-semibold text-navy-900 ring-1 ring-slate-200 transition hover:bg-navy-900 hover:text-white"
              >
                {g.category.name} <span className="font-normal opacity-60">({g.products.length})</span>
              </a>
            ))}
          </nav>
        )}

        {list.error && <ErrorState message="We could not load the price list." onRetry={list.retry} />}
        {list.loading && !list.data && (
          <div className="space-y-3">
            {Array.from({ length: 6 }, (_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        )}

        {list.data && total === 0 && (
          <EmptyState
            title="No products match your search"
            message={`Nothing in the price list matches “${deferredQuery.trim()}”. Try a different product, brand or category name.`}
            actionLabel="Clear search"
            onAction={() => setQuery('')}
          />
        )}

        <div className="space-y-8 print:space-y-5">
          {groups.map((g, gi) => (
            <section key={g.category.id} id={`cat-${g.category.slug}`} className="scroll-mt-52" aria-labelledby={`h-${g.category.slug}`}>
              <div className="mb-3 flex items-baseline justify-between gap-3 print:mb-1 print:break-after-avoid">
                <h2 id={`h-${g.category.slug}`} className="text-xl font-bold print:text-base print:text-black">
                  {g.category.name}
                </h2>
                <Link to={`/categories/${g.category.slug}`} className="text-xs font-semibold text-orange-600 hover:underline print:hidden">
                  View in catalogue →
                </Link>
              </div>

              <div className="overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-200/70 print:rounded-none print:shadow-none print:ring-1 print:ring-black/30">
                <table className="w-full text-sm print:text-[0.75rem]">
                  <caption className="sr-only">{g.category.name} prices</caption>
                  <thead className="bg-navy-900 text-left text-[0.7rem] tracking-wider text-white uppercase print:bg-white print:text-black">
                    <tr>
                      <th scope="col" className="w-12 px-3 py-2.5 text-center">No.</th>
                      <th scope="col" className="px-3 py-2.5">Product</th>
                      <th scope="col" className="hidden px-3 py-2.5 md:table-cell print:table-cell">Brand</th>
                      <th scope="col" className="hidden px-3 py-2.5 sm:table-cell print:table-cell">Pack</th>
                      <th scope="col" className="px-3 py-2.5 text-right">MRP</th>
                      <th scope="col" className="px-3 py-2.5 text-right">Price*</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 print:divide-black/20">
                    {g.products.map((p, pi) => {
                      const serial = firstSerial[gi] + pi;
                      const off = discountPercent(p.mrp, p.price);
                      return (
                        <tr key={p.id} className="break-inside-avoid hover:bg-cream/60">
                          <td className="px-3 py-2.5 text-center text-slate-400 print:text-black">{serial}</td>
                          <td className="px-3 py-2.5">
                            <Link to={`/products/${p.slug}`} className="font-medium text-navy-900 hover:text-orange-600">
                              {p.name}
                            </Link>
                            {p.stockStatus === 'OUT_OF_STOCK' && (
                              <span className="ml-2 text-[0.7rem] font-semibold text-slate-400 print:hidden">Out of stock</span>
                            )}
                            {/* brand + pack under the name on small screens */}
                            <span className="block text-xs text-slate-500 md:hidden print:hidden">
                              <span>{p.brand.name}</span>
                              <span className="sm:hidden"> · {p.packSize}</span>
                            </span>
                          </td>
                          <td className="hidden px-3 py-2.5 text-slate-600 md:table-cell print:table-cell print:text-black">{p.brand.name}</td>
                          <td className="hidden px-3 py-2.5 whitespace-nowrap text-slate-600 sm:table-cell print:table-cell print:text-black">{p.packSize}</td>
                          <td className="px-3 py-2.5 text-right whitespace-nowrap text-slate-400 line-through print:text-black print:no-underline">
                            {formatCurrency(p.mrp)}
                          </td>
                          <td className="px-3 py-2.5 text-right whitespace-nowrap">
                            <span className="font-bold text-navy-900">{formatCurrency(p.price)}</span>
                            {off && <span className="block text-[0.65rem] font-semibold text-emerald-700 print:hidden">{off}% off</span>}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </section>
          ))}
        </div>

        {list.data && total > 0 && (
          <p className="mt-8 text-xs text-slate-500 print:text-black">
            * {disclaimer} MRP is the maximum retail price printed on the pack; the indicative price is the shop&apos;s expected selling price. All
            purchases are made in person at the shop.
          </p>
        )}
      </section>
    </>
  );
}
