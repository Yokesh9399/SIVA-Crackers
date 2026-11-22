import { Link } from 'react-router-dom';
import { Eye } from 'lucide-react';
import ProductArt from './ProductArt.jsx';
import PriceTag from './PriceTag.jsx';
import StockBadge from './StockBadge.jsx';
import WhatsAppEnquiryButton from './WhatsAppEnquiryButton.jsx';
import DemoTag from '../ui/DemoTag.jsx';
import { discountPercent } from '../../utils/format.js';

/**
 * Reusable product card (homepage rows now; catalogue, category and brand pages in Phase 2).
 * Shows: image, brand, name, category, pack size, indicative price, MRP, stock, actions.
 */
export default function ProductCard({ product }) {
  const { name, slug, brand, category, packSize, price, mrp, stockStatus, images } = product;
  const discount = discountPercent(mrp, price);
  const primaryImage = images?.[0];
  const detailPath = `/products/${slug}`;

  return (
    // `relative` keeps screen-reader-only labels inside the card, so they cannot widen the page on phones.
    <article className="group relative flex h-full flex-col overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-200/70 transition duration-300 hover:-translate-y-1 hover:shadow-xl hover:shadow-navy-900/10">
      <Link to={detailPath} className="relative block aspect-square overflow-hidden" tabIndex={-1} aria-hidden="true">
        {primaryImage ? (
          <img
            src={primaryImage.url}
            alt={primaryImage.alt ?? name}
            loading="lazy"
            className="size-full object-cover transition duration-500 group-hover:scale-105"
          />
        ) : (
          <ProductArt
            categorySlug={category.slug}
            label={`${name} illustration`}
            className="size-full transition duration-500 group-hover:scale-105"
          />
        )}
        {discount && (
          <span className="absolute top-3 left-3 rounded-full bg-orange-500 px-2.5 py-1 text-[0.7rem] font-bold text-white shadow">
            {discount}% off MRP
          </span>
        )}
        <DemoTag show={product.isDemo} className="absolute top-3 right-3 shadow" />
        <span className="absolute right-3 bottom-3 rounded-full bg-navy-900/80 px-2.5 py-1 text-[0.7rem] font-medium text-white backdrop-blur">
          {packSize}
        </span>
      </Link>

      <div className="flex flex-1 flex-col p-3.5 sm:p-4">
        <div className="flex items-center justify-between gap-2">
          <p className="truncate text-[0.7rem] font-semibold tracking-wider text-orange-600 uppercase">{brand.name}</p>
          <StockBadge status={stockStatus} />
        </div>

        <h3 className="mt-1.5 line-clamp-2 min-h-[2.5rem] text-sm leading-snug font-semibold text-navy-900 sm:text-[0.95rem]">
          <Link to={detailPath} className="hover:text-orange-600">
            {name}
          </Link>
        </h3>
        <p className="mt-1 text-xs text-slate-500">{category.name}</p>

        <div className="mt-3">
          <PriceTag price={price} mrp={mrp} />
        </div>

        <div className="mt-auto grid gap-2 pt-4">
          <Link
            to={detailPath}
            className="btn border border-navy-900/15 py-2 text-navy-900 hover:border-navy-900 hover:bg-navy-900 hover:text-white"
          >
            <Eye className="size-4" aria-hidden="true" /> View details
          </Link>
          <WhatsAppEnquiryButton product={product} label="Enquire" className="py-2" />
        </div>
      </div>
    </article>
  );
}
