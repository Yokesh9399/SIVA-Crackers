import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth.js';
import PageLoader from '../ui/PageLoader.jsx';

/** Pages inside this route need a logged-in customer; others are sent to /login and brought back afterwards. */
export default function ProtectedRoute() {
  const { status, sessionExpired } = useAuth();
  const location = useLocation();

  if (status === 'loading') return <PageLoader />;
  if (status !== 'authenticated') {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search, expired: sessionExpired }} />;
  }
  return <Outlet />;
}
