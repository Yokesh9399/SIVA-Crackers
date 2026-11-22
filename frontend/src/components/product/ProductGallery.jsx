import { useCallback, useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { ChevronLeft, ChevronRight, Expand, X } from 'lucide-react';
import ProductArt from './ProductArt.jsx';
import DemoTag from '../ui/DemoTag.jsx';

/**
 * Gallery for the product detail page: large image, thumbnails, keyboard
 * arrows and a full-screen viewer. Uses real photos when the product has them
 * (uploaded from the admin app in Phase 5); otherwise three illustrated views.
 */
function buildSlides(product) {
  if (product.images?.length) {
    return product.images.map((img, i) => ({ key: `img-${i}`, src: img.url, alt: img.alt ?? product.name }));
  }
  return [
    { key: 'art-0', variant: 0, alt: `${product.name} – illustration` },
    { key: 'art-1', variant: 1, alt: `${product.name} – close-up illustration` },
    { key: 'art-2', variant: 2, alt: `${product.name} – night-sky illustration` },
  ];
}

function Slide({ slide, product, className }) {
  if (slide.src) return <img src={slide.src} alt={slide.alt} className={`object-cover ${className}`} />;
  return <ProductArt categorySlug={product.category.slug} variant={slide.variant} label={slide.alt} className={className} />;
}

function Lightbox({ slides, index, product, onClose, onMove }) {
  useEffect(() => {
    const onKey = (e) => {
      if (e.key === 'Escape') onClose();
      if (e.key === 'ArrowRight') onMove(1);
      if (e.key === 'ArrowLeft') onMove(-1);
    };
    document.addEventListener('keydown', onKey);
    const previous = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = previous;
    };
  }, [onClose, onMove]);

  return createPortal(
    <div role="dialog" aria-modal="true" aria-label={`${product.name} images`} className="fixed inset-0 z-50 flex items-center justify-center bg-navy-950/95 p-4">
      <button type="button" onClick={onClose} className="absolute top-4 right-4 rounded-full bg-white/10 p-2.5 text-white hover:bg-white/20" aria-label="Close image viewer">
        <X className="size-6" aria-hidden="true" />
      </button>
      <button type="button" onClick={() => onMove(-1)} className="absolute left-3 rounded-full bg-white/10 p-3 text-white hover:bg-white/20" aria-label="Previous image">
        <ChevronLeft className="size-6" aria-hidden="true" />
      </button>
      <div className="w-full max-w-2xl overflow-hidden rounded-3xl">
        <Slide slide={slides[index]} product={product} className="aspect-square w-full" />
      </div>
      <button type="button" onClick={() => onMove(1)} className="absolute right-3 rounded-full bg-white/10 p-3 text-white hover:bg-white/20" aria-label="Next image">
        <ChevronRight className="size-6" aria-hidden="true" />
      </button>
      <p className="absolute bottom-5 text-sm text-white/70">
        {index + 1} / {slides.length}
      </p>
    </div>,
    document.body,
  );
}

export default function ProductGallery({ product, discount }) {
  const slides = buildSlides(product);
  const [index, setIndex] = useState(0);
  const [viewerOpen, setViewerOpen] = useState(false);
  const move = useCallback((step) => setIndex((i) => (i + step + slides.length) % slides.length), [slides.length]);
  const closeViewer = useCallback(() => setViewerOpen(false), []);

  return (
    <div>
      <div className="group relative overflow-hidden rounded-3xl shadow-xl shadow-navy-900/15 ring-1 ring-slate-200/70">
        <button type="button" onClick={() => setViewerOpen(true)} className="block w-full cursor-zoom-in" aria-label="Open full-size image viewer">
          <Slide slide={slides[index]} product={product} className="aspect-square w-full transition duration-500 group-hover:scale-[1.03]" />
        </button>
        {discount && (
          <span className="pointer-events-none absolute top-4 left-4 rounded-full bg-orange-500 px-3 py-1.5 text-xs font-bold text-white shadow">
            {discount}% off MRP
          </span>
        )}
        <DemoTag show={product.isDemo} className="pointer-events-none absolute top-4 right-4 shadow" />
        <span className="pointer-events-none absolute right-4 bottom-4 inline-flex items-center gap-1.5 rounded-full bg-navy-900/80 px-3 py-1.5 text-xs text-white backdrop-blur">
          <Expand className="size-3.5" aria-hidden="true" /> Tap to enlarge
        </span>
        {slides.length > 1 && (
          <>
            <button type="button" onClick={() => move(-1)} className="absolute top-1/2 left-3 -translate-y-1/2 rounded-full bg-white/85 p-2 text-navy-900 shadow hover:bg-white" aria-label="Previous image">
              <ChevronLeft className="size-5" aria-hidden="true" />
            </button>
            <button type="button" onClick={() => move(1)} className="absolute top-1/2 right-3 -translate-y-1/2 rounded-full bg-white/85 p-2 text-navy-900 shadow hover:bg-white" aria-label="Next image">
              <ChevronRight className="size-5" aria-hidden="true" />
            </button>
          </>
        )}
      </div>

      {slides.length > 1 && (
        <ul className="mt-4 grid grid-cols-4 gap-3" aria-label="Product images">
          {slides.map((slide, i) => (
            <li key={slide.key}>
              <button
                type="button"
                onClick={() => setIndex(i)}
                aria-label={`Show image ${i + 1}`}
                aria-current={i === index}
                className={`block w-full overflow-hidden rounded-xl ring-2 transition ${i === index ? 'ring-orange-500' : 'ring-transparent opacity-70 hover:opacity-100'}`}
              >
                <Slide slide={slide} product={product} className="aspect-square w-full" />
              </button>
            </li>
          ))}
        </ul>
      )}
      {!product.images?.length && <p className="mt-3 text-xs text-slate-500">Illustrations shown – product photos will be added by the shop.</p>}

      {viewerOpen && <Lightbox slides={slides} index={index} product={product} onClose={closeViewer} onMove={move} />}
    </div>
  );
}
