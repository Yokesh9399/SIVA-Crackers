import { lazy, Suspense, useEffect } from 'react';
import { Link, Navigate, Route, Routes } from 'react-router-dom';
import AdminRoute from './AdminRoute.jsx';
import AdminLayout from './AdminLayout.jsx';
import RequireRole from './RequireRole.jsx';
import { ROLES_FOR, homeFor } from './roles.js';
import { useAuth } from '../hooks/useAuth.js';
import { ToastProvider } from './context/ToastProvider.jsx';
import AdminLoginPage from './pages/AdminLoginPage.jsx';
import DashboardPage from './pages/DashboardPage.jsx';
import PageLoader from '../components/ui/PageLoader.jsx';

const ProductsPage = lazy(() => import('./pages/ProductsPage.jsx'));
const ProductFormPage = lazy(() => import('./pages/ProductFormPage.jsx'));
const ProductViewPage = lazy(() => import('./pages/ProductViewPage.jsx'));
const CategoriesPage = lazy(() => import('./pages/CategoriesPage.jsx'));
const BrandsPage = lazy(() => import('./pages/BrandsPage.jsx'));
const ContentPage = lazy(() => import('./pages/ContentPage.jsx'));
const SettingsPage = lazy(() => import('./pages/SettingsPage.jsx'));
const StaffPage = lazy(() => import('./pages/StaffPage.jsx'));
const ActivityPage = lazy(() => import('./pages/ActivityPage.jsx'));
const InventoryPage = lazy(() => import('./pages/InventoryPage.jsx'));
const StockHistoryPage = lazy(() => import('./pages/StockHistoryPage.jsx'));
const SuppliersPage = lazy(() => import('./pages/SuppliersPage.jsx'));
const ExpensesPage = lazy(() => import('./pages/ExpensesPage.jsx'));
const CustomersPage = lazy(() => import('./pages/CustomersPage.jsx'));
const ReportsPage = lazy(() => import('./pages/ReportsPage.jsx'));
const BillingPage = lazy(() => import('./pages/BillingPage.jsx'));
const BillsPage = lazy(() => import('./pages/BillsPage.jsx'));

/** /admin: the dashboard for owners and managers; cashiers start on Billing / POS. */
function AdminHome() {
  const { user } = useAuth();
  return homeFor(user?.role) === '/admin' ? <DashboardPage /> : <Navigate to={homeFor(user?.role)} replace />;
}

function AdminNotFound() {
  return (
    <div className="admin-card p-8 text-center">
      <h1 className="text-xl font-bold">Page not found</h1>
      <Link to="/admin" className="btn-primary mt-4">Back to the dashboard</Link>
    </div>
  );
}

/**
 * The OWNER's admin dashboard under /admin. Loaded only when someone opens /admin,
 * so visitors of the public website never download admin code.
 */
export default function AdminApp() {
  // Admin pages must not appear in search engines.
  useEffect(() => {
    const meta = document.createElement('meta');
    meta.name = 'robots';
    meta.content = 'noindex, nofollow';
    document.head.appendChild(meta);
    return () => meta.remove();
  }, []);

  return (
    <ToastProvider>
      <Suspense fallback={<PageLoader />}>
        <Routes>
          <Route path="login" element={<AdminLoginPage />} />
          <Route element={<AdminRoute />}>
            <Route element={<AdminLayout />}>
              <Route index element={<AdminHome />} />
              {/* Everyone on the staff: in-store billing, view stock, look up and add customers */}
              <Route path="billing" element={<BillingPage />} />
              <Route path="bills" element={<BillsPage />} />
              <Route path="inventory" element={<InventoryPage />} />
              <Route path="stock-history" element={<StockHistoryPage />} />
              <Route path="customers" element={<CustomersPage />} />
              {/* Owner and manager: run the shop */}
              <Route element={<RequireRole roles={ROLES_FOR.runShop} />}>
                <Route path="products" element={<ProductsPage />} />
                <Route path="products/new" element={<ProductFormPage />} />
                <Route path="products/:id" element={<ProductViewPage />} />
                <Route path="products/:id/edit" element={<ProductFormPage />} />
                <Route path="categories" element={<CategoriesPage />} />
                <Route path="brands" element={<BrandsPage />} />
                <Route path="suppliers" element={<SuppliersPage />} />
                <Route path="expenses" element={<ExpensesPage />} />
                <Route path="reports" element={<ReportsPage />} />
              </Route>
              {/* Owner only */}
              <Route element={<RequireRole roles={ROLES_FOR.owner} />}>
                <Route path="staff" element={<StaffPage />} />
                <Route path="activity" element={<ActivityPage />} />
                <Route path="content" element={<ContentPage />} />
                <Route path="settings" element={<SettingsPage />} />
              </Route>
              <Route path="*" element={<AdminNotFound />} />
            </Route>
          </Route>
        </Routes>
      </Suspense>
    </ToastProvider>
  );
}
