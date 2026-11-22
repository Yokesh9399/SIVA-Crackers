import { Link } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';

/** Consistent section title with optional eyebrow, subtitle and "View all" link. */
export default function SectionHeading({ eyebrow, title, subtitle, actionLabel, actionTo, align = 'left', dark = false }) {
  const centered = align === 'center';
  return (
    <div className={`mb-8 flex flex-col gap-4 ${centered ? 'items-center text-center' : 'sm:flex-row sm:items-end sm:justify-between'}`}>
      <div className={centered ? 'max-w-2xl' : 'max-w-3xl'}>
        {eyebrow && (
          <p className="mb-2 text-xs font-semibold tracking-[0.2em] text-orange-500 uppercase">{eyebrow}</p>
        )}
        <h2 className={`text-2xl font-bold sm:text-3xl ${dark ? 'text-white' : 'text-navy-900'}`}>{title}</h2>
        {subtitle && <p className={`mt-2 text-sm sm:text-base ${dark ? 'text-white/70' : 'text-slate-600'}`}>{subtitle}</p>}
      </div>
      {actionLabel && actionTo && (
        <Link
          to={actionTo}
          className={`group inline-flex shrink-0 items-center gap-1.5 text-sm font-semibold ${dark ? 'text-gold-400' : 'text-orange-600'} hover:underline`}
        >
          {actionLabel}
          <ArrowRight className="size-4 transition group-hover:translate-x-0.5" aria-hidden="true" />
        </Link>
      )}
    </div>
  );
}
