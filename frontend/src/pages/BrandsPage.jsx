import { Link } from 'react-router-dom';
import BrandLogo from '../components/ui/BrandLogo.jsx';
import { ArrowRight } from 'lucide-react';
import PageHeader from '../components/ui/PageHeader.jsx';
import Skeleton from '../components/ui/Skeleton.jsx';
import ErrorState from '../components/ui/ErrorState.jsx';
import DemoTag from '../components/ui/DemoTag.jsx';
import Reveal from '../components/ui/Reveal.jsx';
import { useAsync } from '../hooks/useAsync.js';
import { useDocumentTitle } from '../hooks/useDocumentTitle.js';
import { brandService } from '../services/catalogService.js';

/** /brands – every brand once (no duplicates), linking to its products. */
export default function BrandsPage() {
  useDocumentTitle('Brands');
  const brands = useAsync(() => brandService.getAll(), []);

  return (
    <>
      <PageHeader title="Brands" subtitle="Explore products by brand." crumbs={[{ label: 'Brands' }]} />
      <section className="container-page py-10" aria-label="All brands">
        {brands.error && <ErrorState onRetry={brands.retry} />}
        <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {brands.loading &&
            !brands.data &&
            Array.from({ length: 8 }, (_, i) => (
              <li key={i}>
                <Skeleton className="h-28 w-full" />
              </li>
            ))}
          {brands.data?.map((brand, i) => (
            <Reveal as="li" key={brand.id} delay={(i % 4) * 60}>
              <Link
                to={`/brands/${brand.slug}`}
                className="group flex h-full items-center gap-4 rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-200/70 transition hover:-translate-y-0.5 hover:shadow-lg hover:ring-gold-400"
              >
                <BrandLogo name={brand.name} logoUrl={brand.logoUrl} className="size-14 text-lg" />
                <span className="min-w-0 flex-1">
                  <span className="flex min-w-0 items-center gap-2">
                    <span className="min-w-0 truncate font-display font-semibold text-navy-900">{brand.name}</span>
                    <DemoTag show={brand.isDemo} />
                  </span>
                  <span className="mt-0.5 block text-sm text-slate-500">{brand.productCount} products</span>
                </span>
                <ArrowRight className="size-4 text-slate-400 transition group-hover:translate-x-0.5 group-hover:text-orange-500" aria-hidden="true" />
              </Link>
            </Reveal>
          ))}
        </ul>
      </section>
    </>
  );
}
