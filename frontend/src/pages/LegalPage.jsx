import { NavLink, useParams } from 'react-router-dom';
import { FileText } from 'lucide-react';
import PageHeader from '../components/ui/PageHeader.jsx';
import NotFoundPage from './NotFoundPage.jsx';
import { useDocumentTitle } from '../hooks/useDocumentTitle.js';
import { LEGAL_PAGES, LEGAL_REVIEW_NOTE } from '../content/legalPages.js';

/** /legal/:slug – disclaimer, privacy, terms. */
export default function LegalPage() {
  const { slug } = useParams();
  const page = LEGAL_PAGES[slug];
  useDocumentTitle(page?.title);
  if (!page) return <NotFoundPage />;

  return (
    <>
      <PageHeader title={page.title} subtitle={page.intro} crumbs={[{ label: page.title }]} />
      <section className="container-page grid gap-8 py-10 lg:grid-cols-[14rem_1fr]">
        <nav aria-label="Legal pages" className="lg:sticky lg:top-32 lg:self-start">
          <ul className="flex flex-wrap gap-2 lg:flex-col">
            {Object.entries(LEGAL_PAGES).map(([key, p]) => (
              <li key={key}>
                <NavLink
                  to={`/legal/${key}`}
                  className={({ isActive }) =>
                    `flex items-center gap-2 rounded-xl px-3 py-2 text-sm font-medium ${isActive ? 'bg-navy-900 text-gold-400' : 'bg-white text-navy-900 ring-1 ring-slate-200 hover:bg-slate-50'}`
                  }
                >
                  <FileText className="size-4" aria-hidden="true" /> {p.title}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>

        <article className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200/70 sm:p-10">
          <p className="mb-6 rounded-xl bg-amber-50 px-4 py-3 text-xs font-medium text-amber-800 ring-1 ring-amber-200">{LEGAL_REVIEW_NOTE}</p>
          <div className="space-y-7">
            {page.sections.map((s) => (
              <section key={s.heading}>
                <h2 className="text-lg font-bold">{s.heading}</h2>
                {s.body.map((para) => (
                  <p key={para} className="mt-2 text-sm leading-relaxed text-slate-700">
                    {para}
                  </p>
                ))}
              </section>
            ))}
          </div>
        </article>
      </section>
    </>
  );
}
