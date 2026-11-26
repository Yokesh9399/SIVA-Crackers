import { useState } from 'react';

/** Brand logo uploaded by the owner; falls back to an initials badge when there is none or it fails to load. */
export default function BrandLogo({ name, logoUrl, className = 'size-12 text-base' }) {
  const [failed, setFailed] = useState(false);
  if (logoUrl && !failed) {
    return (
      <img
        src={logoUrl}
        alt={`${name} logo`}
        loading="lazy"
        onError={() => setFailed(true)}
        className={`shrink-0 rounded-full bg-white object-contain p-1 ${className}`}
      />
    );
  }
  const initials = name
    .split(/\s+/)
    .filter(Boolean)
    .map((w) => w[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();
  return (
    <span className={`grid shrink-0 place-items-center rounded-full bg-gradient-to-br from-gold-300 to-orange-500 font-display font-bold text-navy-950 ${className}`} aria-hidden="true">
      {initials}
    </span>
  );
}
