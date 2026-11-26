import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Plus, Search, Eye, Pencil, Trash2 } from 'lucide-react';
import AdminPageHeader from '../components/AdminPageHeader.jsx';
import ConfirmDialog from '../components/ConfirmDialog.jsx';
import { AdminImage } from '../components/ImageUploader.jsx';
import { StockBadge, VisibilityBadge, DemoBadge } from '../components/Badges.jsx';
import Pagination from '../../components/ui/Pagination.jsx';
import ErrorState from '../../components/ui/ErrorState.jsx';
import EmptyState from '../../components/ui/EmptyState.jsx';
import Skeleton from '../../components/ui/Skeleton.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import { useToast } from '../hooks/useToast.js';
import { adminService } from '../adminService.js';
import { formatCurrency } from '../../utils/format.js';

const SORTS = [
  ['default', 'Category order'],
  ['newest', 'Newest first'],
  ['name,asc', 'Name A–Z'],
  ['name,desc', 'Name Z–A'],
  ['price,asc', 'Price: low to high'],
  ['price,desc', 'Price: high to low'],
];

function RowActions({ product, onDelete }) {
  const btn = 'grid size-10 place-items-center rounded-lg text-slate-500 hover:bg-slate-100 hover:text-navy-900';
  return (
    <div className="flex justify-end gap-1">
      <Link to={`/admin/products/${product.id}`} className={btn} aria-label={`View ${product.name}`} title="View">
        <Eye className="size-4" aria-hidden="true" />
      </Link>
      <Link to={`/admin/products/${product.id}/edit`} className={btn} aria-label={`Edit ${product.name}`} title="Edit">
        <Pencil className="size-4" aria-hidden="true" />
      </Link>
      <button type="button" onClick={() => onDelete(product)} className={`${btn} hover:bg-red-50 hover:text-red-600`} aria-label={`Delete ${product.name}`} title="Delete">
        <Trash2 className="size-4" aria-hidden="true" />
      </button>
    </div>
  );
}

/** /admin/products – search, filter, sort and page through every product (any status). */
export default function ProductsPage() {
  const toast = useToast();
  const [params, setParams] = useSearchParams();
  const filters = {
    search: params.get('search') ?? '',
    categoryId: params.get('categoryId') ?? '',
    brandId: params.get('brandId') ?? '',
    availability: params.get('availability') ?? '',
    visibility: params.get('visibility') ?? '',
    sort: params.get('sort') ?? 'default',
    page: Math.max(1, Number(params.get('page')) || 1),
    size: params.get('size') ?? '25',
  };
  const [searchText, setSearchText] = useState(filters.search);
  const [toDelete, setToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [reload, setReload] = useState(0);

  const update = (changes) => {
    const next = new URLSearchParams(params);
    for (const [k, v] of Object.entries(changes)) {
      if (v === '' || v === null || v === undefined) next.delete(k);
      else next.set(k, String(v));
    }
    if (!('page' in changes)) next.delete('page');
    setParams(next, { replace: 'search' in changes });
  };

  // Search as you type (debounced).
  useEffect(() => {
    if (searchText === filters.search) return undefined;
    const t = setTimeout(() => update({ search: searchText.trim() }), 350);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchText]);

  const categories = useAsync(() => adminService.listCategories(), []);
  const brands = useAsync(() => adminService.listBrands(), []);
  const apiFilters = { ...filters, page: filters.page - 1 };
  const list = useAsync(() => adminService.listProducts(apiFilters), [apiFilters, reload]);
  const data = list.data;

  async function confirmDelete() {
    setDeleting(true);
    try {
      await adminService.deleteProduct(toDelete.id);
      toast.success(`“${toDelete.name}” was deleted.`);
      setToDelete(null);
      setReload((n) => n + 1);
    } catch (err) {
      toast.error(err.message);
    } finally {
      setDeleting(false);
    }
  }

  const activeFilters = ['search', 'categoryId', 'brandId', 'availability', 'visibility'].some((k) => filters[k]);

  return (
    <>
      <AdminPageHeader
        title="Products"
        description="Every product, including hidden and inactive ones. Changes appear on the website immediately."
        crumbs={[{ label: 'Products' }]}
        actions={
          <Link to="/admin/products/new" className="btn-primary">
            <Plus className="size-4" aria-hidden="true" /> Add product
          </Link>
        }
      />

      <section aria-label="Filters" className="admin-card mb-4 grid grid-cols-1 gap-3 p-4 sm:grid-cols-2 lg:grid-cols-6">
        <label className="relative sm:col-span-2">
          <span className="sr-only">Search by name or product code</span>
          <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" aria-hidden="true" />
          <input
            type="search"
            className="admin-input pl-9"
            placeholder="Search name or code…"
            value={searchText}
            maxLength={100}
            onChange={(e) => setSearchText(e.target.value)}
          />
        </label>
        <select className="admin-input" aria-label="Category" value={filters.categoryId} onChange={(e) => update({ categoryId: e.target.value })}>
          <option value="">All categories</option>
          {categories.data?.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        <select className="admin-input" aria-label="Brand" value={filters.brandId} onChange={(e) => update({ brandId: e.target.value })}>
          <option value="">All brands</option>
          {brands.data?.map((b) => (
            <option key={b.id} value={b.id}>
              {b.name}
            </option>
          ))}
        </select>
        <select className="admin-input" aria-label="Availability" value={filters.availability} onChange={(e) => update({ availability: e.target.value })}>
          <option value="">Any availability</option>
          <option value="IN_STOCK">In stock</option>
          <option value="LOW_STOCK">Few left</option>
          <option value="OUT_OF_STOCK">Out of stock</option>
        </select>
        <select className="admin-input" aria-label="Visibility" value={filters.visibility} onChange={(e) => update({ visibility: e.target.value })}>
          <option value="">Any visibility</option>
          <option value="VISIBLE">Visible</option>
          <option value="HIDDEN">Hidden</option>
          <option value="INACTIVE">Inactive</option>
        </select>
        <select className="admin-input sm:col-span-2 lg:col-span-2" aria-label="Sort" value={filters.sort} onChange={(e) => update({ sort: e.target.value })}>
          {SORTS.map(([v, l]) => (
            <option key={v} value={v}>
              Sort: {l}
            </option>
          ))}
        </select>
        <select className="admin-input" aria-label="Products per page" value={filters.size} onChange={(e) => update({ size: e.target.value })}>
          {['10', '25', '50'].map((s) => (
            <option key={s} value={s}>
              {s} per page
            </option>
          ))}
        </select>
        {activeFilters && (
          <button
            type="button"
            className="admin-btn-secondary"
            onClick={() => {
              setSearchText('');
              setParams(new URLSearchParams(filters.sort !== 'default' ? { sort: filters.sort } : {}));
            }}
          >
            Clear filters
          </button>
        )}
      </section>

      {list.error ? (
        <ErrorState message={list.error.message} onRetry={list.retry} />
      ) : list.loading && !data ? (
        <div className="space-y-2">
          {Array.from({ length: 6 }, (_, i) => (
            <Skeleton key={i} className="h-16" />
          ))}
        </div>
      ) : data.totalElements === 0 ? (
        <EmptyState
          title={activeFilters ? 'No products match these filters' : 'No products yet'}
          message={activeFilters ? 'Try a different search or clear the filters.' : 'Add your first product to show it on the website.'}
        >
          {!activeFilters && (
            <Link to="/admin/products/new" className="btn-primary mt-6">
              <Plus className="size-4" aria-hidden="true" /> Add product
            </Link>
          )}
        </EmptyState>
      ) : (
        <>
          <p className="mb-2 text-sm text-slate-600" aria-live="polite">
            {data.totalElements} {data.totalElements === 1 ? 'product' : 'products'}
            {list.loading && ' · updating…'}
          </p>

          {/* Table: tablets and up */}
          <div className="admin-card hidden overflow-x-auto md:block">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-100 text-xs tracking-wide text-slate-500 uppercase">
                <tr>
                  <th scope="col" className="px-4 py-3">Product</th>
                  <th scope="col" className="px-4 py-3">Category / Brand</th>
                  <th scope="col" className="px-4 py-3 text-right">Price</th>
                  <th scope="col" className="px-4 py-3">Availability</th>
                  <th scope="col" className="px-4 py-3">Visibility</th>
                  <th scope="col" className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {data.content.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-50/60">
                    <td className="px-4 py-3">
                      <div className="flex min-w-0 items-center gap-3">
                        <AdminImage src={p.imageUrl} alt="" className="size-12 shrink-0 rounded-lg" />
                        <div className="min-w-0">
                          <Link to={`/admin/products/${p.id}`} className="font-medium text-navy-900 hover:text-orange-600">
                            {p.name}
                          </Link>
                          <p className="text-xs text-slate-500">
                            Code {p.sku} <DemoBadge show={p.isDemo} />
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {p.category.name}
                      <span className="block text-xs text-slate-400">{p.brand.name}</span>
                    </td>
                    <td className="px-4 py-3 text-right whitespace-nowrap">
                      <span className="font-semibold">{formatCurrency(p.price)}</span>
                      <span className="block text-xs text-slate-400 line-through">{formatCurrency(p.mrp)}</span>
                    </td>
                    <td className="px-4 py-3">
                      <StockBadge status={p.stockStatus} quantity={p.stockQuantity} />
                    </td>
                    <td className="px-4 py-3">
                      <VisibilityBadge {...p} />
                    </td>
                    <td className="px-2 py-3">
                      <RowActions product={p} onDelete={setToDelete} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Cards: phones */}
          <ul className="space-y-3 md:hidden">
            {data.content.map((p) => (
              <li key={p.id} className="admin-card flex gap-3 p-3">
                <AdminImage src={p.imageUrl} alt="" className="size-16 shrink-0 rounded-lg" />
                <div className="min-w-0 flex-1">
                  <Link to={`/admin/products/${p.id}`} className="block font-medium break-words text-navy-900">
                    {p.name}
                  </Link>
                  <p className="truncate text-xs text-slate-500">
                    {p.category.name} · {p.brand.name}
                  </p>
                  <p className="mt-1 text-sm font-semibold">{formatCurrency(p.price)}</p>
                  <div className="mt-2 flex flex-wrap gap-1">
                    <StockBadge status={p.stockStatus} quantity={p.stockQuantity} />
                    <VisibilityBadge {...p} />
                    <DemoBadge show={p.isDemo} />
                  </div>
                  <RowActions product={p} onDelete={setToDelete} />
                </div>
              </li>
            ))}
          </ul>

          <div className="mt-6">
            <Pagination page={filters.page} totalPages={data.totalPages} onChange={(page) => update({ page })} />
          </div>
        </>
      )}

      {toDelete && (
        <ConfirmDialog
          title="Delete product?"
          message={
            <>
              <p>
                <strong>{toDelete.name}</strong> and its photos will be deleted permanently. This cannot be undone.
              </p>
              <p className="mt-2 text-slate-500">To take it off the website but keep it, edit it and switch off “Show on website” instead.</p>
            </>
          }
          busy={deleting}
          onConfirm={confirmDelete}
          onCancel={() => setToDelete(null)}
        />
      )}
    </>
  );
}
