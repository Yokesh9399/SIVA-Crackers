import { Link, Outlet } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';
import { useAuth } from '../hooks/useAuth.js';
import { homeFor, ROLE_LABELS } from './roles.js';

/** Shows the nested admin page only to the given roles; others see a clear "not available" message. */
export default function RequireRole({ roles }) {
  const { user } = useAuth();
  if (roles.includes(user?.role)) return <Outlet />;
  return (
    <div className="admin-card mx-auto max-w-lg p-8 text-center">
      <ShieldAlert className="mx-auto size-10 text-orange-500" aria-hidden="true" />
      <h1 className="mt-4 text-xl font-bold">This page is not available for your role</h1>
      <p className="mt-2 text-sm text-slate-600">
        You are logged in as {ROLE_LABELS[user?.role] ?? 'staff'}. Ask the shop owner if you need access.
      </p>
      <Link to={homeFor(user?.role)} className="btn-primary mt-6">
        Go to my start page
      </Link>
    </div>
  );
}
