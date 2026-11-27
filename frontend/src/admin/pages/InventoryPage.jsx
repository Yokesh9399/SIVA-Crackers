import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { ArrowDownUp, BellRing, History, PackageX, Search, SlidersHorizontal, AlertTriangle } from 'lucide-react';
import AdminPageHeader from '../components/AdminPageHeader.jsx';
import Modal from '../components/Modal.jsx';
import { SelectField, TextAreaField } from '../components/Fields.jsx';
import { AdminImage } from '../components/ImageUploader.jsx';
import { StockBadge, Badge } from '../components/Badges.jsx';
import FormField, { FormMessage } from '../../components/auth/FormField.jsx';
import Pagination from '../../components/ui/Pagination.jsx';
import ErrorState from '../../components/ui/ErrorState.jsx';
import EmptyState from '../../components/ui/EmptyState.jsx';
import Skeleton from '../../components/ui/Skeleton.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import { useAuth } from '../../hooks/useAuth.js';
import { useToast } from '../hooks/useToast.js';
import { fmtDateTime, fmtMoney, plural } from '../format.js';
import { adminService } from '../adminService.js';
import { canEdit } from '../roles.js';
import { formErrorMessage, serverFieldErrors } from '../../utils/authValidation.js';

const TYPES = [
  ['STOCK_IN', 'Stock in (received)'],
  ['STOCK_OUT', 'Stock out (leaving the shop)'],
  ['ADJUSTMENT', 'Adjust to a counted quantity'],
];
const LABELS = { quantity: 'Quantity', reason: 'Reason', supplierId: 'Supplier', unitCost: 'Unit cost', note: 'Note', reorderLevel: 'Level' };

/** Record stock in / out / adjustment for one product. */
export function MovementForm({ product, onClose, onSaved }) {
  const toast = useToast();
  const reasons = useAsync(() => adminService.movementReasons(), []);
  const suppliers = useAsync(() => adminService.listSuppliers({ active: true }), []);
  const [form, setForm] = useState({ type: 'STOCK_IN', reason: 'PURCHASE', quantity: '', supplierId: '', unitCost: '', note: '' });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const forType = (reasons.data ?? []).filter((r) => r.type === form.type);
  const supplierAllowed = form.reason === 'PURCHASE' || form.reason === 'RETURNED_TO_SUPPLIER';
  const qty = Number(form.quantity);
  const preview = !/^\d{1,7}$/.test(form.quantity)
    ? null
    : form.type === 'STOCK_IN' ? product.stockQuantity + qty : form.type === 'STOCK_OUT' ? product.stockQuantity - qty : qty;

  function changeType(e) {
    const type = e.target.value;
    const first = (reasons.data ?? []).find((r) => r.type === type);
    setForm((f) => ({ ...f, type, reason: first?.code ?? '', supplierId: '', unitCost: '' }));
  }

  async function submit(e) {
    e.preventDefault();
    if (saving) return;
    const p = {};
    if (!/^\d{1,7}$/.test(form.quantity)) p.quantity = 'Enter a whole number.';
    else if (form.type !== 'ADJUSTMENT' && qty < 1) p.quantity = 'Enter at least 1.';
    else if (form.type === 'STOCK_OUT' && qty > product.stockQuantity) p.quantity = `Only ${product.stockQuantity} in stock – stock cannot go below 0.`;
    else if (form.type === 'ADJUSTMENT' && qty === product.stockQuantity) p.quantity = 'This is already the current stock.';
    if (!form.reason) p.reason = 'Choose a reason.';
    if (form.unitCost && !/^\d{1,8}(\.\d{1,2})?$/.test(form.unitCost)) p.unitCost = 'Enter an amount with at most 2 decimal places.';
    setErrors(p);
    setFormError('');
    if (Object.keys(p).length) return;
    setSaving(true);
    try {
      const m = await adminService.recordMovement({
        productId: product.productId, reason: form.reason, quantity: qty,
        supplierId: supplierAllowed && form.supplierId ? Number(form.supplierId) : null,
        unitCost: form.reason === 'PURCHASE' && form.unitCost ? form.unitCost : null,
        note: form.note.trim() || null,
      });
      toast.success(`${product.name}: stock ${m.previousQuantity} → ${m.newQuantity}.`);
      onSaved();
    } catch (err) {
      setErrors(serverFieldErrors(err, LABELS));
      setFormError(formErrorMessage(err));
      setSaving(false);
    }
  }

  return (
    <Modal
      title={`Record stock change – ${product.name}`}
      onClose={onClose}
      closeDisabled={saving}
      footer={
        <>
          <button type="button" className="admin-btn-secondary" onClick={onClose} disabled={saving}>Cancel</button>
          <button type="submit" form="movement-form" className="btn-primary" disabled={saving || !reasons.data}>{saving ? 'Saving…' : 'Save'}</button>
        </>
      }
    >
      <form id="movement-form" onSubmit={submit} noValidate className="space-y-4">
        <FormMessage>{formError}</FormMessage>
        <p className="rounded-lg bg-slate-50 p-3 text-sm">
          Current stock: <strong>{product.stockQuantity}</strong>
          {preview !== null && (
            <>
              {' '}→ after saving: <strong className={preview < 0 ? 'text-red-600' : ''}>{preview}</strong>
            </>
          )}
        </p>
        <SelectField label="What happened?" value={form.type} onChange={changeType}>
          {TYPES.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
        </SelectField>
        <SelectField label="Reason" value={form.reason} onChange={set('reason')} error={errors.reason}>
          {forType.map((r) => <option key={r.code} value={r.code}>{r.label}</option>)}
        </SelectField>
        <FormField
          label={form.type === 'ADJUSTMENT' ? 'Correct quantity now in stock' : 'Quantity'}
          inputMode="numeric"
          value={form.quantity}
          onChange={set('quantity')}
          error={errors.quantity}
          maxLength={7}
        />
        {supplierAllowed && (
          <SelectField label="Supplier" optional value={form.supplierId} onChange={set('supplierId')} error={errors.supplierId}
            hint={suppliers.data?.length === 0 ? 'No active suppliers yet – add them on the Suppliers page.' : undefined}>
            <option value="">—</option>
            {(suppliers.data ?? []).map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </SelectField>
        )}
        {form.reason === 'PURCHASE' && (
          <FormField label="Purchase cost per unit (₹)" optional inputMode="decimal" value={form.unitCost} onChange={set('unitCost')} error={errors.unitCost}
            hint="Only for your records and the supplier report. Never shown on the website." maxLength={11} />
        )}
        <TextAreaField label="Note" optional rows={2} value={form.note} onChange={set('note')} error={errors.note} maxLength={500} />
      </form>
    </Modal>
  );
}

function ThresholdForm({ product, onClose, onSaved }) {
  const toast = useToast();
  const [level, setLevel] = useState(String(product.reorderLevel));
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  async function submit(e) {
    e.preventDefault();
    if (saving) return;
    if (!/^\d{1,6}$/.test(level)) {
      setError('Enter a whole number (0 or more).');
      return;
    }
    setSaving(true);
    try {
      await adminService.setThreshold(product.productId, Number(level));
      toast.success(`${product.name}: "few left" alert at ${level} or fewer.`);
      onSaved();
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  }

  return (
    <Modal
      title={`Low-stock level – ${product.name}`}
      onClose={onClose}
      size="sm"
      closeDisabled={saving}
      footer={
        <>
          <button type="button" className="admin-btn-secondary" onClick={onClose} disabled={saving}>Cancel</button>
          <button type="submit" form="threshold-form" className="btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
        </>
      }
    >
      <form id="threshold-form" onSubmit={submit} noValidate className="space-y-3">
        <FormField label="Alert when stock is at or below" inputMode="numeric" value={level} onChange={(e) => setLevel(e.target.value)} error={error} maxLength={6}
          hint="The website also shows “Few left” at this level. 0 = only alert when out of stock." />
      </form>
    </Modal>
  );
}

function AlertList({ title, icon: Icon, tone, rows }) {
  if (!rows?.length) return null;
  return (
    <section className={`admin-card p-4 ring-1 ${tone}`} aria-label={title}>
      <h2 className="flex items-center gap-2 font-bold"><Icon className="size-5" aria-hidden="true" /> {title} ({rows.length})</h2>
      <ul className="mt-2 flex flex-wrap gap-2">
        {rows.slice(0, 12).map((r) => (
          <li key={r.productId} className="rounded-full bg-white px-3 py-1 text-xs ring-1 ring-slate-200">
            {r.name} · <strong>{r.stockQuantity}</strong>
          </li>
        ))}
        {rows.length > 12 && <li className="px-2 py-1 text-xs text-slate-500">and {rows.length - 12} more</li>}
      </ul>
    </section>
  );
}

/** /admin/inventory – current stock and alerts. Cashiers can look; owners and managers can change stock. */
export default function InventoryPage() {
  const { user } = useAuth();
  const editable = canEdit(user?.role);
  const [params, setParams] = useSearchParams();
  const filters = {
    search: params.get('search') ?? '',
    categoryId: params.get('categoryId') ?? '',
    availability: params.get('availability') ?? '',
    sort: params.get('sort') ?? 'name,asc',
    page: Math.max(1, Number(params.get('page')) || 1),
    size: '25',
  };
  const [searchText, setSearchText] = useState(filters.search);
  const [reload, setReload] = useState(0);
  const [moving, setMoving] = useState(null);
  const [threshold, setThreshold] = useState(null);

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

  const categories = useAsync(() => (editable ? adminService.listCategories() : Promise.resolve([])), [editable]);
  const alerts = useAsync(() => adminService.stockAlerts(), [reload]);
  const list = useAsync(() => adminService.listStock({ ...filters, page: filters.page - 1 }), [filters, reload]);
  const saved = () => {
    setMoving(null);
    setThreshold(null);
    setReload((n) => n + 1);
  };

  return (
    <>
      <AdminPageHeader
        title="Inventory"
        description={editable ? 'Current stock of every product. Record stock received, stock leaving the shop and counts here – every change is kept in the stock history.' : 'Current stock of every product (view only).'}
        crumbs={[{ label: 'Inventory' }]}
        actions={
          <Link to="/admin/stock-history" className="admin-btn-secondary">
            <History className="size-4" aria-hidden="true" /> Stock Movements
          </Link>
        }
      />

      <div className="mb-4 grid gap-3 md:grid-cols-2">
        {alerts.data && (
          <>
            <button type="button" onClick={() => update({ availability: 'LOW_STOCK' })} className="admin-card flex items-center gap-3 p-4 text-left hover:ring-amber-300">
              <BellRing className="size-6 text-amber-600" aria-hidden="true" />
              <span><span className="block text-2xl font-extrabold">{alerts.data.lowStockCount}</span><span className="text-xs text-slate-500">Few left (at or below their level)</span></span>
            </button>
            <button type="button" onClick={() => update({ availability: 'OUT_OF_STOCK' })} className="admin-card flex items-center gap-3 p-4 text-left hover:ring-red-300">
              <PackageX className="size-6 text-red-600" aria-hidden="true" />
              <span><span className="block text-2xl font-extrabold">{alerts.data.outOfStockCount}</span><span className="text-xs text-slate-500">Out of stock</span></span>
            </button>
          </>
        )}
      </div>
      {alerts.data && (
        <div className="mb-4 space-y-3">
          <AlertList title="Out of stock" icon={PackageX} tone="ring-red-200" rows={alerts.data.outOfStock} />
          <AlertList title="Few left" icon={AlertTriangle} tone="ring-amber-200" rows={alerts.data.lowStock} />
        </div>
      )}

      <section aria-label="Filters" className="admin-card mb-4 grid grid-cols-1 gap-3 p-4 sm:grid-cols-2 lg:grid-cols-4">
        <label className="relative sm:col-span-2 lg:col-span-1">
          <span className="sr-only">Search by name or product code</span>
          <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" aria-hidden="true" />
          <input type="search" className="admin-input pl-9" placeholder="Search name or code…" value={searchText} maxLength={100} onChange={(e) => setSearchText(e.target.value)} />
        </label>
        {editable && (
          <select className="admin-input" aria-label="Category" value={filters.categoryId} onChange={(e) => update({ categoryId: e.target.value })}>
            <option value="">All categories</option>
            {categories.data?.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        )}
        <select className="admin-input" aria-label="Availability" value={filters.availability} onChange={(e) => update({ availability: e.target.value })}>
          <option value="">Any availability</option>
          <option value="IN_STOCK">In stock</option>
          <option value="LOW_STOCK">Few left</option>
          <option value="OUT_OF_STOCK">Out of stock</option>
        </select>
        <select className="admin-input" aria-label="Sort" value={filters.sort} onChange={(e) => update({ sort: e.target.value })}>
          <option value="name,asc">Name A–Z</option>
          <option value="name,desc">Name Z–A</option>
          <option value="default">Category order</option>
          <option value="newest">Newest first</option>
        </select>
      </section>

      {list.error ? (
        <ErrorState message={list.error.message} onRetry={list.retry} />
      ) : !list.data ? (
        <div className="space-y-2">{Array.from({ length: 6 }, (_, i) => <Skeleton key={i} className="h-16" />)}</div>
      ) : list.data.totalElements === 0 ? (
        <EmptyState title="No products match" message="Try a different search or filter." />
      ) : (
        <>
          <p className="mb-2 text-sm text-slate-600" aria-live="polite">{plural(list.data.totalElements, 'product')}</p>
          <ul className="space-y-2">
            {list.data.content.map((r) => (
              <li key={r.productId} className="admin-card flex flex-wrap items-center gap-3 p-3">
                <AdminImage src={r.imageUrl} alt="" className="size-12 shrink-0 rounded-lg" />
                <div className="min-w-0 flex-1 basis-48">
                  <p className="font-medium break-words">{r.name}</p>
                  <p className="text-xs text-slate-500">Code {r.sku} · {r.category.name} · {fmtMoney(r.price)} {!r.active && <Badge color="slate">Inactive</Badge>}</p>
                  <p className="text-xs text-slate-400">
                    {r.lastMovementAt ? <>Last updated {fmtDateTime(r.lastMovementAt)} by {r.lastMovementBy ?? '—'}</> : 'No stock changes recorded yet'}
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <div className="text-right">
                    <p className="text-xl font-extrabold">{r.stockQuantity}</p>
                    <p className="text-xs text-slate-500">alert at {r.reorderLevel}</p>
                  </div>
                  <StockBadge status={r.stockStatus} />
                </div>
                <div className="flex w-full justify-end gap-1 sm:w-auto">
                  <Link to={`/admin/stock-history?productId=${r.productId}&product=${encodeURIComponent(r.name)}`} className="grid size-10 place-items-center rounded-lg text-slate-500 hover:bg-slate-100" aria-label={`Stock history of ${r.name}`} title="History">
                    <History className="size-4" aria-hidden="true" />
                  </Link>
                  {editable && (
                    <>
                      <button type="button" className="grid size-10 place-items-center rounded-lg text-slate-500 hover:bg-slate-100" onClick={() => setThreshold(r)} aria-label={`Low-stock level of ${r.name}`} title="Low-stock level">
                        <SlidersHorizontal className="size-4" aria-hidden="true" />
                      </button>
                      <button type="button" className="btn-primary px-3 py-2" onClick={() => setMoving(r)} aria-label={`Record stock change for ${r.name}`}>
                        <ArrowDownUp className="size-4" aria-hidden="true" /> <span className="hidden sm:inline">Stock</span>
                      </button>
                    </>
                  )}
                </div>
              </li>
            ))}
          </ul>
          <div className="mt-6">
            <Pagination page={filters.page} totalPages={list.data.totalPages} onChange={(page) => update({ page })} />
          </div>
        </>
      )}
      {moving && <MovementForm product={moving} onClose={() => setMoving(null)} onSaved={saved} />}
      {threshold && <ThresholdForm product={threshold} onClose={() => setThreshold(null)} onSaved={saved} />}
    </>
  );
}
