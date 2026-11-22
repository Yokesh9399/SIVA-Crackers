import { useEffect } from 'react';
import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  useEffect(() => {
    document.title = 'Page not found | SIVA Crackers';
  }, []);

  return (
    <section className="container-page flex min-h-[55vh] flex-col items-center justify-center py-16 text-center">
      <p className="font-display text-7xl font-extrabold text-gold-400">404</p>
      <h1 className="mt-4 text-2xl font-bold">This page fizzled out</h1>
      <p className="mt-2 text-slate-600">The page you are looking for does not exist or has moved.</p>
      <div className="mt-8 flex flex-wrap justify-center gap-3">
        <Link to="/" className="btn-primary">
          Go to home
        </Link>
        <Link to="/products" className="btn border border-navy-900/20 text-navy-900 hover:bg-navy-900 hover:text-white">
          Browse products
        </Link>
      </div>
    </section>
  );
}
