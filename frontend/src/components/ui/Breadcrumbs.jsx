import { Link } from 'react-router-dom';
import { ChevronRight } from 'lucide-react';

/**
 * Home › … › Current page. crumbs: [{ label, to? }]; the last crumb is the current page.
 * `dark` for use on navy banners.
 */
export default function Breadcrumbs({ crumbs = [], dark = false, className = '' }) {
  const link = dark ? 'hover:text-gold-400' : 'hover:text-orange-600';
  return (
    <nav aria-label="Breadcrumb" className={className}>
      <ol className={`flex flex-wrap items-center gap-1 text-xs ${dark ? 'text-white/60' : 'text-slate-500'}`}>
        <li>
          <Link to="/" className={link}>
            Home
          </Link>
        </li>
        {crumbs.map((c, i) => (
          <li key={`${c.label}-${i}`} className="flex min-w-0 items-center gap-1">
            <ChevronRight className="size-3.5 shrink-0" aria-hidden="true" />
            {c.to && i < crumbs.length - 1 ? (
              <Link to={c.to} className={link}>
                {c.label}
              </Link>
            ) : (
              <span aria-current="page" className={`truncate ${dark ? 'text-gold-300' : 'font-medium text-navy-900'}`}>
                {c.label}
              </span>
            )}
          </li>
        ))}
      </ol>
    </nav>
  );
}
