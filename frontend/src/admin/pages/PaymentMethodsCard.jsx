import { useState } from 'react';
import { Pencil, Plus } from 'lucide-react';
import Modal from '../components/Modal.jsx';
import { ActiveBadge } from '../components/Badges.jsx';
import { SwitchField } from '../components/Fields.jsx';
import FormField, { FormMessage } from '../../components/auth/FormField.jsx';
import ErrorState from '../../components/ui/ErrorState.jsx';
import Skeleton from '../../components/ui/Skeleton.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import { useToast } from '../hooks/useToast.js';
import { adminService } from '../adminService.js';
import { formErrorMessage, serverFieldErrors } from '../../utils/authValidation.js';

const LABELS = { code: 'Code', label: 'Name', displayOrder: 'Order' };

function MethodForm({ method, onClose, onSaved }) {
  const toast = useToast();
  const [form, setForm] = useState({
    code: method?.code ?? '', label: method?.label ?? '', displayOrder: String(method?.displayOrder ?? 50), active: method?.active ?? true,
  });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  async function submit(e) {
    e.preventDefault();
    if (saving) return;
    const p = {};
    if (!/^[A-Z][A-Z0-9_]{1,19}$/.test(form.code)) p.code = 'Use 2–20 capital letters, digits or _ (for example BANK_TRANSFER).';
    if (!form.label.trim()) p.label = 'Please enter a name.';
    if (!/^\d{1,4}$/.test(form.displayOrder)) p.displayOrder = 'Enter a whole number from 0 to 9999.';
    setErrors(p);
    setFormError('');
    if (Object.keys(p).length) return;
    setSaving(true);
    const body = { code: form.code, label: form.label.trim(), displayOrder: Number(form.displayOrder), active: form.active };
    try {
      if (method) await adminService.updatePaymentMethod(method.id, body);
      else await adminService.createPaymentMethod(body);
      toast.success(`Payment method “${body.label}” saved.`);
      onSaved();
    } catch (err) {
      setErrors(serverFieldErrors(err, LABELS));
      setFormError(formErrorMessage(err));
      setSaving(false);
    }
  }

  return (
    <Modal
      title={method ? 'Edit payment method' : 'Add payment method'}
      onClose={onClose}
      closeDisabled={saving}
      footer={
        <>
          <button type="button" className="admin-btn-secondary" onClick={onClose} disabled={saving}>Cancel</button>
          <button type="submit" form="method-form" className="btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
        </>
      }
    >
      <form id="method-form" onSubmit={submit} noValidate className="space-y-4">
        <FormMessage>{formError}</FormMessage>
        <p className="text-xs text-slate-500">These are only labels for how a customer paid at the counter. Nothing is charged online.</p>
        <FormField label="Code" value={form.code} onChange={(e) => setForm((f) => ({ ...f, code: e.target.value.toUpperCase() }))} error={errors.code} maxLength={20} />
        <FormField label="Name shown on bills" value={form.label} onChange={(e) => setForm((f) => ({ ...f, label: e.target.value }))} error={errors.label} maxLength={60} />
        <FormField label="Order" inputMode="numeric" value={form.displayOrder} onChange={(e) => setForm((f) => ({ ...f, displayOrder: e.target.value }))} error={errors.displayOrder} maxLength={4} />
        <SwitchField label="Available at the counter" description="Switched-off methods stay on old bills but cannot be chosen for new ones." checked={form.active} onChange={(v) => setForm((f) => ({ ...f, active: v }))} />
      </form>
    </Modal>
  );
}

/** Owner only: the payment methods the counter can choose (Cash, UPI at the shop, …). */
export default function PaymentMethodsCard() {
  const [reload, setReload] = useState(0);
  const [editing, setEditing] = useState(null);
  const list = useAsync(() => adminService.paymentMethods(), [reload]);
  return (
    <section className="admin-card mt-6 space-y-3 p-5" aria-labelledby="methods-title">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 id="methods-title" className="text-lg font-bold">In-store payment methods</h2>
        <button type="button" className="admin-btn-secondary" onClick={() => setEditing('new')}><Plus className="size-4" aria-hidden="true" /> Add method</button>
      </div>
      {list.error ? (
        <ErrorState message={list.error.message} onRetry={list.retry} />
      ) : !list.data ? (
        <Skeleton className="h-24" />
      ) : (
        <ul className="divide-y divide-slate-100">
          {list.data.map((m) => (
            <li key={m.id} className="flex items-center gap-3 py-2 text-sm">
              <div className="min-w-0 flex-1">
                <p className="font-medium break-words">{m.label}</p>
                <p className="text-xs text-slate-500">{m.code} · order {m.displayOrder}</p>
              </div>
              <ActiveBadge active={m.active} />
              <button type="button" className="grid size-10 place-items-center rounded-lg text-slate-500 hover:bg-slate-100" onClick={() => setEditing(m)} aria-label={`Edit ${m.label}`}>
                <Pencil className="size-4" aria-hidden="true" />
              </button>
            </li>
          ))}
        </ul>
      )}
      {editing && <MethodForm method={editing === 'new' ? null : editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); setReload((n) => n + 1); }} />}
    </section>
  );
}
