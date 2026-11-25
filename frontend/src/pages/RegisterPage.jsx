import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { UserPlus } from 'lucide-react';
import PageHeader from '../components/ui/PageHeader.jsx';
import FormField, { FormMessage } from '../components/auth/FormField.jsx';
import { useAuth } from '../hooks/useAuth.js';
import { useDocumentTitle } from '../hooks/useDocumentTitle.js';
import { USE_MOCKS } from '../config/env.js';
import {
  PASSWORD_RULES,
  formErrorMessage,
  safeReturnPath,
  serverFieldErrors,
  validateEmail,
  validateName,
  validatePassword,
  validatePhone,
} from '../utils/authValidation.js';

const LABELS = { fullName: 'Name', email: 'Email', phone: 'Mobile number', password: 'Password' };

/** /register – creates a customer account (the only kind of account anyone can create here). */
export default function RegisterPage() {
  useDocumentTitle('Create Account');
  const { status, register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const returnTo = safeReturnPath(location.state?.from);

  const [form, setForm] = useState({ fullName: '', email: '', phone: '', password: '', confirm: '' });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (status === 'authenticated' && !submitting) return <Navigate to={returnTo} replace />;

  const set = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  async function submit(e) {
    e.preventDefault();
    const next = {
      fullName: validateName(form.fullName),
      email: validateEmail(form.email),
      phone: validatePhone(form.phone),
      password: validatePassword(form.password),
      confirm: form.confirm === form.password ? null : 'The two passwords do not match.',
    };
    setErrors(next);
    setFormError('');
    if (Object.values(next).some(Boolean)) return;

    setSubmitting(true);
    try {
      await register({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        phone: form.phone.trim() || null,
        password: form.password,
      });
      navigate(returnTo, { replace: true });
    } catch (err) {
      setErrors(serverFieldErrors(err, LABELS));
      setFormError(formErrorMessage(err));
      setSubmitting(false);
    }
  }

  return (
    <>
      <PageHeader
        title="Create Account"
        subtitle="A free customer account to keep your details with SIVA Crackers."
        crumbs={[{ label: 'Create Account' }]}
      />
      <div className="container-page py-10 sm:py-14">
        <form
          onSubmit={submit}
          noValidate
          className="mx-auto max-w-md space-y-4 rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200/70 sm:p-8"
        >
          {USE_MOCKS && (
            <FormMessage tone="info">Accounts are not available while the website runs on local sample data.</FormMessage>
          )}
          <FormMessage>{formError}</FormMessage>

          <FormField label="Full name" name="fullName" autoComplete="name" value={form.fullName} onChange={set('fullName')} error={errors.fullName} maxLength={100} />
          <FormField label="Email" type="email" name="email" autoComplete="email" value={form.email} onChange={set('email')} error={errors.email} maxLength={254} />
          <FormField
            label="Mobile number"
            optional
            type="tel"
            name="phone"
            autoComplete="tel-national"
            inputMode="tel"
            value={form.phone}
            onChange={set('phone')}
            error={errors.phone}
            hint="10-digit Indian mobile number; you can also log in with it."
            maxLength={16}
          />
          <FormField
            label="Password"
            type="password"
            name="password"
            autoComplete="new-password"
            value={form.password}
            onChange={set('password')}
            error={errors.password}
            hint={`Use ${PASSWORD_RULES}.`}
            maxLength={200}
          />
          <FormField label="Confirm password" type="password" name="confirm" autoComplete="new-password" value={form.confirm} onChange={set('confirm')} error={errors.confirm} maxLength={200} />

          <button type="submit" className="btn-primary w-full justify-center" disabled={submitting || USE_MOCKS}>
            <UserPlus className="size-4" aria-hidden="true" />
            {submitting ? 'Creating account…' : 'Create account'}
          </button>

          <p className="text-xs text-slate-500">
            SIVA Crackers does not sell, book, deliver or accept payment for firecrackers online. Your Estimate List stays in
            this browser only. See our{' '}
            <Link to="/legal/privacy" className="font-medium text-orange-600 hover:underline">
              Privacy Policy
            </Link>
            .
          </p>
          <p className="text-center text-sm text-slate-600">
            Already have an account?{' '}
            <Link to="/login" state={location.state} className="font-semibold text-orange-600 hover:underline">
              Log in
            </Link>
          </p>
        </form>
      </div>
    </>
  );
}
