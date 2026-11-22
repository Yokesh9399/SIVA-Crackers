import FireworksBackdrop from '../home/FireworksBackdrop.jsx';
import Breadcrumbs from './Breadcrumbs.jsx';

/**
 * Navy page banner with breadcrumbs, title and subtitle. Used by the catalogue
 * and business pages. crumbs: [{ label, to? }] – the last crumb is the current page.
 */
export default function PageHeader({ title, subtitle, crumbs = [], children }) {
  return (
    <section className="relative overflow-hidden bg-gradient-to-br from-navy-950 via-navy-900 to-navy-700 print:hidden">
      <FireworksBackdrop variant={2} />
      <div className="container-page relative py-10 sm:py-12">
        {crumbs.length > 0 && <Breadcrumbs crumbs={crumbs} dark className="mb-3" />}
        <h1 className="text-3xl font-extrabold text-white sm:text-4xl">{title}</h1>
        {subtitle && <p className="mt-2 max-w-2xl text-sm text-white/70 sm:text-base">{subtitle}</p>}
        {children}
      </div>
    </section>
  );
}
