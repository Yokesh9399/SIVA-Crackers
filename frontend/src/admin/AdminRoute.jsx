import { useState } from 'react';
import { Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';
import { useAuth } from '../hooks/useAuth.js';
import PageLoader from '../components/ui/PageLoader.jsx';
import { STAFF_ROLES } from './roles.js';

/**
 * Admin pages need a logged-in staff member (OWNER, MANAGER or CASHIER). Visitors go to /admin/login
 * (and come back afterwards); website customers see a "no access" message. Each page then checks the
 * role (RequireRole) and the server checks it again on every request.
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
  if (!STAFF_ROLES.includes(user.role)) {
    return (
      <main className="grid min-h-dvh place-items-center bg-slate-50 p-4">
        <div className="admin-card max-w-md p-8 text-center">
          <ShieldAlert className="mx-auto size-10 text-orange-500" aria-hidden="true" />
          <h1 className="mt-4 text-xl font-bold">No access to the admin dashboard</h1>
          <p className="mt-2 text-sm text-slate-600">
            You are logged in as {user.fullName}, a website customer account. The admin is for shop staff only.
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
