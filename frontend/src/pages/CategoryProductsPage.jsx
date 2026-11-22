import { useParams } from 'react-router-dom';
import PageHeader from '../components/ui/PageHeader.jsx';
import PageLoader from '../components/ui/PageLoader.jsx';
import ErrorState from '../components/ui/ErrorState.jsx';
import CatalogueView from '../components/catalogue/CatalogueView.jsx';
import ScopeSwitcher from '../components/catalogue/ScopeSwitcher.jsx';
import NotFoundPage from './NotFoundPage.jsx';
import { useAsync } from '../hooks/useAsync.js';
import { useDocumentTitle } from '../hooks/useDocumentTitle.js';
import { categoryService } from '../services/catalogService.js';

/** /categories/:slug – the catalogue locked to one category. */
export default function CategoryProductsPage() {
  const { slug } = useParams();
  const categories = useAsync(() => categoryService.getAll(), []);
  const category = categories.data?.find((c) => c.slug === slug);
  useDocumentTitle(category?.name);

  if (categories.loading && !categories.data) return <PageLoader />;
  if (categories.error) return <ErrorState message="Could not load this category." onRetry={categories.retry} />;
  if (!category) return <NotFoundPage />;

  return (
    <>
      <PageHeader
        title={category.name}
        subtitle={category.description}
        crumbs={[{ label: 'Categories', to: '/categories' }, { label: category.name }]}
      >
        <ScopeSwitcher items={categories.data} basePath="/categories" label="Other categories" />
      </PageHeader>
      {/* key resets the catalogue state when switching category */}
      <CatalogueView key={slug} fixedCategory={slug} />
    </>
  );
}
