import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import FireworksBackdrop from './FireworksBackdrop.jsx';
import Skeleton from '../ui/Skeleton.jsx';

const AUTOPLAY_MS = 6000;

const THEMES = {
  gold: 'from-navy-950 via-navy-900 to-navy-700',
  orange: 'from-navy-950 via-[#2a1840] to-[#5a2a1a]',
  blue: 'from-navy-950 via-navy-800 to-[#16426b]',
};

/** Auto-playing hero banner. Slides come from settingsService.getBanners() (admin-managed later). */
export default function HeroCarousel({ banners, loading }) {
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);
  const count = banners?.length ?? 0;

  const go = useCallback((next) => setIndex((current) => (count ? (current + next + count) % count : 0)), [count]);

  useEffect(() => {
    if (paused || count < 2) return undefined;
    const timer = setInterval(() => go(1), AUTOPLAY_MS);
    return () => clearInterval(timer);
  }, [paused, count, go]);

  if (loading || !count) {
    return (
      <section className="bg-navy-900">
        <div className="container-page py-16 sm:py-24">
          <Skeleton className="h-6 w-40 bg-white/10" />
          <Skeleton className="mt-4 h-12 w-3/4 max-w-xl bg-white/10" />
          <Skeleton className="mt-4 h-5 w-2/3 max-w-lg bg-white/10" />
        </div>
      </section>
    );
  }

  return (
    <section
      aria-roledescription="carousel"
      aria-label="Highlights"
      className="relative overflow-hidden"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      onFocus={() => setPaused(true)}
      onBlur={() => setPaused(false)}
    >
      {/* All slides sit in the same grid cell, so the tallest slide sets the height (no overflow on phones). */}
      <div className="grid min-h-[26rem] sm:min-h-[30rem] lg:min-h-[34rem]">
        {banners.map((slide, i) => {
          const active = i === index;
          return (
            <div
              key={slide.id}
              role="group"
              aria-roledescription="slide"
              aria-label={`${i + 1} of ${count}`}
              aria-hidden={!active}
              inert={!active}
              className={`relative col-start-1 row-start-1 bg-gradient-to-br ${THEMES[slide.theme] ?? THEMES.gold} transition-opacity duration-700 ${
                active ? 'z-[1] opacity-100' : 'opacity-0'
              }`}
            >
              <FireworksBackdrop variant={i} />
              {/* Extra side padding on tablets/laptops keeps text clear of the prev/next arrows. */}
              <div className="container-page relative flex h-full flex-col justify-center pt-12 pb-28 sm:pt-16 sm:pb-32 md:px-16 2xl:px-8">
                <div className={`max-w-2xl ${active ? 'animate-fade-up' : ''}`}>
                  <p className="mb-4 inline-flex items-center gap-2 rounded-full border border-gold-400/40 bg-gold-400/10 px-4 py-1.5 text-xs font-semibold tracking-[0.15em] text-gold-300 uppercase">
                    {slide.eyebrow}
                  </p>
                  <h1 className="text-4xl leading-tight font-extrabold text-white sm:text-5xl lg:text-6xl">
                    {slide.title.split(' ').slice(0, -2).join(' ')}{' '}
                    <span className="bg-gradient-to-r from-gold-300 via-gold-400 to-orange-500 bg-clip-text text-transparent">
                      {slide.title.split(' ').slice(-2).join(' ')}
                    </span>
                  </h1>
                  <p className="mt-5 max-w-xl text-base text-white/75 sm:text-lg">{slide.subtitle}</p>
                  <div className="mt-8 flex flex-wrap gap-3">
                    <Link to={slide.ctaLink} className="btn-primary px-6 py-3 text-base">
                      {slide.ctaLabel}
                    </Link>
                    <Link to="/categories" className="btn-outline px-6 py-3 text-base">
                      Shop by category
                    </Link>
                  </div>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {count > 1 && (
        <>
          <button
            type="button"
            onClick={() => go(-1)}
            className="absolute top-1/2 left-3 z-[2] hidden -translate-y-1/2 rounded-full bg-white/10 p-2.5 text-white backdrop-blur hover:bg-white/20 md:block"
            aria-label="Previous slide"
          >
            <ChevronLeft className="size-5" aria-hidden="true" />
          </button>
          <button
            type="button"
            onClick={() => go(1)}
            className="absolute top-1/2 right-3 z-[2] hidden -translate-y-1/2 rounded-full bg-white/10 p-2.5 text-white backdrop-blur hover:bg-white/20 md:block"
            aria-label="Next slide"
          >
            <ChevronRight className="size-5" aria-hidden="true" />
          </button>
          <div className="absolute inset-x-0 bottom-16 z-[2] flex justify-center gap-2 sm:bottom-20">
            {banners.map((slide, i) => (
              <button
                key={slide.id}
                type="button"
                onClick={() => setIndex(i)}
                className={`h-2 rounded-full transition-all ${i === index ? 'w-8 bg-gold-400' : 'w-2 bg-white/40 hover:bg-white/70'}`}
                aria-label={`Go to slide ${i + 1}`}
                aria-current={i === index}
              />
            ))}
          </div>
        </>
      )}
    </section>
  );
}
