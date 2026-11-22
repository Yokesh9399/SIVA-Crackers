import { Link } from 'react-router-dom';
import { HeartHandshake, MapPin, Search, ShieldCheck, Store } from 'lucide-react';
import PageHeader from '../components/ui/PageHeader.jsx';
import Reveal from '../components/ui/Reveal.jsx';
import PlaceholderValue from '../components/ui/PlaceholderValue.jsx';
import VisitSteps from '../components/business/VisitSteps.jsx';
import { useDocumentTitle } from '../hooks/useDocumentTitle.js';
import { useSettings } from '../hooks/useSettings.js';
import { formatAddress } from '../utils/format.js';

const HIGHLIGHT_ICONS = [HeartHandshake, Search, ShieldCheck, Store];

const BRAND_COLOURS = [
  { name: 'Night navy', className: 'bg-navy-900', hex: '#0B1636', text: 'text-white' },
  { name: 'Festive gold', className: 'bg-gold-400', hex: '#F5B921', text: 'text-navy-900' },
  { name: 'Spark orange', className: 'bg-orange-500', hex: '#F26A1B', text: 'text-white' },
  { name: 'Diya cream', className: 'bg-cream ring-1 ring-slate-200', hex: '#FFF8EC', text: 'text-navy-900' },
];

/** /about – business introduction. Only facts provided by the owner are shown. */
export default function AboutPage() {
  useDocumentTitle('About Us');
  const { settings } = useSettings();
  const shopName = settings?.shopName ?? 'SIVA Crackers';

  return (
    <>
      <PageHeader title="About Us" subtitle={settings?.tagline} crumbs={[{ label: 'About Us' }]} />

      <section className="container-page grid items-center gap-10 py-12 lg:grid-cols-[1fr_1.2fr]">
        {/* Brand mark */}
        <Reveal>
          <div className="relative mx-auto grid aspect-square w-full max-w-sm place-items-center overflow-hidden rounded-[2.5rem] bg-gradient-to-br from-navy-950 via-navy-900 to-navy-700 shadow-2xl shadow-navy-900/30">
            <svg viewBox="0 0 48 48" className="w-1/2 animate-[spin_40s_linear_infinite]" aria-hidden="true">
              <g stroke="#F5B921" strokeWidth="2.2" strokeLinecap="round">
                <path d="M24 5v9M24 34v9M5 24h9M34 24h9M10.5 10.5l6.4 6.4M31.1 31.1l6.4 6.4M37.5 10.5l-6.4 6.4M16.9 31.1l-6.4 6.4" />
              </g>
              <circle cx="24" cy="24" r="6" fill="#F26A1B" />
            </svg>
            <p className="absolute bottom-8 font-display text-2xl font-extrabold text-white">
              SIVA <span className="text-gold-400">Crackers</span>
            </p>
          </div>
        </Reveal>

        <Reveal delay={100}>
          <p className="text-xs font-semibold tracking-[0.2em] text-orange-500 uppercase">Who we are</p>
          <h2 className="mt-2 text-3xl font-bold">Welcome to {shopName}</h2>
          <p className="mt-4 text-base leading-relaxed text-slate-700">{settings?.aboutText}</p>

          <div className="mt-6 flex gap-4 rounded-2xl bg-white p-5 ring-1 ring-slate-200/70">
            <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-gold-400/15 text-orange-600">
              <MapPin className="size-5" aria-hidden="true" />
            </span>
            <div>
              <h3 className="font-semibold">Our location</h3>
              <p className="text-sm text-slate-700">{formatAddress(settings?.address)}</p>
              {!settings?.address?.line1 && (
                <p className="text-xs text-slate-500">
                  Street address: <PlaceholderValue value={null} />
                </p>
              )}
            </div>
          </div>

          <div className="mt-6 flex flex-wrap gap-3">
            <Link to="/products" className="btn-primary">
              Explore our products
            </Link>
            <Link to="/contact" className="btn border border-navy-900/20 text-navy-900 hover:bg-navy-900 hover:text-white">
              Contact &amp; directions
            </Link>
          </div>
        </Reveal>
      </section>

      {/* Highlights */}
      <section className="bg-white py-12" aria-labelledby="highlights-title">
        <div className="container-page">
          <h2 id="highlights-title" className="mb-6 text-2xl font-bold">
            What makes us different
          </h2>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {settings?.highlights?.map((item, i) => {
              const Icon = HIGHLIGHT_ICONS[i % HIGHLIGHT_ICONS.length];
              return (
                <Reveal key={item.title} delay={i * 70}>
                  <div className="h-full rounded-3xl bg-cream p-6">
                    <span className="grid size-11 place-items-center rounded-xl bg-navy-900 text-gold-400">
                      <Icon className="size-5" aria-hidden="true" />
                    </span>
                    <h3 className="mt-4 font-semibold">{item.title}</h3>
                    <p className="mt-1 text-sm text-slate-600">{item.text}</p>
                  </div>
                </Reveal>
              );
            })}
          </div>
        </div>
      </section>

      <div className="container-page space-y-14 py-12">
        <VisitSteps />

        {/* Brand identity */}
        <section aria-labelledby="brand-title" className="rounded-3xl bg-white p-6 ring-1 ring-slate-200/70 sm:p-8">
          <h2 id="brand-title" className="text-2xl font-bold">
            Our brand
          </h2>
          <p className="mt-2 max-w-2xl text-sm text-slate-600">
            The SIVA Crackers starburst stands for the joy of a festival night sky. Our colours bring together the deep navy of the
            night, the gold of a diya and the orange spark of celebration.
          </p>
          <ul className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-4">
            {BRAND_COLOURS.map((c) => (
              <li key={c.name}>
                <div className={`flex h-24 items-end rounded-2xl p-3 ${c.className}`}>
                  <span className={`text-xs font-semibold ${c.text}`}>{c.hex}</span>
                </div>
                <p className="mt-2 text-sm font-medium text-navy-900">{c.name}</p>
              </li>
            ))}
          </ul>
        </section>

        <p className="rounded-2xl bg-gold-400/15 p-5 text-sm text-navy-900">
          <strong>Please note:</strong> {settings?.legalNotice}
        </p>
      </div>
    </>
  );
}
