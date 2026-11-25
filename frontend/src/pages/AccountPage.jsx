import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { KeyRound, LogOut, Save } from 'lucide-react';
import PageHeader from '../components/ui/PageHeader.jsx';
import FormField, { FormMessage } from '../components/auth/FormField.jsx';
import { useAuth } from '../hooks/useAuth.js';
import { useDocumentTitle } from '../hooks/useDocumentTitle.js';
import { authService } from '../services/authService.js';
import { PASSWORD_RULES, formErrorMessage, serverFieldErrors, validateName, validatePassword, validatePhone } from '../utils/authValidation.js';

const cardClass = 'space-y-4 rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200/70 sm:p-8';

function ProfileForm({ user, onSaved }) {
  const [fullName, setFullName] = useState(user.fullName);
  const [phone, setPhone] = useState(user.phone ?? '');
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState({ tone: 'error', text: '' });
  const [saving, setSaving] = useState(false);

  async function submit(e) {
    e.preventDefault();
    const next = { fullName: validateName(fullName), phone: validatePhone(phone) };
    setErrors(next);
    setMessage({ tone: 'error', text: '' });
    if (next.fullName || next.phone) return;
    setSaving(true);
    try {
      const updated = await authService.updateProfile({ fullName: fullName.trim(), phone: phone.trim() || null });
      onSaved(updated);
      setPhone(updated.phone ?? '');
      setMessage({ tone: 'success', text: 'Your details have been saved.' });
    } catch (err) {
      setErrors(serverFieldErrors(err, { fullName: 'Name', phone: 'Mobile number' }));
      setMessage({ tone: 'error', text: formErrorMessage(err) });
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate className={cardClass} aria-labelledby="profile-title">
      <h2 id="profile-title" className="text-xl font-bold">
        My details
      </h2>
      <FormMessage tone={message.tone}>{message.text}</FormMessage>
      <div>
        <p className="text-sm font-medium text-navy-900">Email</p>
        <p className="mt-1 text-sm text-slate-600">{user.email}</p>
      </div>
      <FormField label="Full name" autoComplete="name" value={fullName} onChange={(e) => setFullName(e.target.value)} error={errors.fullName} maxLength={100} />
      <FormField label="Mobile number" optional type="tel" inputMode="tel" autoComplete="tel-national" value={phone} onChange={(e) => setPhone(e.target.value)} error={errors.phone} maxLength={16} />
      <button type="submit" className="btn-primary" disabled={saving}>
        <Save className="size-4" aria-hidden="true" /> {saving ? 'Saving…' : 'Save details'}
      </button>
    </form>
  );
}

function PasswordForm({ onChanged }) {
  const empty = { currentPassword: '', newPassword: '', confirm: '' };
  const [form, setForm] = useState(empty);
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState({ tone: 'error', text: '' });
  const [saving, setSaving] = useState(false);
  const set = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  async function submit(e) {
    e.preventDefault();
    const next = {
      currentPassword: form.currentPassword ? null : 'Please enter your current password.',
      newPassword: validatePassword(form.newPassword),
      confirm: form.confirm === form.newPassword ? null : 'The two passwords do not match.',
    };
    setErrors(next);
    setMessage({ tone: 'error', text: '' });
    if (Object.values(next).some(Boolean)) return;
    setSaving(true);
    try {
      const updated = await authService.changePassword({ currentPassword: form.currentPassword, newPassword: form.newPassword });
      onChanged(updated);
      setForm(empty);
      setMessage({ tone: 'success', text: 'Password changed. You have been logged out on your other devices.' });
    } catch (err) {
      setErrors(serverFieldErrors(err, { currentPassword: 'Current password', newPassword: 'New password' }));
      setMessage({ tone: 'error', text: formErrorMessage(err) });
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate className={cardClass} aria-labelledby="password-title">
      <h2 id="password-title" className="text-xl font-bold">
        Change password
      </h2>
      <FormMessage tone={message.tone}>{message.text}</FormMessage>
      <FormField label="Current password" type="password" autoComplete="current-password" value={form.currentPassword} onChange={set('currentPassword')} error={errors.currentPassword} maxLength={200} />
      <FormField label="New password" type="password" autoComplete="new-password" value={form.newPassword} onChange={set('newPassword')} error={errors.newPassword} hint={`Use ${PASSWORD_RULES}.`} maxLength={200} />
      <FormField label="Confirm new password" type="password" autoComplete="new-password" value={form.confirm} onChange={set('confirm')} error={errors.confirm} maxLength={200} />
      <button type="submit" className="btn-primary" disabled={saving}>
        <KeyRound className="size-4" aria-hidden="true" /> {saving ? 'Changing…' : 'Change password'}
      </button>
    </form>
  );
}

/** /account – the logged-in customer's details, password and logout. */
export default function AccountPage() {
  useDocumentTitle('My Account');
  const { user, setUser, logout } = useAuth();
  const navigate = useNavigate();
  const [leaving, setLeaving] = useState(false);

  async function signOut() {
    setLeaving(true);
    // Leave the protected page first; otherwise the login guard would redirect to /login.
    navigate('/', { replace: true });
    await logout().catch(() => {});
  }

  if (!user) return null; // ProtectedRoute redirects

  return (
    <>
      <PageHeader title="My Account" subtitle={`Logged in as ${user.fullName}.`} crumbs={[{ label: 'My Account' }]}>
        <button type="button" onClick={signOut} disabled={leaving} className="btn-outline mt-5">
          <LogOut className="size-4" aria-hidden="true" /> {leaving ? 'Logging out…' : 'Log out'}
        </button>
      </PageHeader>
      <div className="container-page grid gap-6 py-10 sm:py-14 lg:grid-cols-2">
        <ProfileForm user={user} onSaved={setUser} />
        <PasswordForm onChanged={setUser} />
      </div>
    </>
  );
}
