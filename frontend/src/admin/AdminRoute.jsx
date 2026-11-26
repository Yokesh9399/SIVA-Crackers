import { useState } from 'react';
import { Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';
import { useAuth } from '../hooks/useAuth.js';
import PageLoader from '../components/ui/PageLoader.jsx';

/**
 * Admin pages need a logged-in OWNER. Visitors go to /admin/login (and come back afterwards);
 * other accounts see a "no access" message. The server checks the role again on every request.
 */
export default function AdminRoute() {
  const { status, user, logout, sessionExpired } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [leaving, setLeaving] = useState(false);

  if (status === 'loading') return <PageLoader />;
  if (status !== 'authenticated') {
    return <Navigate to="/admin/login" replace state={{ from: location.pathname + location.search, expired: sessionExpired }} />;
  }
  if (user.role !== 'OWNER') {
    return (
      <main className="grid min-h-dvh place-items-center bg-slate-50 p-4">
        <div className="admin-card max-w-md p-8 text-center">
          <ShieldAlert className="mx-auto size-10 text-orange-500" aria-hidden="true" />
          <h1 className="mt-4 text-xl font-bold">No access to the admin dashboard</h1>
          <p className="mt-2 text-sm text-slate-600">
            You are logged in as {user.fullName}. Only the shop owner&apos;s account can manage the catalogue.
          </p>
          <button
            type="button"
            className="btn-primary mt-6"
            disabled={leaving}
            onClick={async () => {
              setLeaving(true);
              await logout().catch(() => {});
              navigate('/admin/login', { replace: true });
            }}
          >
            Log out and switch account
          </button>
        </div>
      </main>
    );
  }
  return <Outlet />;
}
