import { Link } from 'react-router-dom';
import { AlertTriangle, ClipboardCheck, Droplets, Flame, HeartHandshake, Phone, Scale, Shirt, Siren, Users } from 'lucide-react';
import PageHeader from '../components/ui/PageHeader.jsx';
import Reveal from '../components/ui/Reveal.jsx';
import { useDocumentTitle } from '../hooks/useDocumentTitle.js';
import { EMERGENCY, SAFETY_DISCLAIMER, SAFETY_SECTIONS } from '../content/safetyGuidelines.js';

const ICONS = { ClipboardCheck, Users, Flame, Droplets, Shirt, HeartHandshake };

/** /safety – detailed fireworks safety guidelines. */
export default function SafetyPage() {
  useDocumentTitle('Safety Guidelines');

  return (
    <>
      <PageHeader
        title="Safety Guidelines"
        subtitle="Simple habits that keep your family, neighbours and pets safe while you celebrate."
        crumbs={[{ label: 'Safety Guidelines' }]}
      />

      <section className="container-page py-10">
        {/* Section index */}
        <nav aria-label="On this page" className="mb-8 flex flex-wrap gap-2">
          {[...SAFETY_SECTIONS, { id: 'emergency', title: EMERGENCY.title }, { id: 'disclaimer', title: 'Legal disclaimer' }].map((s) => (
            <a key={s.id} href={`#${s.id}`} className="rounded-full bg-white px-3 py-1.5 text-xs font-semibold text-navy-900 ring-1 ring-slate-200 hover:bg-navy-900 hover:text-white">
              {s.title}
            </a>
          ))}
        </nav>

        <div className="grid gap-5 md:grid-cols-2">
          {SAFETY_SECTIONS.map((section, i) => {
            const Icon = ICONS[section.icon] ?? ClipboardCheck;
            return (
              <Reveal key={section.id} delay={(i % 2) * 80}>
                <article id={section.id} className="h-full scroll-mt-40 rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200/70">
                  <h2 className="flex items-center gap-3 text-lg font-bold">
                    <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-navy-900 text-gold-400">
                      <Icon className="size-5" aria-hidden="true" />
                    </span>
                    {section.title}
                  </h2>
                  <ul className="mt-4 space-y-2.5">
                    {section.points.map((point) => (
                      <li key={point} className="flex gap-2.5 text-sm leading-relaxed text-slate-700">
                        <span className="mt-2 size-1.5 shrink-0 rounded-full bg-orange-500" aria-hidden="true" />
                        {point}
                      </li>
                    ))}
                  </ul>
                </article>
              </Reveal>
            );
          })}
        </div>

        {/* Emergency */}
        <section id="emergency" aria-labelledby="emergency-title" className="mt-10 scroll-mt-40 overflow-hidden rounded-3xl bg-red-50 ring-1 ring-red-200">
          <div className="flex flex-col gap-4 bg-red-600 px-6 py-5 text-white sm:flex-row sm:items-center sm:justify-between">
            <h2 id="emergency-title" className="flex items-center gap-3 text-xl font-bold text-white">
              <Siren className="size-6" aria-hidden="true" /> {EMERGENCY.title}
            </h2>
            <ul className="flex flex-wrap gap-2">
              {EMERGENCY.numbers.map((n) => (
                <li key={n.number}>
                  <a href={`tel:${n.number}`} className="inline-flex items-center gap-2 rounded-full bg-white px-4 py-2 text-sm font-bold text-red-700 hover:bg-red-100">
                    <Phone className="size-4" aria-hidden="true" /> {n.number}
                    <span className="font-medium text-red-600/80">{n.label}</span>
                  </a>
                </li>
              ))}
            </ul>
          </div>
          <p className="px-6 pt-5 text-sm font-medium text-red-800">{EMERGENCY.note}</p>
          <div className="grid gap-4 p-6 sm:grid-cols-2">
            {EMERGENCY.firstAid.map((item) => (
              <div key={item.title} className="rounded-2xl bg-white p-5 ring-1 ring-red-100">
                <h3 className="flex items-center gap-2 font-semibold">
                  <AlertTriangle className="size-4 text-red-600" aria-hidden="true" /> {item.title}
                </h3>
                <p className="mt-2 text-sm leading-relaxed text-slate-700">{item.text}</p>
              </div>
            ))}
          </div>
        </section>

        {/* Disclaimer */}
        <section id="disclaimer" aria-labelledby="disclaimer-title" className="mt-10 scroll-mt-40 rounded-3xl bg-navy-900 p-6 text-white/80 sm:p-8">
          <h2 id="disclaimer-title" className="flex items-center gap-3 text-lg font-bold text-white">
            <Scale className="size-5 text-gold-400" aria-hidden="true" /> Legal disclaimer
          </h2>
          <p className="mt-3 text-sm leading-relaxed">{SAFETY_DISCLAIMER}</p>
          <p className="mt-3 text-sm leading-relaxed">
            SIVA Crackers sells firecrackers only in person at its shop, subject to applicable laws. See our{' '}
            <Link to="/legal/disclaimer" className="font-semibold text-gold-400 hover:underline">
              full legal disclaimer
            </Link>
            .
          </p>
        </section>
      </section>
    </>
  );
}
