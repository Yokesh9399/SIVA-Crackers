import { Link } from 'react-router-dom';
import { ChevronRight } from 'lucide-react';
import FireworksBackdrop from '../home/FireworksBackdrop.jsx';

/**
 * Navy page banner with breadcrumbs, title and subtitle. Reused by catalogue
 * pages now and by the Phase 3 business pages.
 * crumbs: [{ label, to? }] – the last crumb is the current page.
 */
export default function PageHeader({ title, subtitle, crumbs = [], children }) {
  return (
    <section className="relative overflow-hidden bg-gradient-to-br from-navy-950 via-navy-900 to-navy-700">
      <FireworksBackdrop variant={2} />
      <div className="container-page relative py-10 sm:py-12">
        {crumbs.length > 0 && (
          <nav aria-label="Breadcrumb" className="mb-3">
            <ol className="flex flex-wrap items-center gap-1 text-xs text-white/60">
              <li>
                <Link to="/" className="hover:text-gold-400">
                  Home
                </Link>
              </li>
              {crumbs.map((c, i) => (
                <li key={c.label} className="flex items-center gap-1">
                  <ChevronRight className="size-3.5" aria-hidden="true" />
                  {c.to && i < crumbs.length - 1 ? (
                    <Link to={c.to} className="hover:text-gold-400">
                      {c.label}
                    </Link>
                  ) : (
                    <span aria-current="page" className="text-gold-300">
                      {c.label}
                    </span>
                  )}
                </li>
              ))}
            </ol>
          </nav>
        )}
        <h1 className="text-3xl font-extrabold text-white sm:text-4xl">{title}</h1>
        {subtitle && <p className="mt-2 max-w-2xl text-sm text-white/70 sm:text-base">{subtitle}</p>}
        {children}
      </div>
    </section>
  );
}
