import { useState } from 'react';
import { Plus, Pencil, Trash2, Search } from 'lucide-react';
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
import { fmtDateTime } from '../format.js';
import { formErrorMessage, serverFieldErrors, validateName, validatePhone } from '../../utils/authValidation.js';

const LABELS = { fullName: 'Name', phone: 'Mobile number', email: 'Email', address: 'Address', notes: 'Notes' };

function CustomerForm({ customer, onClose, onSaved }) {
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
      onSaved();
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

/** /admin/customers – shop contact list. Cashiers can look up; owners and managers can change. */
export default function CustomersPage() {
  const toast = useToast();
  const { user } = useAuth();
  const editable = canEdit(user?.role);
  const [search, setSearch] = useState('');
  const [reload, setReload] = useState(0);
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);
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
        description="Contact details of regular customers, kept for the shop only. (No purchase history: in-store billing is not part of the system.)"
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
              {editable && (
                <div className="flex shrink-0 gap-1">
                  <button type="button" className="grid size-10 place-items-center rounded-lg text-slate-500 hover:bg-slate-100" onClick={() => setEditing(c)} aria-label={`Edit ${c.fullName}`}><Pencil className="size-4" aria-hidden="true" /></button>
                  <button type="button" className="grid size-10 place-items-center rounded-lg text-slate-500 hover:bg-red-50 hover:text-red-600" onClick={() => setDeleting(c)} aria-label={`Delete ${c.fullName}`}><Trash2 className="size-4" aria-hidden="true" /></button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
      {editing && <CustomerForm customer={editing === 'new' ? null : editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); setReload((n) => n + 1); }} />}
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
