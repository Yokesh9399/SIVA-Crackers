import { useState } from 'react';
import { Plus, Pencil, Trash2, Search, ReceiptText } from 'lucide-react';
import { Link } from 'react-router-dom';
import AdminPageHeader from '../components/AdminPageHeader.jsx';
import ConfirmDialog from '../components/ConfirmDialog.jsx';
import Modal from '../components/Modal.jsx';
import { TextAreaField } from '../components/Fields.jsx';
import FormField, { FormMessage } from '../../components/auth/FormField.jsx';
import ErrorState from '../../components/ui/ErrorState.jsx';
import EmptyState from '../../components/ui/EmptyState.jsx';
import Skeleton from '../../components/ui/Skeleton.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import { useAuth } from '../../hooks/useAuth.js';
import { useToast } from '../hooks/useToast.js';
import { adminService } from '../adminService.js';
import { canEdit } from '../roles.js';
import { fmtDateTime, fmtMoney } from '../format.js';
import { Badge } from '../components/Badges.jsx';
import { formErrorMessage, serverFieldErrors, validateName, validatePhone } from '../../utils/authValidation.js';

const LABELS = { fullName: 'Name', phone: 'Mobile number', email: 'Email', address: 'Address', notes: 'Notes' };

export function CustomerForm({ customer, onClose, onSaved }) {
  const toast = useToast();
  const [form, setForm] = useState({
    fullName: customer?.fullName ?? '', phone: customer?.phone ?? '', email: customer?.email ?? '',
    address: customer?.address ?? '', notes: customer?.notes ?? '',
  });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  async function submit(e) {
    e.preventDefault();
    if (saving) return;
    const p = { fullName: validateName(form.fullName), phone: validatePhone(form.phone) };
    if (form.email.trim() && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(form.email.trim())) p.email = 'Please enter a valid email address.';
    if (!form.phone.trim() && !form.email.trim()) p.phone = 'Enter a mobile number or an email address.';
    setErrors(p);
    setFormError('');
    if (Object.values(p).some(Boolean)) return;
    setSaving(true);
    const body = Object.fromEntries(Object.entries(form).map(([k, v]) => [k, v.trim()]));
    try {
      const saved = customer ? await adminService.updateCustomer(customer.id, body) : await adminService.createCustomer(body);
      toast.success(customer ? `${saved.fullName}'s details were saved.` : `${saved.fullName} was added.`);
      onSaved(saved);
    } catch (err) {
      setErrors(serverFieldErrors(err, LABELS));
      setFormError(formErrorMessage(err));
      setSaving(false);
    }
  }

  return (
    <Modal
      title={customer ? 'Edit customer' : 'Add customer'}
      onClose={onClose}
      closeDisabled={saving}
      footer={
        <>
          <button type="button" className="admin-btn-secondary" onClick={onClose} disabled={saving}>Cancel</button>
          <button type="submit" form="customer-form" className="btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
        </>
      }
    >
      <form id="customer-form" onSubmit={submit} noValidate className="space-y-4">
        <FormMessage>{formError}</FormMessage>
        <p className="text-xs text-slate-500">Only save details the customer has agreed to share. This list is for the shop only – it is never shown on the website.</p>
        <FormField label="Name" value={form.fullName} onChange={set('fullName')} error={errors.fullName} maxLength={100} />
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label="Mobile number" optional type="tel" inputMode="tel" value={form.phone} onChange={set('phone')} error={errors.phone} maxLength={16} />
          <FormField label="Email" optional type="email" value={form.email} onChange={set('email')} error={errors.email} maxLength={254} />
        </div>
        <TextAreaField label="Address" optional rows={2} value={form.address} onChange={set('address')} error={errors.address} maxLength={300} />
        <TextAreaField label="Notes" optional rows={2} value={form.notes} onChange={set('notes')} error={errors.notes} maxLength={500} />
      </form>
    </Modal>
  );
}

/** Purchase history from in-store bills. Totals count finalized bills only (voided bills are not purchases). */
function PurchaseHistory({ customer, onClose }) {
  const [page, setPage] = useState(0);
  const bills = useAsync(() => adminService.customerBills(customer.id, page), [customer.id, page]);
  const d = bills.data;
  return (
    <Modal title={`Purchases – ${customer.fullName}`} onClose={onClose} size="lg">
      {bills.error ? (
        <ErrorState message={bills.error.message} onRetry={bills.retry} />
      ) : !d ? (
        <Skeleton className="h-32" />
      ) : (
        <>
          <dl className="mb-4 grid grid-cols-2 gap-3 text-sm">
            <div className="rounded-lg bg-slate-50 p-3"><dt className="text-xs text-slate-500">Total purchases</dt><dd className="text-lg font-bold">{fmtMoney(d.finalizedTotal)}</dd></div>
            <div className="rounded-lg bg-slate-50 p-3"><dt className="text-xs text-slate-500">Bills</dt><dd className="text-lg font-bold">{d.finalizedCount}{d.voidedCount > 0 && <span className="ml-1 text-xs font-normal text-slate-500">(+{d.voidedCount} voided)</span>}</dd></div>
          </dl>
          {d.page.totalElements === 0 ? (
            <p className="py-4 text-center text-sm text-slate-500">No bills for this customer yet.</p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {d.page.content.map((b) => (
                <li key={b.id} className="flex items-center justify-between gap-3 py-2 text-sm">
                  <div className="min-w-0">
                    <Link to={`/admin/bills?search=${encodeURIComponent(b.billNumber)}`} className="font-medium text-orange-600 hover:underline">{b.billNumber}</Link>
                    <p className="text-xs text-slate-500">{fmtDateTime(b.createdAt)} · {b.itemCount} items · {b.paymentMethod}</p>
                  </div>
                  <div className="shrink-0 text-right">
                    <p className={`font-semibold ${b.status === 'VOIDED' ? 'text-slate-400 line-through' : ''}`}>{fmtMoney(b.total)}</p>
                    {b.status === 'VOIDED' ? <Badge color="slate">Voided</Badge> : b.paymentStatus === 'PENDING' && <Badge color="amber">Pending</Badge>}
                  </div>
                </li>
              ))}
            </ul>
          )}
          {d.page.totalPages > 1 && (
            <div className="mt-3 flex justify-between">
              <button type="button" className="admin-btn-secondary" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Newer</button>
              <button type="button" className="admin-btn-secondary" disabled={page + 1 >= d.page.totalPages} onClick={() => setPage((p) => p + 1)}>Older</button>
            </div>
          )}
        </>
      )}
    </Modal>
  );
}

/** /admin/customers – shop contact list. Cashiers can look up; owners and managers can change. */
export default function CustomersPage() {
  const toast = useToast();
  const { user } = useAuth();
  const editable = canEdit(user?.role);
  const [search, setSearch] = useState('');
  const [reload, setReload] = useState(0);
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [history, setHistory] = useState(null);
  const [busy, setBusy] = useState(false);
  const list = useAsync(() => adminService.listCustomers(search.trim()), [search, reload]);

  async function remove() {
    setBusy(true);
    try {
      await adminService.deleteCustomer(deleting.id);
      toast.success(`${deleting.fullName} was removed.`);
      setDeleting(null);
      setReload((n) => n + 1);
    } catch (err) {
      toast.error(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <AdminPageHeader
        title="Customers"
        description="Contact details of regular customers, kept for the shop only, with their in-store purchase history."
        crumbs={[{ label: 'Customers' }]}
        actions={editable && <button type="button" className="btn-primary" onClick={() => setEditing('new')}><Plus className="size-4" aria-hidden="true" /> Add customer</button>}
      />
      <label className="relative mb-4 block max-w-md">
        <span className="sr-only">Search customers</span>
        <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" aria-hidden="true" />
        <input type="search" className="admin-input pl-9" placeholder="Search name, mobile or email…" value={search} maxLength={100} onChange={(e) => setSearch(e.target.value)} />
      </label>
      {list.error ? (
        <ErrorState message={list.error.message} onRetry={list.retry} />
      ) : !list.data ? (
        <div className="space-y-2">{Array.from({ length: 4 }, (_, i) => <Skeleton key={i} className="h-14" />)}</div>
      ) : list.data.length === 0 ? (
        <EmptyState title={search ? 'No customers match' : 'No customers yet'} message={search ? 'Try another name or number.' : editable ? 'Add customers who want the shop to keep their details.' : 'The owner or manager adds customers.'} />
      ) : (
        <ul className="grid grid-cols-1 gap-3 lg:grid-cols-2">
          {list.data.map((c) => (
            <li key={c.id} className="admin-card flex gap-3 p-4">
              <div className="min-w-0 flex-1">
                <h2 className="font-semibold break-words">{c.fullName}</h2>
                <p className="text-sm break-words text-slate-600">{[c.phone, c.email].filter(Boolean).join(' · ')}</p>
                {c.address && <p className="text-xs break-words text-slate-500">{c.address}</p>}
                {c.notes && <p className="text-xs break-words text-slate-500">{c.notes}</p>}
                <p className="text-xs text-slate-400">Added {fmtDateTime(c.createdAt)}</p>
              </div>
              <div className="flex shrink-0 gap-1">
                <button type="button" className="grid size-10 place-items-center rounded-lg text-slate-500 hover:bg-slate-100" onClick={() => setHistory(c)} aria-label={`Purchases of ${c.fullName}`}><ReceiptText className="size-4" aria-hidden="true" /></button>
              {editable && (
                <>
                  <button type="button" className="grid size-10 place-items-center rounded-lg text-slate-500 hover:bg-slate-100" onClick={() => setEditing(c)} aria-label={`Edit ${c.fullName}`}><Pencil className="size-4" aria-hidden="true" /></button>
                  <button type="button" className="grid size-10 place-items-center rounded-lg text-slate-500 hover:bg-red-50 hover:text-red-600" onClick={() => setDeleting(c)} aria-label={`Delete ${c.fullName}`}><Trash2 className="size-4" aria-hidden="true" /></button>
                </>
              )}
              </div>
            </li>
          ))}
        </ul>
      )}
      {editing && <CustomerForm customer={editing === 'new' ? null : editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); setReload((n) => n + 1); }} />}
      {history && <PurchaseHistory customer={history} onClose={() => setHistory(null)} />}
      {deleting && (
        <ConfirmDialog
          title="Delete customer?"
          message={<p>{deleting.fullName}&apos;s contact details will be deleted permanently (for example when they ask you to). This cannot be undone.</p>}
          busy={busy}
          onConfirm={remove}
          onCancel={() => setDeleting(null)}
        />
      )}
    </>
  );
}
