import { ShieldAlert } from 'lucide-react';
import { useSettings } from '../../hooks/useSettings.js';

const FALLBACK =
  'This website is an information catalogue only. We do not sell, book, accept payment for, or deliver firecrackers online. All purchases are made in person at our shop.';

/** Persistent legal strip shown on every page, just above the footer. */
export default function LegalNotice() {
  const { settings } = useSettings();
  return (
    <section aria-label="Legal notice" className="border-y border-gold-400/30 bg-navy-800">
      <div className="container-page flex items-start gap-3 py-4 text-xs leading-relaxed text-white/80 sm:items-center sm:text-sm">
        <ShieldAlert className="mt-0.5 size-5 shrink-0 text-gold-400 sm:mt-0" aria-hidden="true" />
        <p>
          <strong className="font-semibold text-gold-400">Important: </strong>
          {settings?.legalNotice ?? FALLBACK}
        </p>
      </div>
    </section>
  );
}
