import { Link } from 'react-router-dom';
import { ChevronRight } from 'lucide-react';
import { useDocumentTitle } from '../../hooks/useDocumentTitle.js';

/** Breadcrumbs, page title, short description and action buttons for each admin page. */
export default function AdminPageHeader({ title, description, crumbs = [], actions }) {
  useDocumentTitle(`${title} · Admin`);
  const trail = [{ label: 'Admin', to: '/admin' }, ...crumbs];
  return (
    <header className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div className="min-w-0">
        <nav aria-label="Breadcrumb">
          <ol className="flex flex-wrap items-center gap-1 text-xs text-slate-500">
            {trail.map((c, i) => (
              <li key={c.label} className="flex items-center gap-1">
                {i > 0 && <ChevronRight className="size-3" aria-hidden="true" />}
                {c.to && i < trail.length - 1 ? (
                  <Link to={c.to} className="hover:text-orange-600 hover:underline">
                    {c.label}
                  </Link>
                ) : (
                  <span aria-current={i === trail.length - 1 ? 'page' : undefined}>{c.label}</span>
                )}
              </li>
            ))}
            {crumbs.length === 0 && (
              <li className="flex items-center gap-1">
                <ChevronRight className="size-3" aria-hidden="true" />
                <span aria-current="page">{title}</span>
              </li>
            )}
          </ol>
        </nav>
        <h1 className="mt-1 text-2xl font-extrabold sm:text-3xl">{title}</h1>
        {description && <p className="mt-1 max-w-2xl text-sm text-slate-600">{description}</p>}
      </div>
      {actions && <div className="flex shrink-0 flex-wrap gap-2">{actions}</div>}
    </header>
  );
}
