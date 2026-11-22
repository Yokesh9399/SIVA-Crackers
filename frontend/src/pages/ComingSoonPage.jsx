import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Hammer } from 'lucide-react';

/** Temporary page for routes that are built in later phases, so no nav link is broken. */
export default function ComingSoonPage({ title, phase }) {
  useEffect(() => {
    document.title = `${title} | SIVA Crackers`;
  }, [title]);

  return (
    <section className="container-page flex min-h-[55vh] flex-col items-center justify-center py-16 text-center">
      <span className="grid size-16 place-items-center rounded-2xl bg-navy-900 text-gold-400">
        <Hammer className="size-8" aria-hidden="true" />
      </span>
      <h1 className="mt-6 text-3xl font-bold">{title}</h1>
      <p className="mt-3 max-w-md text-slate-600">
        This page is being built and will be available soon (development phase {phase}).
      </p>
      <Link to="/" className="btn-primary mt-8">
        Back to home
      </Link>
    </section>
  );
}
