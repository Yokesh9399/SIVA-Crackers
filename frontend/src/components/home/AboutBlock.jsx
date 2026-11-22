import { Link } from 'react-router-dom';
import { HeartHandshake, Search, ShieldCheck, Store } from 'lucide-react';
import { useSettings } from '../../hooks/useSettings.js';
import SectionHeading from '../ui/SectionHeading.jsx';
import Reveal from '../ui/Reveal.jsx';

const ICONS = [HeartHandshake, Search, ShieldCheck, Store];

/** Short "About the shop" block with highlight cards. All text comes from settings. */
export default function AboutBlock() {
  const { settings } = useSettings();
  if (!settings) return null;

  return (
    <section className="container-page py-14 sm:py-20" aria-labelledby="about-heading">
      <div className="grid items-center gap-10 lg:grid-cols-2">
        <Reveal>
          <SectionHeading eyebrow="About us" title={<span id="about-heading">Welcome to {settings.shopName}</span>} />
          <p className="-mt-4 text-base leading-relaxed text-slate-600">{settings.aboutText}</p>
          <Link to="/about" className="btn-primary mt-6">
            Read more about us
          </Link>
        </Reveal>

        <div className="grid gap-4 sm:grid-cols-2">
          {settings.highlights?.map((item, i) => {
            const Icon = ICONS[i % ICONS.length];
            return (
              <Reveal key={item.title} delay={i * 80}>
                <div className="h-full rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-200/70">
                  <span className="grid size-10 place-items-center rounded-xl bg-navy-900 text-gold-400">
                    <Icon className="size-5" aria-hidden="true" />
                  </span>
                  <h3 className="mt-4 text-base font-semibold">{item.title}</h3>
                  <p className="mt-1 text-sm text-slate-600">{item.text}</p>
                </div>
              </Reveal>
            );
          })}
        </div>
      </div>
    </section>
  );
}
