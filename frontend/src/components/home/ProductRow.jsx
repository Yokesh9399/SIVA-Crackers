import ProductCard from '../product/ProductCard.jsx';
import SectionHeading from '../ui/SectionHeading.jsx';
import { ProductCardSkeleton } from '../ui/Skeleton.jsx';
import ErrorState from '../ui/ErrorState.jsx';
import Reveal from '../ui/Reveal.jsx';

/**
 * Titled row of product cards with a "View all" link.
 * Phones: horizontal swipe row. Tablets/desktop: grid.
 */
export default function ProductRow({ eyebrow, title, subtitle, actionTo, products, loading, error, onRetry, className = '' }) {
  return (
    <section className={`py-14 sm:py-16 ${className}`}>
      <div className="container-page">
        <SectionHeading eyebrow={eyebrow} title={title} subtitle={subtitle} actionLabel="View all" actionTo={actionTo} />

        {error ? (
          <ErrorState onRetry={onRetry} />
        ) : (
          <div className="no-scrollbar -mx-4 flex snap-x snap-mandatory gap-3 overflow-x-auto px-4 pb-2 sm:mx-0 sm:grid sm:grid-cols-3 sm:gap-5 sm:overflow-visible sm:px-0 lg:grid-cols-4">
            {loading
              ? Array.from({ length: 4 }, (_, i) => (
                  <div key={i} className="w-[46%] shrink-0 snap-start sm:w-auto">
                    <ProductCardSkeleton />
                  </div>
                ))
              : products?.map((product, i) => (
                  <Reveal key={product.id} delay={(i % 4) * 70} className="w-[62%] shrink-0 snap-start sm:w-auto">
                    <ProductCard product={product} />
                  </Reveal>
                ))}
          </div>
        )}
      </div>
    </section>
  );
}
