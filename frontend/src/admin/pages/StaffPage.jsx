import { useState } from 'react';
import { Plus, Pencil, KeyRound, Power, Search } from 'lucide-react';
import AdminPageHeader from '../components/AdminPageHeader.jsx';
import ConfirmDialog from '../components/ConfirmDialog.jsx';
import Modal from '../components/Modal.jsx';
import { SelectField } from '../components/Fields.jsx';
import { Badge } from '../components/Badges.jsx';
import FormField, { FormMessage } from '../../components/auth/FormField.jsx';
import ErrorState from '../../components/ui/ErrorState.jsx';
import EmptyState from '../../components/ui/EmptyState.jsx';
import Skeleton from '../../components/ui/Skeleton.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import { useAuth } from '../../hooks/useAuth.js';
import { useToast } from '../hooks/useToast.js';
import { adminService } from '../adminService.js';
import { fmtDateTime } from '../format.js';
import { ROLE_DESCRIPTIONS, ROLE_LABELS } from '../roles.js';
import { PASSWORD_RULES, formErrorMessage, serverFieldErrors, validateEmail, validateName, validatePassword, validatePhone } from '../../utils/authValidation.js';

const LABELS = { fullName: 'Name', email: 'Email', phone: 'Mobile number', role: 'Role', password: 'Password', newPassword: 'New password' };
const ROLE_COLOR = { OWNER: 'violet', MANAGER: 'blue', CASHIER: 'slate' };

function StaffForm({ member, onClose, onSaved }) {
  const toast = useToast();
  const { user } = useAuth();
  const editing = Boolean(member);
  const lockedRole = editing && (member.role === 'OWNER' || member.id === user?.id);
  const [form, setForm] = useState({
    fullName: member?.fullName ?? '', email: member?.email ?? '', phone: member?.phone ?? '', role: member?.role ?? 'CASHIER',
    password: '', confirm: '',
  });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  async function submit(e) {
    e.preventDefault();
    if (saving) return;
    const p = { fullName: validateName(form.fullName), email: validateEmail(form.email), phone: validatePhone(form.phone) };
    if (!editing) {
      p.password = validatePassword(form.password);
      p.confirm = form.confirm === form.password ? null : 'The two passwords do not match.';
    }
    setErrors(p);
    setFormError('');
    if (Object.values(p).some(Boolean)) return;
    setSaving(true);
    const body = { fullName: form.fullName.trim(), email: form.email.trim(), phone: form.phone.trim() || null, role: form.role };
    try {
      if (editing) await adminService.updateStaff(member.id, body);
      else await adminService.createStaff({ ...body, password: form.password });
      toast.success(editing ? `${body.fullName}'s details were saved.` : `Account created for ${body.fullName}.`);
      onSaved();
    } catch (err) {
      setErrors(serverFieldErrors(err, LABELS));
      setFormError(formErrorMessage(err));
      setSaving(false);
    }
  }

  return (
    <Modal
      title={editing ? 'Edit staff member' : 'Add staff member'}
      onClose={onClose}
      closeDisabled={saving}
      footer={
        <>
          <button type="button" className="admin-btn-secondary" onClick={onClose} disabled={saving}>Cancel</button>
          <button type="submit" form="staff-form" className="btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
        </>
      }
    >
      <form id="staff-form" onSubmit={submit} noValidate className="space-y-4">
        <FormMessage>{formError}</FormMessage>
        <FormField label="Full name" value={form.fullName} onChange={set('fullName')} error={errors.fullName} maxLength={100} />
        <FormField label="Email (used to log in)" type="email" value={form.email} onChange={set('email')} error={errors.email} maxLength={254} />
        <FormField label="Mobile number" optional type="tel" inputMode="tel" value={form.phone} onChange={set('phone')} error={errors.phone} maxLength={16} />
        {lockedRole ? (
          <p className="rounded-lg bg-slate-50 p-3 text-sm text-slate-600">
            Role: <strong>{ROLE_LABELS[member.role]}</strong>{' '}
            {member.id === user?.id ? '(you cannot change your own role)' : '(an owner account cannot be re-assigned)'}
          </p>
        ) : (
          <SelectField label="Role" value={form.role} onChange={set('role')} error={errors.role} hint={ROLE_DESCRIPTIONS[form.role]}>
            <option value="MANAGER">Manager</option>
            <option value="CASHIER">Cashier</option>
          </SelectField>
        )}
        {!editing && (
          <>
            <FormField label="Password" type="password" autoComplete="new-password" value={form.password} onChange={set('password')} error={errors.password} hint={`Use ${PASSWORD_RULES}. Give it to the person directly.`} maxLength={200} />
            <FormField label="Confirm password" type="password" autoComplete="new-password" value={form.confirm} onChange={set('confirm')} error={errors.confirm} maxLength={200} />
          </>
        )}
      </form>
    </Modal>
  );
}

function ResetPasswordForm({ member, onClose }) {
  const toast = useToast();
  const [pw, setPw] = useState('');
  const [confirm, setConfirm] = useState('');
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  async function submit(e) {
    e.preventDefault();
    if (saving) return;
    const p = { newPassword: validatePassword(pw), confirm: confirm === pw ? null : 'The two passwords do not match.' };
    setErrors(p);
    if (Object.values(p).some(Boolean)) return;
    setSaving(true);
    try {
      await adminService.resetStaffPassword(member.id, pw);
      toast.success(`Password reset for ${member.fullName}. They have been logged out everywhere.`);
      onClose();
    } catch (err) {
      setErrors(serverFieldErrors(err, LABELS));
      setFormError(formErrorMessage(err));
      setSaving(false);
    }
  }

  return (
    <Modal
      title={`Reset password – ${member.fullName}`}
      onClose={onClose}
      size="sm"
      closeDisabled={saving}
      footer={
        <>
          <button type="button" className="admin-btn-secondary" onClick={onClose} disabled={saving}>Cancel</button>
          <button type="submit" form="reset-form" className="btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Reset password'}</button>
        </>
      }
    >
      <form id="reset-form" onSubmit={submit} noValidate className="space-y-4">
        <FormMessage>{formError}</FormMessage>
        <p className="text-sm text-slate-600">Choose a temporary password and give it to {member.fullName} in person. They will be logged out of every device.</p>
        <FormField label="New password" type="password" autoComplete="new-password" value={pw} onChange={(e) => setPw(e.target.value)} error={errors.newPassword} hint={`Use ${PASSWORD_RULES}.`} maxLength={200} />
        <FormField label="Confirm new password" type="password" autoComplete="new-password" value={confirm} onChange={(e) => setConfirm(e.target.value)} error={errors.confirm} maxLength={200} />
      </form>
    </Modal>
  );
}

/** /admin/staff – OWNER only. */
export default function StaffPage() {
  const toast = useToast();
  const { user } = useAuth();
  const [filters, setFilters] = useState({ search: '', role: '', active: '' });
  const [reload, setReload] = useState(0);
  const [editing, setEditing] = useState(null);
  const [resetting, setResetting] = useState(null);
  const [toggling, setToggling] = useState(null);
  const [busy, setBusy] = useState(false);
  const list = useAsync(() => adminService.listStaff(filters), [filters, reload]);

  async function toggle() {
    setBusy(true);
    try {
      await adminService.setStaffActive(toggling.id, !toggling.active);
      toast.success(toggling.active ? `${toggling.fullName} was deactivated and logged out.` : `${toggling.fullName} can log in again.`);
      setToggling(null);
      setReload((n) => n + 1);
    } catch (err) {
      toast.error(err.message);
    } finally {
      setBusy(false);
    }
  }

  const actionBtn = 'grid size-10 place-items-center rounded-lg text-slate-500 hover:bg-slate-100 hover:text-navy-900';

  return (
    <>
      <AdminPageHeader
        title="Staff Management"
        description="Accounts for the people who work at the shop. Managers run the shop day to day; cashiers can view stock and look up customers."
        crumbs={[{ label: 'Staff Management' }]}
        actions={
          <button type="button" className="btn-primary" onClick={() => setEditing('new')}>
            <Plus className="size-4" aria-hidden="true" /> Add staff member
          </button>
        }
      />
      <section aria-label="Filters" className="admin-card mb-4 grid grid-cols-1 gap-3 p-4 sm:grid-cols-3">
        <label className="relative">
          <span className="sr-only">Search staff by name or email</span>
          <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" aria-hidden="true" />
          <input type="search" className="admin-input pl-9" placeholder="Search name or email…" value={filters.search} onChange={(e) => setFilters((f) => ({ ...f, search: e.target.value }))} />
        </label>
        <select className="admin-input" aria-label="Role" value={filters.role} onChange={(e) => setFilters((f) => ({ ...f, role: e.target.value }))}>
          <option value="">All roles</option>
          <option value="OWNER">Owner</option>
          <option value="MANAGER">Manager</option>
          <option value="CASHIER">Cashier</option>
        </select>
        <select className="admin-input" aria-label="Status" value={filters.active} onChange={(e) => setFilters((f) => ({ ...f, active: e.target.value }))}>
          <option value="">Any status</option>
          <option value="true">Active</option>
          <option value="false">Deactivated</option>
        </select>
      </section>

      {list.error ? (
        <ErrorState message={list.error.message} onRetry={list.retry} />
      ) : !list.data ? (
        <div className="space-y-2">{Array.from({ length: 4 }, (_, i) => <Skeleton key={i} className="h-16" />)}</div>
      ) : list.data.length === 0 ? (
        <EmptyState title="No staff found" message="Try a different search, or add a staff member." />
      ) : (
        <ul className="grid grid-cols-1 gap-3 lg:grid-cols-2">
          {list.data.map((m) => {
            const self = m.id === user?.id;
            const isOwner = m.role === 'OWNER';
            return (
              <li key={m.id} className="admin-card flex gap-3 p-4">
                <span className="grid size-11 shrink-0 place-items-center rounded-full bg-gold-400/30 font-display font-bold text-navy-900" aria-hidden="true">
                  {m.fullName.trim()[0]?.toUpperCase()}
                </span>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <h2 className="font-semibold break-words">{m.fullName}</h2>
                    <Badge color={ROLE_COLOR[m.role]}>{ROLE_LABELS[m.role]}</Badge>
                    {m.active ? <Badge color="green">Active</Badge> : <Badge color="red">Deactivated</Badge>}
                    {self && <Badge color="amber">You</Badge>}
                  </div>
                  <p className="truncate text-sm text-slate-600">{m.email}{m.phone ? ` · ${m.phone}` : ''}</p>
                  <p className="text-xs text-slate-500">Added {fmtDateTime(m.createdAt)} · Last login {fmtDateTime(m.lastLoginAt)}</p>
                </div>
                <div className="flex shrink-0 flex-col gap-1 sm:flex-row sm:items-start">
                  <button type="button" className={actionBtn} onClick={() => setEditing(m)} aria-label={`Edit ${m.fullName}`} title="Edit">
                    <Pencil className="size-4" aria-hidden="true" />
                  </button>
                  {!isOwner && !self && (
                    <>
                      <button type="button" className={actionBtn} onClick={() => setResetting(m)} aria-label={`Reset password for ${m.fullName}`} title="Reset password">
                        <KeyRound className="size-4" aria-hidden="true" />
                      </button>
                      <button type="button" className={`${actionBtn} ${m.active ? 'hover:bg-red-50 hover:text-red-600' : 'hover:bg-emerald-50 hover:text-emerald-700'}`}
                        onClick={() => setToggling(m)} aria-label={`${m.active ? 'Deactivate' : 'Activate'} ${m.fullName}`} title={m.active ? 'Deactivate' : 'Activate'}>
                        <Power className="size-4" aria-hidden="true" />
                      </button>
                    </>
                  )}
                </div>
              </li>
            );
          })}
        </ul>
      )}

      {editing && (
        <StaffForm
          member={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null);
            setReload((n) => n + 1);
          }}
        />
      )}
      {resetting && <ResetPasswordForm member={resetting} onClose={() => setResetting(null)} />}
      {toggling && (
        <ConfirmDialog
          title={toggling.active ? `Deactivate ${toggling.fullName}?` : `Activate ${toggling.fullName}?`}
          message={
            toggling.active ? (
              <p>{toggling.fullName} will be logged out at once and will not be able to log in until the account is activated again. Their records are kept.</p>
            ) : (
              <p>{toggling.fullName} will be able to log in again with their existing password.</p>
            )
          }
          confirmLabel={toggling.active ? 'Deactivate' : 'Activate'}
          busy={busy}
          onConfirm={toggle}
          onCancel={() => setToggling(null)}
        />
      )}
    </>
  );
}
