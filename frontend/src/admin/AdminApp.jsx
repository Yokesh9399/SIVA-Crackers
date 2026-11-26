import { lazy, Suspense, useEffect } from 'react';
import { Link, Route, Routes } from 'react-router-dom';
import AdminRoute from './AdminRoute.jsx';
import AdminLayout from './AdminLayout.jsx';
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
              <Route index element={<DashboardPage />} />
              <Route path="products" element={<ProductsPage />} />
              <Route path="products/new" element={<ProductFormPage />} />
              <Route path="products/:id" element={<ProductViewPage />} />
              <Route path="products/:id/edit" element={<ProductFormPage />} />
              <Route path="categories" element={<CategoriesPage />} />
              <Route path="brands" element={<BrandsPage />} />
              <Route path="content" element={<ContentPage />} />
              <Route path="settings" element={<SettingsPage />} />
              <Route path="*" element={<AdminNotFound />} />
            </Route>
          </Route>
        </Routes>
      </Suspense>
    </ToastProvider>
  );
}
