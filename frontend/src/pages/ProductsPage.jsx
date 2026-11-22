import PageHeader from '../components/ui/PageHeader.jsx';
import CatalogueView from '../components/catalogue/CatalogueView.jsx';
import { useDocumentTitle } from '../hooks/useDocumentTitle.js';

export default function ProductsPage() {
  useDocumentTitle('All Products');
  return (
    <>
      <PageHeader
        title="All Products"
        subtitle="Browse our full range with indicative prices. Search, filter by category, brand or price, and visit the shop to buy."
        crumbs={[{ label: 'All Products' }]}
      />
      <CatalogueView />
    </>
  );
}
