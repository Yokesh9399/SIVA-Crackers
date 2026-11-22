import { Link } from 'react-router-dom';
import { ClipboardList, FileText, Search, Store } from 'lucide-react';
import Reveal from '../ui/Reveal.jsx';

const STEPS = [
  { icon: Search, title: 'Browse the range', text: 'Explore products by category or brand.', to: '/products', cta: 'All products' },
  { icon: FileText, title: 'Check prices', text: 'See indicative prices in the category-wise price list.', to: '/price-list', cta: 'Price list' },
  { icon: ClipboardList, title: 'Make a list', text: 'Note quantities in your Estimate List. It stays on your device.', to: '/estimate', cta: 'Estimate list' },
  { icon: Store, title: 'Visit the shop', text: 'Buy in person at our shop in Eachanari and confirm final prices.', to: '/contact', cta: 'Find us' },
];

/** "How to plan your visit" – used on About and Contact pages. */
export default function VisitSteps({ title = 'Plan your visit in four steps' }) {
  return (
    <section aria-labelledby="visit-steps-title">
      <h2 id="visit-steps-title" className="mb-6 text-2xl font-bold">
        {title}
      </h2>
      <ol className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {STEPS.map((step, i) => (
          <Reveal as="li" key={step.title} delay={i * 70}>
            <div className="relative h-full rounded-3xl bg-white p-5 shadow-sm ring-1 ring-slate-200/70">
              <span className="absolute top-4 right-5 font-display text-3xl font-extrabold text-gold-400/40" aria-hidden="true">
                {i + 1}
              </span>
              <span className="grid size-11 place-items-center rounded-xl bg-gradient-to-br from-gold-400 to-orange-500 text-navy-950">
                <step.icon className="size-5" aria-hidden="true" />
              </span>
              <h3 className="mt-4 font-semibold">{step.title}</h3>
              <p className="mt-1 text-sm text-slate-600">{step.text}</p>
              <Link to={step.to} className="mt-3 inline-block text-sm font-semibold text-orange-600 hover:underline">
                {step.cta} →
              </Link>
            </div>
          </Reveal>
        ))}
      </ol>
    </section>
  );
}
