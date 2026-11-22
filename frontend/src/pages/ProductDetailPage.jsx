import { Link, useParams } from 'react-router-dom';
import { AlertTriangle, Info, ShieldCheck, Store } from 'lucide-react';
import Breadcrumbs from '../components/ui/Breadcrumbs.jsx';
import PageLoader from '../components/ui/PageLoader.jsx';
import ErrorState from '../components/ui/ErrorState.jsx';
import DemoTag from '../components/ui/DemoTag.jsx';
import ProductGallery from '../components/product/ProductGallery.jsx';
import PriceTag from '../components/product/PriceTag.jsx';
import StockBadge from '../components/product/StockBadge.jsx';
import WhatsAppEnquiryButton from '../components/product/WhatsAppEnquiryButton.jsx';
import AddToEstimate from '../components/estimate/AddToEstimate.jsx';
import ProductRow from '../components/home/ProductRow.jsx';
import NotFoundPage from './NotFoundPage.jsx';
import { useAsync } from '../hooks/useAsync.js';
import { useDocumentTitle } from '../hooks/useDocumentTitle.js';
import { useSettings } from '../hooks/useSettings.js';
import { productService } from '../services/productService.js';
import { discountPercent, formatCurrency, formatUnit } from '../utils/format.js';

const STOCK_TEXT = {
  IN_STOCK: 'Available at the shop',
  LOW_STOCK: 'Only a few left at the shop',
  OUT_OF_STOCK: 'Currently out of stock at the shop',
};

function DetailRow({ label, children }) {
  return (
    <div className="flex justify-between gap-4 border-b border-slate-100 py-2.5 text-sm last:border-0">
      <dt className="text-slate-500">{label}</dt>
      <dd className="text-right font-medium text-navy-900">{children}</dd>
    </div>
  );
}

/** /products/:slug */
export default function ProductDetailPage() {
  const { slug } = useParams();
  const { settings } = useSettings();
  const product = useAsync(() => productService.getBySlug(slug), [slug]);
  const related = useAsync(() => productService.getRelated(slug, 4), [slug]);
  const p = product.loading ? null : product.data;
  useDocumentTitle(p?.name);

  if (product.loading) return <PageLoader />;
  if (product.error?.status === 404) return <NotFoundPage />;
  if (product.error || !p) {
    return (
      <div className="container-page py-16">
        <ErrorState message="We could not load this product." onRetry={product.retry} />
      </div>
    );
  }

  const discount = discountPercent(p.mrp, p.price);
  const saving = p.mrp > p.price ? p.mrp - p.price : 0;

  return (
    <>
      <div className="border-b border-slate-200 bg-white/60">
        <Breadcrumbs
          className="container-page py-3"
          crumbs={[
            { label: 'All Products', to: '/products' },
            { label: p.category.name, to: `/categories/${p.category.slug}` },
            { label: p.name },
          ]}
        />
      </div>

      <section className="container-page grid gap-8 py-8 sm:py-10 lg:grid-cols-2 lg:gap-12">
        {/* key resets the gallery position when moving to a related product */}
        <ProductGallery key={p.slug} product={p} discount={discount} />

        <div>
          <div className="flex flex-wrap items-center gap-2 text-xs font-semibold tracking-wider uppercase">
            <Link to={`/brands/${p.brand.slug}`} className="text-orange-600 hover:underline">
              {p.brand.name}
            </Link>
            <span className="text-slate-300" aria-hidden="true">
              •
            </span>
            <Link to={`/categories/${p.category.slug}`} className="text-slate-500 hover:text-navy-900 hover:underline">
              {p.category.name}
            </Link>
            <DemoTag show={p.isDemo} />
          </div>

          <h1 className="mt-2 text-3xl font-extrabold sm:text-4xl">{p.name}</h1>

          <div className="mt-3 flex flex-wrap gap-2 text-xs">
            <span className="rounded-full bg-navy-900 px-3 py-1 font-semibold text-white">Pack: {p.packSize}</span>
            <span className="rounded-full bg-white px-3 py-1 text-slate-600 ring-1 ring-slate-200">Sold per {formatUnit(p.unit).toLowerCase()}</span>
            <span className="rounded-full bg-white px-3 py-1 text-slate-600 ring-1 ring-slate-200">Code: {p.sku}</span>
          </div>

          <div className="mt-6 rounded-2xl bg-white p-5 ring-1 ring-slate-200/70">
            <p className="text-xs font-semibold tracking-wider text-slate-500 uppercase">Indicative price</p>
            <div className="mt-1">
              <PriceTag price={p.price} mrp={p.mrp} size="lg" />
            </div>
            {saving > 0 && (
              <p className="mt-1 text-sm text-emerald-700">
                {formatCurrency(saving)} below MRP ({discount}% off) – indicative
              </p>
            )}
            <p className="mt-3 flex gap-2 text-xs text-slate-500">
              <Info className="mt-0.5 size-3.5 shrink-0" aria-hidden="true" />
              {settings?.priceDisclaimer ?? 'Prices shown are indicative only. Please confirm at the shop.'}
            </p>

            <div className="mt-4 flex flex-wrap items-center gap-2 border-t border-slate-100 pt-4">
              <StockBadge status={p.stockStatus} />
              <span className="text-sm text-slate-600">{STOCK_TEXT[p.stockStatus]}</span>
              {p.isDemo && <span className="text-xs text-violet-700">(demo stock level, for testing only)</span>}
            </div>
          </div>

          <p className="mt-6 leading-relaxed text-slate-700">{p.description}</p>

          <div className="mt-6 space-y-2">
            <WhatsAppEnquiryButton product={p} label="Enquire on WhatsApp" className="py-3" />
            {settings?.whatsappEnquiryEnabled && <p className="text-center text-xs text-slate-500">{settings.whatsappNotice}</p>}
          </div>

          <div className="mt-4">
            <AddToEstimate key={p.slug} product={p} />
          </div>

          <p className="mt-4 flex items-start gap-2 rounded-2xl bg-gold-400/15 p-4 text-sm text-navy-900">
            <Store className="mt-0.5 size-4 shrink-0 text-orange-600" aria-hidden="true" />
            This product is not sold online. Please visit our shop in Eachanari, Coimbatore to purchase.
          </p>
        </div>
      </section>

      <section className="container-page grid gap-6 pb-6 lg:grid-cols-2">
        <div className="rounded-3xl bg-white p-6 ring-1 ring-slate-200/70" aria-labelledby="safety-title">
          <h2 id="safety-title" className="flex items-center gap-2 text-lg font-bold">
            <ShieldCheck className="size-5 text-emerald-600" aria-hidden="true" /> Safety information
          </h2>
          <ul className="mt-4 space-y-2.5">
            {p.safetyNotes.map((note) => (
              <li key={note} className="flex gap-2.5 text-sm text-slate-700">
                <AlertTriangle className="mt-0.5 size-4 shrink-0 text-orange-500" aria-hidden="true" />
                {note}
              </li>
            ))}
          </ul>
          <p className="mt-4 text-xs text-slate-500">
            Always read the instructions printed on the pack. Follow local rules on timings and permitted areas.{' '}
            <Link to="/safety" className="font-semibold text-orange-600 hover:underline">
              Full safety guidelines →
            </Link>
          </p>
        </div>

        <div className="rounded-3xl bg-white p-6 ring-1 ring-slate-200/70" aria-labelledby="details-title">
          <h2 id="details-title" className="text-lg font-bold">
            Product details
          </h2>
          <dl className="mt-3">
            <DetailRow label="Brand">
              <Link to={`/brands/${p.brand.slug}`} className="hover:text-orange-600">
                {p.brand.name}
              </Link>
            </DetailRow>
            <DetailRow label="Category">
              <Link to={`/categories/${p.category.slug}`} className="hover:text-orange-600">
                {p.category.name}
              </Link>
            </DetailRow>
            <DetailRow label="Pack size">{p.packSize}</DetailRow>
            <DetailRow label="Unit">{formatUnit(p.unit)}</DetailRow>
            <DetailRow label="Product code">{p.sku}</DetailRow>
            <DetailRow label="MRP">{formatCurrency(p.mrp)}</DetailRow>
            <DetailRow label="Indicative price">{formatCurrency(p.price)}*</DetailRow>
            <DetailRow label="Data">{p.isDemo ? 'Demonstration sample' : 'Shop catalogue'}</DetailRow>
          </dl>
        </div>
      </section>

      <ProductRow
        eyebrow="You may also like"
        title="Related products"
        subtitle={`More from ${p.category.name} and ${p.brand.name}.`}
        actionTo={`/categories/${p.category.slug}`}
        products={related.data}
        loading={related.loading}
        error={related.error}
        onRetry={related.retry}
      />
    </>
  );
}
