import { Link } from 'react-router-dom';
import { CheckCircle2, ShieldCheck } from 'lucide-react';
import { useSettings } from '../../hooks/useSettings.js';
import Reveal from '../ui/Reveal.jsx';

/** Safety tips band. Tips come from settings so the owner can edit them. */
export default function SafetyBlock() {
  const { settings } = useSettings();
  const tips = settings?.safetyTips ?? [];
  if (!tips.length) return null;

  return (
    <section className="bg-gradient-to-br from-orange-500 to-gold-400 py-14 sm:py-16" aria-labelledby="safety-heading">
      <div className="container-page grid gap-8 lg:grid-cols-[1fr_2fr] lg:items-center">
        <Reveal>
          <span className="grid size-14 place-items-center rounded-2xl bg-navy-900 text-gold-400 shadow-lg">
            <ShieldCheck className="size-7" aria-hidden="true" />
          </span>
          <h2 id="safety-heading" className="mt-5 text-2xl font-bold text-navy-950 sm:text-3xl">
            Celebrate safely
          </h2>
          <p className="mt-2 text-navy-950/80">A few simple habits keep your family and neighbours safe.</p>
          <Link to="/safety" className="btn mt-6 bg-navy-900 text-white hover:bg-navy-800">
            Full safety guidelines
          </Link>
        </Reveal>

        <ul className="grid gap-3 sm:grid-cols-2">
          {tips.slice(0, 6).map((tip, i) => (
            <Reveal as="li" key={tip} delay={i * 60}>
              <div className="flex h-full gap-3 rounded-2xl bg-white/90 p-4 text-sm text-navy-900 shadow-sm">
                <CheckCircle2 className="mt-0.5 size-5 shrink-0 text-emerald-600" aria-hidden="true" />
                {tip}
              </div>
            </Reveal>
          ))}
        </ul>
      </div>
    </section>
  );
}
