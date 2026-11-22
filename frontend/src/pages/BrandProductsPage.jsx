import { useParams } from 'react-router-dom';
import PageHeader from '../components/ui/PageHeader.jsx';
import PageLoader from '../components/ui/PageLoader.jsx';
import ErrorState from '../components/ui/ErrorState.jsx';
import DemoTag from '../components/ui/DemoTag.jsx';
import CatalogueView from '../components/catalogue/CatalogueView.jsx';
import ScopeSwitcher from '../components/catalogue/ScopeSwitcher.jsx';
import NotFoundPage from './NotFoundPage.jsx';
import { useAsync } from '../hooks/useAsync.js';
import { useDocumentTitle } from '../hooks/useDocumentTitle.js';
import { brandService } from '../services/catalogService.js';

/** /brands/:slug – the catalogue locked to one brand. */
export default function BrandProductsPage() {
  const { slug } = useParams();
  const brands = useAsync(() => brandService.getAll(), []);
  const brand = brands.data?.find((b) => b.slug === slug);
  useDocumentTitle(brand?.name);

  if (brands.loading && !brands.data) return <PageLoader />;
  if (brands.error) return <ErrorState message="Could not load this brand." onRetry={brands.retry} />;
  if (!brand) return <NotFoundPage />;

  return (
    <>
      <PageHeader
        title={
          <span className="inline-flex flex-wrap items-center gap-3">
            {brand.name} <DemoTag show={brand.isDemo} dark className="text-xs" />
          </span>
        }
        subtitle={brand.description}
        crumbs={[{ label: 'Brands', to: '/brands' }, { label: brand.name }]}
      >
        <ScopeSwitcher items={brands.data} basePath="/brands" label="Other brands" />
      </PageHeader>
      <CatalogueView key={slug} fixedBrand={slug} />
    </>
  );
}
