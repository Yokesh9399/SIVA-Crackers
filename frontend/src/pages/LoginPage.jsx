import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { LogIn } from 'lucide-react';
import PageHeader from '../components/ui/PageHeader.jsx';
import FormField, { FormMessage } from '../components/auth/FormField.jsx';
import { useAuth } from '../hooks/useAuth.js';
import { useDocumentTitle } from '../hooks/useDocumentTitle.js';
import { USE_MOCKS } from '../config/env.js';
import { safeReturnPath } from '../utils/authValidation.js';

/** /login – customer login with email or mobile number. */
export default function LoginPage() {
  useDocumentTitle('Login');
  const { status, login, sessionExpired } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const returnTo = safeReturnPath(location.state?.from);

  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (status === 'authenticated' && !submitting) return <Navigate to={returnTo} replace />;

  async function submit(e) {
    e.preventDefault();
    const next = {
      identifier: identifier.trim() ? null : 'Please enter your email address or mobile number.',
      password: password ? null : 'Please enter your password.',
    };
    setErrors(next);
    setFormError('');
    if (next.identifier || next.password) return;

    setSubmitting(true);
    try {
      await login({ identifier: identifier.trim(), password });
      navigate(returnTo, { replace: true });
    } catch (err) {
      setPassword('');
      setFormError(err.message);
      setSubmitting(false);
    }
  }

  const expired = location.state?.expired || sessionExpired;

  return (
    <>
      <PageHeader title="Login" subtitle="Log in to your SIVA Crackers customer account." crumbs={[{ label: 'Login' }]} />
      <div className="container-page py-10 sm:py-14">
        <form
          onSubmit={submit}
          noValidate
          className="mx-auto max-w-md space-y-4 rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200/70 sm:p-8"
        >
          {USE_MOCKS && (
            <FormMessage tone="info">Accounts are not available while the website runs on local sample data.</FormMessage>
          )}
          {expired && <FormMessage tone="info">Your session has ended. Please log in again.</FormMessage>}
          <FormMessage>{formError}</FormMessage>

          <FormField
            label="Email or mobile number"
            name="identifier"
            autoComplete="username"
            value={identifier}
            onChange={(e) => setIdentifier(e.target.value)}
            error={errors.identifier}
            maxLength={254}
          />
          <FormField
            label="Password"
            type="password"
            name="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={errors.password}
            maxLength={200}
          />

          <button type="submit" className="btn-primary w-full justify-center" disabled={submitting || USE_MOCKS}>
            <LogIn className="size-4" aria-hidden="true" />
            {submitting ? 'Logging in…' : 'Log in'}
          </button>

          <p className="text-center text-sm text-slate-600">
            New here?{' '}
            <Link to="/register" state={location.state} className="font-semibold text-orange-600 hover:underline">
              Create an account
            </Link>
          </p>
          <p className="text-center text-xs text-slate-500">
            After 5 wrong passwords the account is locked for 15 minutes for your safety.
          </p>
        </form>
      </div>
    </>
  );
}
