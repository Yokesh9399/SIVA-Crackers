import { Link } from 'react-router-dom';
import { Package, Eye, EyeOff, Archive, Star, AlertTriangle, PackageX, Tags, Award, Plus, PanelsTopLeft, FlaskConical, Users, Truck, Wallet, ArrowDownUp, ReceiptIndianRupee, CalendarDays } from 'lucide-react';
import { fmtChange, fmtDate, fmtDateTime, fmtMoney } from '../format.js';
import { useAuth } from '../../hooks/useAuth.js';
import AdminPageHeader from '../components/AdminPageHeader.jsx';
import { AdminImage } from '../components/ImageUploader.jsx';
import { Badge, StockBadge, VisibilityBadge, DemoBadge } from '../components/Badges.jsx';
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

/** A small "latest five" list card. */
function RecentList({ title, to, linkLabel, items, empty, render }) {
  return (
    <section className="admin-card p-5" aria-label={title}>
      <div className="flex items-center justify-between gap-2">
        <h2 className="text-lg font-bold">{title}</h2>
        <Link to={to} className="text-sm font-medium text-orange-600 hover:underline">{linkLabel}</Link>
      </div>
      {items == null ? (
        <Skeleton className="mt-4 h-32" />
      ) : items.length === 0 ? (
        <p className="mt-4 text-sm text-slate-500">{empty}</p>
      ) : (
        <ul className="mt-3 divide-y divide-slate-100 text-sm">{items.map(render)}</ul>
      )}
    </section>
  );
}

/** /admin – live figures. Sales come only from finalized in-store bills; no profit is shown (no reliable cost per sale). */
export default function DashboardPage() {
  const { user } = useAuth();
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
      <AdminPageHeader title="Dashboard" description="An overview of the shop, counted live from the database. Sales are in-store bills only (nothing is sold online)." actions={actions} />

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
          Array.from({ length: 16 }, (_, i) => <Skeleton key={i} className="h-[4.5rem]" />)
        ) : (
          <>
            <StatCard icon={ReceiptIndianRupee} label={`Sales today (${d.billsToday} bills)`} value={fmtMoney(d.revenueToday)} to="/admin/bills" />
            <StatCard icon={CalendarDays} label={`Sales this month (${d.billsThisMonth} bills)`} value={fmtMoney(d.revenueThisMonth)} to="/admin/reports" />
            <StatCard icon={Package} label="Total products" value={d.totalProducts} to="/admin/products" />
            <StatCard icon={Eye} label="Active products" value={d.activeProducts} to="/admin/products?visibility=VISIBLE" />
            <StatCard icon={Archive} label="Inactive products" value={d.inactiveProducts} to="/admin/products?visibility=INACTIVE" />
            <StatCard icon={EyeOff} label="Hidden from website" value={d.hiddenProducts} to="/admin/products?visibility=HIDDEN" />
            <StatCard icon={Star} label="Featured on homepage" value={d.featuredProducts} />
            <StatCard icon={AlertTriangle} label="Few left" value={d.lowStockProducts} tone="text-amber-700" to="/admin/inventory?availability=LOW_STOCK" />
            <StatCard icon={PackageX} label="Out of stock" value={d.outOfStockProducts} tone="text-red-700" to="/admin/inventory?availability=OUT_OF_STOCK" />
            <StatCard icon={Tags} label={`Categories (${d.activeCategories} active)`} value={d.totalCategories} to="/admin/categories" />
            <StatCard icon={Award} label={`Brands (${d.activeBrands} active)`} value={d.totalBrands} to="/admin/brands" />
            <StatCard icon={Users} label={`Staff (${d.activeStaff} active)`} value={d.totalStaff} to={user?.role === 'OWNER' ? '/admin/staff' : undefined} />
            <StatCard icon={Truck} label={`Suppliers (${d.activeSuppliers} active)`} value={d.totalSuppliers} to="/admin/suppliers" />
            <StatCard icon={Wallet} label="Expenses this month" value={fmtMoney(d.expensesThisMonth)} to="/admin/expenses" />
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

      <div className="mt-6 grid gap-6 lg:grid-cols-3">
        <RecentList
          title="Recent bills" to="/admin/bills" linkLabel="All bills" items={d?.recentBills} empty="No bills yet."
          render={(b) => (
            <li key={b.id} className="flex items-center justify-between gap-2 py-2">
              <span className="min-w-0">
                <span className="block font-medium">{b.billNumber}</span>
                <span className="block truncate text-xs text-slate-500">{fmtDateTime(b.createdAt)} · {b.customerName ?? 'Walk-in'}</span>
              </span>
              <span className="shrink-0 text-right">
                <span className={`block font-semibold ${b.status === 'VOIDED' ? 'text-slate-400 line-through' : ''}`}>{fmtMoney(b.total)}</span>
                {b.status === 'VOIDED' ? <Badge color="red">Voided</Badge> : b.paymentStatus === 'PENDING' && <Badge color="amber">Pending</Badge>}
              </span>
            </li>
          )}
        />
        <RecentList
          title="Recent stock changes" to="/admin/stock-history" linkLabel="Stock movements" items={d?.recentMovements} empty="No stock changes yet."
          render={(m) => (
            <li key={m.id} className="flex items-center justify-between gap-2 py-2">
              <span className="min-w-0">
                <span className="block truncate font-medium">{m.productName}</span>
                <span className="block truncate text-xs text-slate-500">{m.reasonLabel} · {m.performedByName ?? '—'} · {fmtDateTime(m.createdAt)}</span>
              </span>
              <span className={`shrink-0 font-semibold ${m.quantityChange > 0 ? 'text-emerald-700' : 'text-red-600'}`}>{fmtChange(m.quantityChange)}</span>
            </li>
          )}
        />
        <RecentList
          title="Recent expenses" to="/admin/expenses" linkLabel="All expenses" items={d?.recentExpenses} empty="No expenses yet."
          render={(e) => (
            <li key={e.id} className="flex items-center justify-between gap-2 py-2">
              <span className="min-w-0">
                <span className="block truncate font-medium">{e.description}</span>
                <span className="block truncate text-xs text-slate-500">{e.category?.name} · {fmtDate(e.expenseDate)}</span>
              </span>
              <span className={`shrink-0 font-semibold ${e.status === 'CANCELLED' ? 'text-slate-400 line-through' : ''}`}>{fmtMoney(e.amount)}</span>
            </li>
          )}
        />
      </div>

      <section className="mt-6" aria-labelledby="quick-title">
        <h2 id="quick-title" className="mb-3 text-lg font-bold">
          Quick actions
        </h2>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {[
            { to: '/admin/billing', icon: ReceiptIndianRupee, label: 'New bill' },
            { to: '/admin/products/new', icon: Plus, label: 'Add a product' },
            { to: '/admin/inventory', icon: ArrowDownUp, label: 'Record stock' },
            { to: '/admin/expenses', icon: Wallet, label: 'Record an expense' },
            { to: '/admin/categories?new=1', icon: Tags, label: 'Add a category' },
            { to: '/admin/brands?new=1', icon: Award, label: 'Add a brand' },
            ...(user?.role === 'OWNER' ? [{ to: '/admin/content', icon: PanelsTopLeft, label: 'Edit homepage content' }] : []),
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
