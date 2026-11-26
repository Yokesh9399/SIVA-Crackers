import { Link } from 'react-router-dom';
import SectionHeading from '../ui/SectionHeading.jsx';
import Skeleton from '../ui/Skeleton.jsx';
import DemoTag from '../ui/DemoTag.jsx';
import BrandLogo from '../ui/BrandLogo.jsx';
import { usePrefersReducedMotion } from '../../hooks/usePrefersReducedMotion.js';

function BrandCard({ brand, hiddenCopy = false }) {
  return (
    <Link
      to={`/brands/${brand.slug}`}
      tabIndex={hiddenCopy ? -1 : undefined}
      className="flex w-60 items-center gap-3 rounded-2xl border border-white/10 bg-white/5 p-4 transition hover:border-gold-400/60 hover:bg-white/10"
    >
      <BrandLogo name={brand.name} logoUrl={brand.logoUrl} />
      <span className="min-w-0">
        <span className="block truncate font-display text-sm font-semibold text-white">{brand.name}</span>
        <span className="mt-0.5 flex items-center gap-2 text-xs text-white/60">
          {brand.productCount} products <DemoTag show={brand.isDemo} dark />
        </span>
      </span>
    </Link>
  );
}

/**
 * "Popular brands" band.
 *
 * Animated version: the list is rendered TWICE so the marquee can loop without a gap.
 * The second copy is purely visual: hidden from screen readers (aria-hidden) and from
 * keyboard focus (tabIndex -1). The real brand list (Brands page, mobile menu) is never duplicated.
 *
 * Reduced-motion version: one copy only, as a normal swipeable row.
 */
export default function BrandStrip({ brands, loading }) {
  const reducedMotion = usePrefersReducedMotion();

  return (
    <section className="overflow-hidden bg-navy-900 py-14 sm:py-16" aria-labelledby="brands-heading">
      <div className="container-page">
        <SectionHeading
          dark
          eyebrow="Brands"
          title={<span id="brands-heading">Popular brands</span>}
          subtitle="Explore products from the brands we stock."
          actionLabel="All brands"
          actionTo="/brands"
        />
      </div>

      {loading && (
        <div className="container-page flex gap-4">
          {Array.from({ length: 4 }, (_, i) => (
            <Skeleton key={i} className="h-20 w-56 bg-white/10" />
          ))}
        </div>
      )}

      {!loading && brands && reducedMotion && (
        <ul className="container-page no-scrollbar flex gap-4 overflow-x-auto">
          {brands.map((brand) => (
            <li key={brand.id} className="shrink-0">
              <BrandCard brand={brand} />
            </li>
          ))}
        </ul>
      )}

      {!loading && brands && !reducedMotion && (
        <div className="group relative">
          <div className="pointer-events-none absolute inset-y-0 left-0 z-10 w-16 bg-gradient-to-r from-navy-900 to-transparent" />
          <div className="pointer-events-none absolute inset-y-0 right-0 z-10 w-16 bg-gradient-to-l from-navy-900 to-transparent" />
          <ul className="flex w-max animate-marquee gap-4 group-hover:[animation-play-state:paused]">
            {brands.map((brand) => (
              <li key={brand.id}>
                <BrandCard brand={brand} />
              </li>
            ))}
            {/* Visual copy for a seamless loop – not a second listing. */}
            {brands.map((brand) => (
              <li key={`loop-${brand.id}`} aria-hidden="true">
                <BrandCard brand={brand} hiddenCopy />
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}
