import { Link } from 'react-router-dom';
import { Package, Eye, EyeOff, Archive, Star, AlertTriangle, PackageX, Tags, Award, Plus, PanelsTopLeft, FlaskConical } from 'lucide-react';
import AdminPageHeader from '../components/AdminPageHeader.jsx';
import { AdminImage } from '../components/ImageUploader.jsx';
import { StockBadge, VisibilityBadge, DemoBadge } from '../components/Badges.jsx';
import ErrorState from '../../components/ui/ErrorState.jsx';
import Skeleton from '../../components/ui/Skeleton.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import { adminService } from '../adminService.js';
import { formatCurrency } from '../../utils/format.js';

function StatCard({ icon: Icon, label, value, to, tone = 'text-navy-900' }) {
  const body = (
    <>
      <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-gold-400/20 text-orange-600">
        <Icon className="size-5" aria-hidden="true" />
      </span>
      <span className="min-w-0">
        <span className={`block font-display text-2xl font-extrabold ${tone}`}>{value}</span>
        <span className="block text-xs font-medium text-slate-500">{label}</span>
      </span>
    </>
  );
  const cls = 'admin-card flex items-center gap-3 p-4';
  return to ? (
    <Link to={to} className={`${cls} transition hover:ring-gold-400`}>
      {body}
    </Link>
  ) : (
    <div className={cls}>{body}</div>
  );
}

/** /admin – live catalogue figures (no sales or order figures: nothing is sold online). */
export default function DashboardPage() {
  const { data: d, loading, error, retry } = useAsync(() => adminService.dashboard(), []);

  const actions = (
    <Link to="/admin/products/new" className="btn-primary">
      <Plus className="size-4" aria-hidden="true" /> Add product
    </Link>
  );

  if (error) {
    return (
      <>
        <AdminPageHeader title="Dashboard" />
        <ErrorState message={error.message} onRetry={retry} />
      </>
    );
  }

  const max = d ? Math.max(1, ...d.productsByCategory.map((c) => c.productCount)) : 1;

  return (
    <>
      <AdminPageHeader title="Dashboard" description="An overview of your catalogue, counted live from the database." actions={actions} />

      {d?.demoProducts > 0 && (
        <p className="mb-6 flex gap-2 rounded-xl bg-violet-50 p-3 text-sm text-violet-900 ring-1 ring-violet-200">
          <FlaskConical className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
          <span>
            {d.demoProducts} products are <strong>demo data</strong> (marked “Demo”). While any are visible, the website shows a
            demonstration notice. Delete or hide them once your real products are added.
          </span>
        </p>
      )}

      <section aria-label="Catalogue figures" className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-4">
        {loading || !d ? (
          Array.from({ length: 8 }, (_, i) => <Skeleton key={i} className="h-[4.5rem]" />)
        ) : (
          <>
            <StatCard icon={Package} label="Total products" value={d.totalProducts} to="/admin/products" />
            <StatCard icon={Eye} label="Active products" value={d.activeProducts} to="/admin/products?visibility=VISIBLE" />
            <StatCard icon={Archive} label="Inactive products" value={d.inactiveProducts} to="/admin/products?visibility=INACTIVE" />
            <StatCard icon={EyeOff} label="Hidden from website" value={d.hiddenProducts} to="/admin/products?visibility=HIDDEN" />
            <StatCard icon={Star} label="Featured on homepage" value={d.featuredProducts} />
            <StatCard icon={AlertTriangle} label="Few left" value={d.lowStockProducts} tone="text-amber-700" to="/admin/products?availability=LOW_STOCK" />
            <StatCard icon={PackageX} label="Out of stock" value={d.outOfStockProducts} tone="text-red-700" to="/admin/products?availability=OUT_OF_STOCK" />
            <StatCard icon={Tags} label={`Categories (${d.activeCategories} active)`} value={d.totalCategories} to="/admin/categories" />
            <StatCard icon={Award} label={`Brands (${d.activeBrands} active)`} value={d.totalBrands} to="/admin/brands" />
          </>
        )}
      </section>

      <div className="mt-6 grid gap-6 lg:grid-cols-5">
        <section className="admin-card p-5 lg:col-span-2" aria-labelledby="dist-title">
          <h2 id="dist-title" className="text-lg font-bold">
            Products by category
          </h2>
          {loading || !d ? (
            <Skeleton className="mt-4 h-48" />
          ) : d.productsByCategory.length === 0 ? (
            <p className="mt-4 text-sm text-slate-500">No categories yet.</p>
          ) : (
            <ul className="mt-4 space-y-3">
              {d.productsByCategory.map((c) => (
                <li key={c.categoryId}>
                  <div className="flex justify-between gap-2 text-sm">
                    <span className="truncate">
                      {c.name} {!c.active && <span className="text-xs text-slate-400">(inactive)</span>}
                    </span>
                    <span className="font-semibold">{c.productCount}</span>
                  </div>
                  <div className="mt-1 h-2 overflow-hidden rounded-full bg-slate-100" aria-hidden="true">
                    <div className="h-full rounded-full bg-gradient-to-r from-gold-400 to-orange-500" style={{ width: `${(c.productCount / max) * 100}%` }} />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="admin-card p-5 lg:col-span-3" aria-labelledby="recent-title">
          <div className="flex items-center justify-between gap-2">
            <h2 id="recent-title" className="text-lg font-bold">
              Recently added
            </h2>
            <Link to="/admin/products?sort=newest" className="text-sm font-medium text-orange-600 hover:underline">
              All products
            </Link>
          </div>
          {loading || !d ? (
            <Skeleton className="mt-4 h-48" />
          ) : d.recentProducts.length === 0 ? (
            <div className="mt-4 rounded-xl bg-slate-50 p-6 text-center text-sm text-slate-600">
              No products yet.{' '}
              <Link to="/admin/products/new" className="font-semibold text-orange-600 hover:underline">
                Add your first product
              </Link>
            </div>
          ) : (
            <ul className="mt-4 divide-y divide-slate-100">
              {d.recentProducts.map((p) => (
                <li key={p.id} className="flex items-center gap-3 py-3">
                  <AdminImage src={p.imageUrl} alt="" className="size-12 shrink-0 rounded-lg" />
                  <div className="min-w-0 flex-1">
                    <Link to={`/admin/products/${p.id}`} className="block truncate font-medium text-navy-900 hover:text-orange-600">
                      {p.name}
                    </Link>
                    <p className="truncate text-xs text-slate-500">
                      {p.category.name} · {p.brand.name} · {formatCurrency(p.price)}
                    </p>
                  </div>
                  <div className="hidden flex-col items-end gap-1 sm:flex">
                    <VisibilityBadge {...p} />
                    <span className="flex gap-1">
                      <StockBadge status={p.stockStatus} />
                      <DemoBadge show={p.isDemo} />
                    </span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      <section className="mt-6" aria-labelledby="quick-title">
        <h2 id="quick-title" className="mb-3 text-lg font-bold">
          Quick actions
        </h2>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
          {[
            { to: '/admin/products/new', icon: Plus, label: 'Add a product' },
            { to: '/admin/categories?new=1', icon: Tags, label: 'Add a category' },
            { to: '/admin/brands?new=1', icon: Award, label: 'Add a brand' },
            { to: '/admin/content', icon: PanelsTopLeft, label: 'Edit homepage content' },
          ].map(({ to, icon: Icon, label }) => (
            <Link key={to} to={to} className="admin-card flex items-center gap-3 p-4 font-medium transition hover:ring-gold-400">
              <Icon className="size-5 text-orange-600" aria-hidden="true" /> {label}
            </Link>
          ))}
        </div>
      </section>
    </>
  );
}
