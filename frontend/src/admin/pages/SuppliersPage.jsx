import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Plus, Pencil, Power, Search, Eye } from 'lucide-react';
import AdminPageHeader from '../components/AdminPageHeader.jsx';
import ConfirmDialog from '../components/ConfirmDialog.jsx';
import Modal from '../components/Modal.jsx';
import { TextAreaField } from '../components/Fields.jsx';
import { ActiveBadge } from '../components/Badges.jsx';
import FormField, { FormMessage } from '../../components/auth/FormField.jsx';
import ErrorState from '../../components/ui/ErrorState.jsx';
import EmptyState from '../../components/ui/EmptyState.jsx';
import Skeleton from '../../components/ui/Skeleton.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import { useToast } from '../hooks/useToast.js';
import { adminService } from '../adminService.js';
import { fmtDateTime, fmtMoney } from '../format.js';
import { formErrorMessage, serverFieldErrors, validatePhone } from '../../utils/authValidation.js';

const LABELS = { name: 'Name', contactPerson: 'Contact person', phone: 'Phone', email: 'Email', address: 'Address', notes: 'Notes' };

function SupplierForm({ supplier, onClose, onSaved }) {
  const toast = useToast();
  const [form, setForm] = useState({
    name: supplier?.name ?? '', contactPerson: supplier?.contactPerson ?? '', phone: supplier?.phone ?? '',
    email: supplier?.email ?? '', address: supplier?.address ?? '', notes: supplier?.notes ?? '',
  });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  async function submit(e) {
    e.preventDefault();
    if (saving) return;
    const p = {};
    if (!form.name.trim()) p.name = 'Please enter the supplier name.';
    const phone = validatePhone(form.phone);
    if (phone) p.phone = phone;
    if (form.email.trim() && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(form.email.trim())) p.email = 'Please enter a valid email address.';
    setErrors(p);
    setFormError('');
    if (Object.keys(p).length) return;
    setSaving(true);
    const body = Object.fromEntries(Object.entries(form).map(([k, v]) => [k, v.trim()]));
    try {
      const saved = supplier ? await adminService.updateSupplier(supplier.id, body) : await adminService.createSupplier(body);
      toast.success(supplier ? `Supplier “${saved.name}” was saved.` : `Supplier “${saved.name}” was added.`);
      onSaved();
    } catch (err) {
      setErrors(serverFieldErrors(err, LABELS));
      setFormError(formErrorMessage(err));
      setSaving(false);
    }
  }

  return (
    <Modal
      title={supplier ? 'Edit supplier' : 'Add supplier'}
      onClose={onClose}
      closeDisabled={saving}
      footer={
        <>
          <button type="button" className="admin-btn-secondary" onClick={onClose} disabled={saving}>Cancel</button>
          <button type="submit" form="supplier-form" className="btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
        </>
      }
    >
      <form id="supplier-form" onSubmit={submit} noValidate className="space-y-4">
        <FormMessage>{formError}</FormMessage>
        <FormField label="Supplier name" value={form.name} onChange={set('name')} error={errors.name} maxLength={120} />
        <FormField label="Contact person" optional value={form.contactPerson} onChange={set('contactPerson')} error={errors.contactPerson} maxLength={100} />
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label="Phone" optional type="tel" inputMode="tel" value={form.phone} onChange={set('phone')} error={errors.phone} maxLength={16} />
          <FormField label="Email" optional type="email" value={form.email} onChange={set('email')} error={errors.email} maxLength={254} />
        </div>
        <TextAreaField label="Address" optional rows={2} value={form.address} onChange={set('address')} error={errors.address} maxLength={300} />
        <TextAreaField label="Notes" optional rows={3} value={form.notes} onChange={set('notes')} error={errors.notes} maxLength={1000} />
      </form>
    </Modal>
  );
}

function SupplierDetail({ id, onClose }) {
  const detail = useAsync(() => adminService.getSupplier(id), [id]);
  const d = detail.data;
  return (
    <Modal title={d ? d.supplier.name : 'Supplier'} onClose={onClose} footer={<button type="button" className="admin-btn-secondary" onClick={onClose}>Close</button>}>
      {detail.error ? (
        <ErrorState message={detail.error.message} onRetry={detail.retry} />
      ) : !d ? (
        <Skeleton className="h-40" />
      ) : (
        <div className="space-y-4 text-sm">
          <dl className="grid grid-cols-3 gap-2">
            <dt className="text-slate-500">Contact</dt><dd className="col-span-2 break-words">{d.supplier.contactPerson || '—'}</dd>
            <dt className="text-slate-500">Phone</dt><dd className="col-span-2">{d.supplier.phone || '—'}</dd>
            <dt className="text-slate-500">Email</dt><dd className="col-span-2 break-words">{d.supplier.email || '—'}</dd>
            <dt className="text-slate-500">Address</dt><dd className="col-span-2 break-words">{d.supplier.address || '—'}</dd>
            <dt className="text-slate-500">Notes</dt><dd className="col-span-2 break-words whitespace-pre-line">{d.supplier.notes || '—'}</dd>
            <dt className="text-slate-500">Added</dt><dd className="col-span-2">{fmtDateTime(d.supplier.createdAt)}</dd>
            <dt className="text-slate-500">Last changed</dt><dd className="col-span-2">{fmtDateTime(d.supplier.updatedAt)}</dd>
          </dl>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            {[['Deliveries', d.deliveries], ['Units received', d.unitsReceived], ['Purchase value', fmtMoney(d.purchaseValue)], ['Last delivery', fmtDateTime(d.lastDeliveryAt)]].map(([l, v]) => (
              <div key={l} className="rounded-xl bg-slate-50 p-3">
                <p className="font-bold">{v}</p>
                <p className="text-xs text-slate-500">{l}</p>
              </div>
            ))}
          </div>
          <p className="text-xs text-slate-500">Purchase value counts only deliveries where a cost per unit was entered.</p>
          <Link to={`/admin/stock-history?supplierId=${id}&supplier=${encodeURIComponent(d.supplier.name)}`} className="btn-primary">Purchase history</Link>
        </div>
      )}
    </Modal>
  );
}

/** /admin/suppliers – OWNER and MANAGER. */
export default function SuppliersPage() {
  const toast = useToast();
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [reload, setReload] = useState(0);
  const [editing, setEditing] = useState(null);
  const [viewing, setViewing] = useState(null);
  const [toggling, setToggling] = useState(null);
  const [busy, setBusy] = useState(false);
  const list = useAsync(() => adminService.listSuppliers({ search, active: status }), [search, status, reload]);

  async function toggle() {
    setBusy(true);
    try {
      await adminService.setSupplierActive(toggling.id, !toggling.active);
      toast.success(toggling.active ? `“${toggling.name}” was deactivated.` : `“${toggling.name}” is active again.`);
      setToggling(null);
      setReload((n) => n + 1);
    } catch (err) {
      toast.error(err.message);
    } finally {
      setBusy(false);
    }
  }

  const btn = 'grid size-10 place-items-center rounded-lg text-slate-500 hover:bg-slate-100 hover:text-navy-900';
  return (
    <>
      <AdminPageHeader
        title="Suppliers"
        description="Where your stock comes from. Suppliers are never deleted – deactivate one you no longer use."
        crumbs={[{ label: 'Suppliers' }]}
        actions={<button type="button" className="btn-primary" onClick={() => setEditing('new')}><Plus className="size-4" aria-hidden="true" /> Add supplier</button>}
      />
      <section aria-label="Filters" className="admin-card mb-4 grid grid-cols-1 gap-3 p-4 sm:grid-cols-2">
        <label className="relative">
          <span className="sr-only">Search suppliers</span>
          <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" aria-hidden="true" />
          <input type="search" className="admin-input pl-9" placeholder="Search name, contact, phone or email…" value={search} onChange={(e) => setSearch(e.target.value)} />
        </label>
        <select className="admin-input" aria-label="Status" value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">Active and inactive</option>
          <option value="true">Active only</option>
          <option value="false">Inactive only</option>
        </select>
      </section>
      {list.error ? (
        <ErrorState message={list.error.message} onRetry={list.retry} />
      ) : !list.data ? (
        <div className="space-y-2">{Array.from({ length: 4 }, (_, i) => <Skeleton key={i} className="h-16" />)}</div>
      ) : list.data.length === 0 ? (
        <EmptyState title={search || status ? 'No suppliers match' : 'No suppliers yet'} message={search || status ? 'Try a different search.' : 'Add the companies you buy stock from.'} />
      ) : (
        <ul className="grid grid-cols-1 gap-3 lg:grid-cols-2">
          {list.data.map((s) => (
            <li key={s.id} className="admin-card flex gap-3 p-4">
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <h2 className="font-semibold break-words">{s.name}</h2>
                  <ActiveBadge active={s.active} />
                </div>
                <p className="text-sm break-words text-slate-600">{[s.contactPerson, s.phone, s.email].filter(Boolean).join(' · ') || 'No contact details yet'}</p>
                <p className="text-xs text-slate-400">Added {fmtDateTime(s.createdAt)}</p>
              </div>
              <div className="flex shrink-0 gap-1">
                <button type="button" className={btn} onClick={() => setViewing(s.id)} aria-label={`View ${s.name}`} title="Details"><Eye className="size-4" aria-hidden="true" /></button>
                <button type="button" className={btn} onClick={() => setEditing(s)} aria-label={`Edit ${s.name}`} title="Edit"><Pencil className="size-4" aria-hidden="true" /></button>
                <button type="button" className={btn} onClick={() => setToggling(s)} aria-label={`${s.active ? 'Deactivate' : 'Activate'} ${s.name}`} title={s.active ? 'Deactivate' : 'Activate'}><Power className="size-4" aria-hidden="true" /></button>
              </div>
            </li>
          ))}
        </ul>
      )}
      {editing && <SupplierForm supplier={editing === 'new' ? null : editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); setReload((n) => n + 1); }} />}
      {viewing && <SupplierDetail id={viewing} onClose={() => setViewing(null)} />}
      {toggling && (
        <ConfirmDialog
          title={toggling.active ? `Deactivate “${toggling.name}”?` : `Activate “${toggling.name}”?`}
          message={<p>{toggling.active ? 'It will no longer be offered when recording stock received. Its purchase history is kept.' : 'It will be offered again when recording stock received.'}</p>}
          confirmLabel={toggling.active ? 'Deactivate' : 'Activate'}
          busy={busy}
          onConfirm={toggle}
          onCancel={() => setToggling(null)}
        />
      )}
    </>
  );
}
