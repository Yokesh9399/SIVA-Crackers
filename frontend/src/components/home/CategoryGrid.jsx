import { Link } from 'react-router-dom';
import CategoryPicture from '../ui/CategoryPicture.jsx';
import SectionHeading from '../ui/SectionHeading.jsx';
import Skeleton from '../ui/Skeleton.jsx';
import ErrorState from '../ui/ErrorState.jsx';
import Reveal from '../ui/Reveal.jsx';

/**
 * "Shop by category" tiles. Homepage shows the heading; the Categories page
 * reuses the grid with showHeading={false} under its own page header.
 */
export default function CategoryGrid({ categories, loading, error, onRetry, showHeading = true }) {
  return (
    <section
      className={`container-page ${showHeading ? 'py-14 sm:py-20' : 'py-10'}`}
      aria-labelledby={showHeading ? 'categories-heading' : undefined}
      aria-label={showHeading ? undefined : 'All categories'}
    >
      {showHeading && (
        <SectionHeading
          eyebrow="Browse"
          title={<span id="categories-heading">Shop by category</span>}
          subtitle="From sparklers for the little ones to grand aerial shots, find what you need for your celebration."
          actionLabel="All categories"
          actionTo="/categories"
        />
      )}

      {error && <ErrorState onRetry={onRetry} />}

      <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 sm:gap-4 lg:grid-cols-6">
        {loading &&
          Array.from({ length: 12 }, (_, i) => (
            <div key={i} className="rounded-2xl bg-white p-3 ring-1 ring-slate-100">
              <Skeleton className="aspect-square w-full" />
              <Skeleton className="mx-auto mt-3 h-3 w-3/4" />
            </div>
          ))}

        {categories?.map((category, i) => (
          <Reveal key={category.id} delay={(i % 6) * 50}>
            <Link
              to={`/categories/${category.slug}`}
              className="group block h-full rounded-2xl bg-white p-2.5 text-center shadow-sm ring-1 ring-slate-200/70 transition duration-300 hover:-translate-y-1 hover:shadow-lg hover:ring-gold-400 sm:p-3"
            >
              <div className="overflow-hidden rounded-xl">
                <CategoryPicture category={category} className="aspect-square w-full transition duration-500 group-hover:scale-110" />
              </div>
              <h3 className="mt-2.5 text-xs leading-tight font-semibold text-navy-900 sm:text-sm">{category.name}</h3>
              <p className="mt-0.5 text-[0.7rem] text-slate-500">{category.productCount} products</p>
            </Link>
          </Reveal>
        ))}
      </div>
    </section>
  );
}
