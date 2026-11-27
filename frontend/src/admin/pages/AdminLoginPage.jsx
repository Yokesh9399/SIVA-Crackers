import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { LockKeyhole } from 'lucide-react';
import Logo from '../../components/layout/Logo.jsx';
import FormField, { FormMessage } from '../../components/auth/FormField.jsx';
import { useAuth } from '../../hooks/useAuth.js';
import { useDocumentTitle } from '../../hooks/useDocumentTitle.js';
import { USE_MOCKS } from '../../config/env.js';
import { STAFF_ROLES, startPageFor } from '../roles.js';

function safeAdminPath(path) {
  return typeof path === 'string' && path.startsWith('/admin') && !path.startsWith('/admin/login') ? path : '/admin';
}

/** /admin/login – staff login (OWNER, MANAGER, CASHIER); each role then sees only its own sections. */
export default function AdminLoginPage() {
  useDocumentTitle('Admin login');
  const { status, user, staffLogin, sessionExpired } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const returnTo = safeAdminPath(location.state?.from);

  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (status === 'authenticated' && STAFF_ROLES.includes(user?.role) && !submitting) return <Navigate to={startPageFor(user.role, returnTo)} replace />;

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
      const signedIn = await staffLogin({ identifier: identifier.trim(), password });
      navigate(startPageFor(signedIn?.role, returnTo), { replace: true });
    } catch (err) {
      setPassword('');
      setFormError(err.message);
      setSubmitting(false);
    }
  }

  const notice = USE_MOCKS
    ? 'The admin dashboard needs the backend. It is switched off while the website runs on sample data.'
    : location.state?.loggedOut
      ? 'You have been logged out.'
      : location.state?.expired || sessionExpired
        ? 'Your session has ended. Please log in again.'
        : status === 'authenticated' && !STAFF_ROLES.includes(user?.role)
          ? `You are logged in as ${user?.fullName} (website account). Log in with a staff account to use the admin.`
          : '';

  return (
    <main className="grid min-h-dvh place-items-center bg-gradient-to-br from-navy-950 via-navy-900 to-navy-700 px-4 py-10">
      <div className="w-full max-w-md">
        <div className="mb-6 flex justify-center">
          <Logo />
        </div>
        <form onSubmit={submit} noValidate className="space-y-4 rounded-3xl bg-white p-6 shadow-xl sm:p-8">
          <div className="flex items-center gap-3">
            <span className="grid size-11 place-items-center rounded-xl bg-navy-900 text-gold-400">
              <LockKeyhole className="size-5" aria-hidden="true" />
            </span>
            <div>
              <h1 className="text-xl font-bold">Admin login</h1>
              <p className="text-sm text-slate-500">For shop staff only.</p>
            </div>
          </div>
          {notice && <FormMessage tone="info">{notice}</FormMessage>}
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
            {submitting ? 'Logging in…' : 'Log in'}
          </button>
          <p className="text-center text-xs text-slate-500">
            After 5 wrong passwords the account is locked for 15 minutes. <Link to="/" className="font-medium text-orange-600 hover:underline">Back to the website</Link>
          </p>
        </form>
      </div>
    </main>
  );
}
