import { useEffect } from 'react';
import HeroCarousel from '../components/home/HeroCarousel.jsx';
import QuickTiles from '../components/home/QuickTiles.jsx';
import CategoryGrid from '../components/home/CategoryGrid.jsx';
import ProductRow from '../components/home/ProductRow.jsx';
import BrandStrip from '../components/home/BrandStrip.jsx';
import AboutBlock from '../components/home/AboutBlock.jsx';
import SafetyBlock from '../components/home/SafetyBlock.jsx';
import ContactBlock from '../components/home/ContactBlock.jsx';
import { useAsync } from '../hooks/useAsync.js';
import { useSettings } from '../hooks/useSettings.js';
import { productService } from '../services/productService.js';
import { brandService, categoryService } from '../services/catalogService.js';
import { settingsService } from '../services/settingsService.js';

export default function HomePage() {
  const { settings } = useSettings();
  const banners = useAsync(() => settingsService.getBanners(), []);
  const categories = useAsync(() => categoryService.getAll(), []);
  const featured = useAsync(() => productService.getFeatured(8), []);
  const newArrivals = useAsync(() => productService.getNewArrivals(8), []);
  const brands = useAsync(() => brandService.getAll(), []);

  useEffect(() => {
    document.title = 'SIVA Crackers | Crackers shop in Eachanari, Coimbatore';
  }, []);

  return (
    <>
      <HeroCarousel banners={banners.data} loading={banners.loading} />
      <QuickTiles />

      <CategoryGrid
        categories={categories.data}
        loading={categories.loading}
        error={categories.error}
        onRetry={categories.retry}
      />

      <ProductRow
        className="bg-white"
        eyebrow="Handpicked"
        title="Featured products"
        subtitle="Popular picks for this festive season."
        actionTo="/products"
        products={featured.data}
        loading={featured.loading}
        error={featured.error}
        onRetry={featured.retry}
      />

      <ProductRow
        eyebrow="Just added"
        title="New arrivals"
        subtitle="The latest additions to our catalogue."
        actionTo="/products?sort=newest"
        products={newArrivals.data}
        loading={newArrivals.loading}
        error={newArrivals.error}
        onRetry={newArrivals.retry}
      />

      {settings?.priceDisclaimer && (
        <p className="container-page -mt-8 pb-10 text-xs text-slate-500">
          <span className="text-orange-500">*</span> {settings.priceDisclaimer}
        </p>
      )}

      <BrandStrip brands={brands.data} loading={brands.loading} />
      <AboutBlock />
      <SafetyBlock />
      <ContactBlock />
    </>
  );
}
