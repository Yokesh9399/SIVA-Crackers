import { lazy, Suspense } from 'react';
import { Route, Routes } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout.jsx';
import HomePage from '../pages/HomePage.jsx';
import PageLoader from '../components/ui/PageLoader.jsx';

// Pages other than Home are lazy-loaded so the first page load stays small.
const ComingSoonPage = lazy(() => import('../pages/ComingSoonPage.jsx'));
const NotFoundPage = lazy(() => import('../pages/NotFoundPage.jsx'));
const ProductsPage = lazy(() => import('../pages/ProductsPage.jsx'));
const ProductDetailPage = lazy(() => import('../pages/ProductDetailPage.jsx'));
const CategoriesPage = lazy(() => import('../pages/CategoriesPage.jsx'));
const CategoryProductsPage = lazy(() => import('../pages/CategoryProductsPage.jsx'));
const BrandsPage = lazy(() => import('../pages/BrandsPage.jsx'));
const BrandProductsPage = lazy(() => import('../pages/BrandProductsPage.jsx'));
const PriceListPage = lazy(() => import('../pages/PriceListPage.jsx'));
const EstimatePage = lazy(() => import('../pages/EstimatePage.jsx'));
const AboutPage = lazy(() => import('../pages/AboutPage.jsx'));
const ContactPage = lazy(() => import('../pages/ContactPage.jsx'));
const SafetyPage = lazy(() => import('../pages/SafetyPage.jsx'));
const LegalPage = lazy(() => import('../pages/LegalPage.jsx'));

// Customer accounts need the real JWT backend, so they are built with it in Phase 4.
// These routes show a friendly placeholder so the Login / Signup links already work.
const UPCOMING = [
  { path: 'login', title: 'Login', phase: 4 },
  { path: 'register', title: 'Create Account', phase: 4 },
];

export default function AppRoutes() {
  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route element={<MainLayout />}>
          <Route index element={<HomePage />} />
          <Route path="products" element={<ProductsPage />} />
          <Route path="products/:slug" element={<ProductDetailPage />} />
          <Route path="categories" element={<CategoriesPage />} />
          <Route path="categories/:slug" element={<CategoryProductsPage />} />
          <Route path="brands" element={<BrandsPage />} />
          <Route path="brands/:slug" element={<BrandProductsPage />} />
          <Route path="price-list" element={<PriceListPage />} />
          <Route path="estimate" element={<EstimatePage />} />
          <Route path="about" element={<AboutPage />} />
          <Route path="contact" element={<ContactPage />} />
          <Route path="safety" element={<SafetyPage />} />
          <Route path="legal/:slug" element={<LegalPage />} />
          {UPCOMING.map(({ path, title, phase }) => (
            <Route key={path} path={path} element={<ComingSoonPage title={title} phase={phase} />} />
          ))}
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </Suspense>
  );
}
