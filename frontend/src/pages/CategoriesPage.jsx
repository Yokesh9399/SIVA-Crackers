import PageHeader from '../components/ui/PageHeader.jsx';
import CategoryGrid from '../components/home/CategoryGrid.jsx';
import { useAsync } from '../hooks/useAsync.js';
import { useDocumentTitle } from '../hooks/useDocumentTitle.js';
import { categoryService } from '../services/catalogService.js';

export default function CategoriesPage() {
  useDocumentTitle('Categories');
  const categories = useAsync(() => categoryService.getAll(), []);
  return (
    <>
      <PageHeader
        title="Categories"
        subtitle="Choose a category to see its products and indicative prices."
        crumbs={[{ label: 'Categories' }]}
      />
      <CategoryGrid
        showHeading={false}
        categories={categories.data}
        loading={categories.loading}
        error={categories.error}
        onRetry={categories.retry}
      />
    </>
  );
}
