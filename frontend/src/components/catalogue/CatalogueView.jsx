import { useCallback, useRef, useState } from 'react';
import { SlidersHorizontal } from 'lucide-react';
import CatalogueSearch from './CatalogueSearch.jsx';
import FilterPanel from './FilterPanel.jsx';
import FilterSheet from './FilterSheet.jsx';
import ActiveFilterChips from './ActiveFilterChips.jsx';
import { PageSizeSelect, SortSelect } from './SortControls.jsx';
import ProductCard from '../product/ProductCard.jsx';
import Pagination from '../ui/Pagination.jsx';
import EmptyState from '../ui/EmptyState.jsx';
import ErrorState from '../ui/ErrorState.jsx';
import { ProductCardSkeleton } from '../ui/Skeleton.jsx';
import { useCatalogueFilters } from '../../hooks/useCatalogueFilters.js';
import { useAsync } from '../../hooks/useAsync.js';
import { useSettings } from '../../hooks/useSettings.js';
import { productService } from '../../services/productService.js';
import { brandService, categoryService } from '../../services/catalogService.js';
import { resultRange } from '../../utils/pagination.js';
import { useDemoMode } from '../../hooks/useDemoMode.js';

/**
 * Complete catalogue: search, filters (sidebar on desktop, bottom sheet on mobile),
 * active filter chips, sorting, page size, results count, product grid and pagination.
 *
 * fixedCategory / fixedBrand: slug locked by a category or brand page.
 */
export default function CatalogueView({ fixedCategory, fixedBrand }) {
  const { settings } = useSettings();
  const isDemo = useDemoMode();
  const { filters, apiParams, update, toggleIn, clearAll, activeCount } = useCatalogueFilters({
    category: fixedCategory,
    brand: fixedBrand,
  });
  const [sheetOpen, setSheetOpen] = useState(false);
  const resultsRef = useRef(null);

  const categories = useAsync(() => categoryService.getAll(), []);
  const brands = useAsync(() => brandService.getAll(), []);
  const priceRange = useAsync(() => productService.getPriceRange(), []);
  const products = useAsync(() => productService.getProducts(apiParams), [apiParams]);

  const result = products.data;
  const range = result ? resultRange(result) : null;
  const hide = { categories: Boolean(fixedCategory), brands: Boolean(fixedBrand) };
  const firstLoad = products.loading && !result;
  const pageOutOfRange = result && result.totalElements > 0 && result.content.length === 0;

  const onSearch = useCallback((search, options) => update({ search }, options), [update]);
  const closeSheet = useCallback(() => setSheetOpen(false), []);

  const goToPage = (page) => {
    update({ page });
    resultsRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  const panelProps = {
    filters,
    categories: categories.data,
    brands: brands.data,
    priceRange: priceRange.data,
    update,
    toggleIn,
    hide,
  };

  return (
    <div className="container-page py-8 sm:py-10">
      <div className="grid gap-8 lg:grid-cols-[17rem_1fr] xl:grid-cols-[18rem_1fr]">
        {/* Desktop filter sidebar */}
        <aside className="hidden lg:block" aria-label="Product filters">
          <div className="sticky top-32 max-h-[calc(100vh-9rem)] overflow-y-auto rounded-3xl bg-white p-5 shadow-sm ring-1 ring-slate-200/70">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="flex items-center gap-2 text-base font-bold">
                <SlidersHorizontal className="size-4 text-orange-500" aria-hidden="true" /> Filters
              </h2>
              {activeCount > 0 && (
                <button type="button" onClick={clearAll} className="text-xs font-semibold text-orange-600 hover:underline">
                  Clear all
                </button>
              )}
            </div>
            <FilterPanel {...panelProps} idPrefix="sidebar" />
          </div>
        </aside>

        <div ref={resultsRef} className="min-w-0 scroll-mt-36">
          {/* Toolbar */}
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
            <div className="flex-1">
              <CatalogueSearch value={filters.search} onSearch={onSearch} />
            </div>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setSheetOpen(true)}
                className="btn relative shrink-0 border border-slate-200 bg-white text-navy-900 shadow-sm lg:hidden"
                aria-haspopup="dialog"
              >
                <SlidersHorizontal className="size-4" aria-hidden="true" />
                Filters
                {activeCount > 0 && (
                  <span className="grid size-5 place-items-center rounded-full bg-orange-500 text-[0.65rem] font-bold text-white">
                    {activeCount}
                  </span>
                )}
              </button>
              <SortSelect value={filters.sort} onChange={(sort) => update({ sort })} />
            </div>
          </div>

          <div className="mt-4">
            <ActiveFilterChips
              filters={filters}
              categories={categories.data}
              brands={brands.data}
              update={update}
              clearAll={clearAll}
              hide={hide}
            />
          </div>

          {/* Results count */}
          <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
            <p className="text-sm text-slate-600" aria-live="polite">
              {firstLoad ? (
                'Loading products…'
              ) : range && range.total > 0 ? (
                <>
                  Showing <strong className="text-navy-900">{range.from}–{range.to}</strong> of{' '}
                  <strong className="text-navy-900">{range.total}</strong> {range.total === 1 ? 'product' : 'products'}
                  {filters.search && (
                    <>
                      {' '}
                      for “<span className="font-semibold text-navy-900">{filters.search}</span>”
                    </>
                  )}
                </>
              ) : result ? (
                '0 products found'
              ) : null}
            </p>
            <PageSizeSelect value={filters.size} onChange={(size) => update({ size })} />
          </div>

          {/* Results */}
          {/* aria-busy lives on the wrapper so it is present for grid, empty and error states alike */}
          <div className="mt-5" aria-busy={products.loading}>
            {products.error ? (
              <ErrorState message="We could not load the products. Please try again." onRetry={products.retry} />
            ) : firstLoad ? (
              <div className="grid grid-cols-2 gap-3 sm:gap-5 md:grid-cols-3 xl:grid-cols-4">
                {Array.from({ length: 8 }, (_, i) => (
                  <ProductCardSkeleton key={i} />
                ))}
              </div>
            ) : pageOutOfRange ? (
              <EmptyState
                title="This page has no products"
                message={`There are only ${result.totalPages} page(s) of results.`}
                actionLabel="Go to the first page"
                onAction={() => goToPage(1)}
              />
            ) : result && result.totalElements === 0 ? (
              <EmptyState
                title="No products match your search"
                message={
                  filters.search
                    ? `We couldn’t find anything for “${filters.search}”. Check the spelling, try a brand or category name, or clear your filters.`
                    : 'No products match these filters. Try widening the price range or removing a filter.'
                }
                actionLabel={activeCount > 0 ? 'Clear all filters' : undefined}
                onAction={clearAll}
              />
            ) : (
              <ul
                className={`grid grid-cols-2 gap-3 transition-opacity sm:gap-5 md:grid-cols-3 xl:grid-cols-4 ${
                  products.loading ? 'opacity-60' : ''
                }`}
              >
                {result?.content.map((product) => (
                  <li key={product.id}>
                    <ProductCard product={product} />
                  </li>
                ))}
              </ul>
            )}
          </div>

          {result && result.totalPages > 1 && !pageOutOfRange && (
            <div className="mt-10">
              <Pagination page={filters.page} totalPages={result.totalPages} onChange={goToPage} />
            </div>
          )}

          {/* Disclaimers */}
          <div className="mt-10 space-y-1.5 rounded-2xl bg-white/70 p-4 text-xs text-slate-500 ring-1 ring-slate-200/70">
            <p>
              <span className="text-orange-500">*</span>{' '}
              {settings?.priceDisclaimer ?? 'Prices shown are indicative only and may change without notice.'}
            </p>
            <p>This catalogue is for information only. Products are not sold, booked or delivered online. Please visit our shop to purchase.</p>
            {isDemo && <p className="font-medium text-violet-700">Items marked DEMO are sample data for testing.</p>}
          </div>
        </div>
      </div>

      <FilterSheet
        open={sheetOpen}
        onClose={closeSheet}
        onClearAll={clearAll}
        resultCount={result?.totalElements}
        activeCount={activeCount}
      >
        <FilterPanel {...panelProps} idPrefix="sheet" />
      </FilterSheet>
    </div>
  );
}
