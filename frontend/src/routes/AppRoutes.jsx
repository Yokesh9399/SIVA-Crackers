import { lazy, Suspense } from 'react';
import { Route, Routes } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout.jsx';
import HomePage from '../pages/HomePage.jsx';
import PageLoader from '../components/ui/PageLoader.jsx';

// Pages other than Home are lazy-loaded so the first page load stays small.
const ComingSoonPage = lazy(() => import('../pages/ComingSoonPage.jsx'));
const NotFoundPage = lazy(() => import('../pages/NotFoundPage.jsx'));
const ProductsPage = lazy(() => import('../pages/ProductsPage.jsx'));
const CategoriesPage = lazy(() => import('../pages/CategoriesPage.jsx'));
const CategoryProductsPage = lazy(() => import('../pages/CategoryProductsPage.jsx'));
const BrandsPage = lazy(() => import('../pages/BrandsPage.jsx'));
const BrandProductsPage = lazy(() => import('../pages/BrandProductsPage.jsx'));

// Routes for pages that are built in later phases. They show a friendly
// placeholder for now so every navigation link already works.
const UPCOMING = [
  { path: 'products/:slug', title: 'Product Details', phase: 3 },
  { path: 'price-list', title: 'Price List', phase: 3 },
  { path: 'about', title: 'About Us', phase: 3 },
  { path: 'contact', title: 'Contact Us', phase: 3 },
  { path: 'safety', title: 'Safety Guidelines', phase: 3 },
  { path: 'legal/:slug', title: 'Legal Information', phase: 3 },
  { path: 'login', title: 'Login', phase: 3 },
  { path: 'register', title: 'Create Account', phase: 3 },
  { path: 'estimate', title: 'My Estimate List', phase: 3 },
];

export default function AppRoutes() {
  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route element={<MainLayout />}>
          <Route index element={<HomePage />} />
          <Route path="products" element={<ProductsPage />} />
          <Route path="categories" element={<CategoriesPage />} />
          <Route path="categories/:slug" element={<CategoryProductsPage />} />
          <Route path="brands" element={<BrandsPage />} />
          <Route path="brands/:slug" element={<BrandProductsPage />} />
          {UPCOMING.map(({ path, title, phase }) => (
            <Route key={path} path={path} element={<ComingSoonPage title={title} phase={phase} />} />
          ))}
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </Suspense>
  );
}
