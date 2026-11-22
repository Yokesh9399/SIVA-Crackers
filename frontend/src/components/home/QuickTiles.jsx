import { Link } from 'react-router-dom';
import { FileText, MapPin, MessageCircle, Phone, ShieldCheck } from 'lucide-react';
import { useSettings } from '../../hooks/useSettings.js';
import { telHref } from '../../utils/format.js';
import { buildWhatsAppUrl, generalEnquiryMessage } from '../../utils/whatsapp.js';
import Reveal from '../ui/Reveal.jsx';

/** Four quick-action tiles below the hero: price list, call, WhatsApp enquiry, safety. */
export default function QuickTiles() {
  const { settings } = useSettings();
  const whatsappUrl = settings?.whatsappEnquiryEnabled
    ? buildWhatsAppUrl(settings.whatsappNumber, generalEnquiryMessage(settings.shopName))
    : null;

  const tiles = [
    { icon: FileText, title: 'Price list', text: 'Category-wise indicative prices', to: '/price-list' },
    settings?.phone
      ? { icon: Phone, title: 'Call the shop', text: settings.phone, href: telHref(settings.phone) }
      : { icon: Phone, title: 'Call the shop', text: 'Number to be updated', disabled: true },
    settings?.whatsappEnquiryEnabled !== false && {
      icon: MessageCircle,
      title: 'WhatsApp enquiry',
      text: whatsappUrl ? 'General questions only' : 'Number to be updated',
      href: whatsappUrl,
      external: true,
      disabled: !whatsappUrl,
    },
    { icon: ShieldCheck, title: 'Safety tips', text: 'Celebrate responsibly', to: '/safety' },
    { icon: MapPin, title: 'Visit us', text: 'Eachanari, Coimbatore', to: '/contact' },
  ].filter(Boolean);

  return (
    <section className="relative z-10 -mt-10 sm:-mt-12">
      <div className="container-page">
        <div className="grid grid-cols-2 gap-3 sm:gap-4 md:grid-cols-3 lg:grid-cols-5">
          {tiles.map((tile, i) => {
            const Icon = tile.icon;
            const inner = (
              <>
                <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-gold-400 to-orange-500 text-navy-950 shadow-md">
                  <Icon className="size-5" aria-hidden="true" />
                </span>
                <span className="min-w-0">
                  <span className="block font-display text-sm font-semibold text-navy-900">{tile.title}</span>
                  <span className="block truncate text-xs text-slate-500">{tile.text}</span>
                </span>
              </>
            );
            const base =
              'flex h-full items-center gap-3 rounded-2xl bg-white p-3.5 shadow-lg shadow-navy-900/5 ring-1 ring-slate-200/70 transition sm:p-4';
            const interactive = 'hover:-translate-y-0.5 hover:shadow-xl hover:ring-gold-400/60';

            let element;
            if (tile.disabled) {
              element = (
                <div className={`${base} cursor-not-allowed grayscale-[60%] [&_span]:text-slate-400`} title="This detail will be added by the shop owner">
                  {inner}
                </div>
              );
            } else if (tile.to) {
              element = (
                <Link to={tile.to} className={`${base} ${interactive}`}>
                  {inner}
                </Link>
              );
            } else {
              element = (
                <a
                  href={tile.href}
                  className={`${base} ${interactive}`}
                  {...(tile.external ? { target: '_blank', rel: 'noopener noreferrer' } : {})}
                >
                  {inner}
                </a>
              );
            }
            return (
              <Reveal key={tile.title} delay={i * 60} className={i === tiles.length - 1 ? 'col-span-2 md:col-span-1' : ''}>
                {element}
              </Reveal>
            );
          })}
        </div>
      </div>
    </section>
  );
}
