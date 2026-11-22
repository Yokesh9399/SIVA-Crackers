import { Link } from 'react-router-dom';

/** Original SIVA Crackers logo: a gold starburst mark plus the wordmark. */
export default function Logo({ light = true, compact = false }) {
  return (
    <Link to="/" className="group inline-flex min-w-0 items-center gap-2 sm:gap-2.5" aria-label="SIVA Crackers home">
      <svg viewBox="0 0 48 48" className="size-9 shrink-0 sm:size-10 transition-transform duration-500 group-hover:rotate-45" aria-hidden="true">
        <defs>
          <radialGradient id="logo-core" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor="#FAD26A" />
            <stop offset="100%" stopColor="#F26A1B" />
          </radialGradient>
        </defs>
        <circle cx="24" cy="24" r="23" fill="#12204A" stroke="#F5B921" strokeWidth="1.5" />
        <g stroke="#F5B921" strokeWidth="2.6" strokeLinecap="round">
          <path d="M24 7v8M24 33v8M7 24h8M33 24h8M12 12l5.5 5.5M30.5 30.5 36 36M36 12l-5.5 5.5M17.5 30.5 12 36" />
        </g>
        <circle cx="24" cy="24" r="5.5" fill="url(#logo-core)" />
      </svg>
      <span className="leading-none whitespace-nowrap">
        <span className={`block font-display text-lg font-extrabold tracking-wide sm:text-xl ${light ? 'text-white' : 'text-navy-900'}`}>
          SIVA <span className="text-gold-400">Crackers</span>
        </span>
        {!compact && (
          <span className={`mt-1 block text-[0.65rem] font-medium tracking-[0.18em] uppercase ${light ? 'text-white/60' : 'text-slate-500'}`}>
            Eachanari · Coimbatore
          </span>
        )}
      </span>
    </Link>
  );
}
