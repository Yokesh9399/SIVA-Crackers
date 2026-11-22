import { useId } from 'react';

// Original placeholder illustrations, one per category, drawn in SVG.
// Used until the owner uploads real product photos (Phase 5 admin).

const GOLD = '#F5B921';
const ORANGE = '#F26A1B';
const CREAM = '#FFF3D6';

function Sparks({ cx, cy, r = 28, color = GOLD, count = 10 }) {
  return (
    <g stroke={color} strokeWidth="3" strokeLinecap="round">
      {Array.from({ length: count }, (_, i) => {
        const a = (i / count) * Math.PI * 2;
        const r1 = r * 0.45;
        return (
          <line
            key={i}
            x1={cx + Math.cos(a) * r1}
            y1={cy + Math.sin(a) * r1}
            x2={cx + Math.cos(a) * r}
            y2={cy + Math.sin(a) * r}
            opacity={i % 2 ? 0.7 : 1}
          />
        );
      })}
    </g>
  );
}

const SHAPES = {
  sparklers: () => (
    <g>
      <line x1="70" y1="160" x2="120" y2="70" stroke="#9AA3B8" strokeWidth="5" strokeLinecap="round" />
      <line x1="95" y1="160" x2="135" y2="85" stroke="#9AA3B8" strokeWidth="5" strokeLinecap="round" />
      <Sparks cx={120} cy={68} r={34} count={14} />
      <Sparks cx={136} cy={84} r={22} color={CREAM} count={8} />
    </g>
  ),
  'flower-pots': () => (
    <g>
      <path d="M80 165 L120 165 L110 115 L90 115 Z" fill={ORANGE} />
      <rect x="86" y="108" width="28" height="9" rx="3" fill={GOLD} />
      <path d="M100 105 C80 70 60 60 50 50 M100 105 C100 65 100 50 100 35 M100 105 C120 70 140 60 150 50" stroke={GOLD} strokeWidth="4" fill="none" strokeLinecap="round" strokeDasharray="2 8" />
    </g>
  ),
  'ground-chakkars': () => (
    <g>
      <circle cx="100" cy="105" r="26" fill="none" stroke={ORANGE} strokeWidth="9" />
      <circle cx="100" cy="105" r="10" fill={GOLD} />
      <path d="M100 60 A45 45 0 0 1 145 105" stroke={GOLD} strokeWidth="4" fill="none" strokeLinecap="round" strokeDasharray="3 7" />
      <path d="M100 150 A45 45 0 0 1 55 105" stroke={GOLD} strokeWidth="4" fill="none" strokeLinecap="round" strokeDasharray="3 7" />
    </g>
  ),
  rockets: () => (
    <g transform="rotate(25 100 100)">
      <path d="M100 40 C112 55 114 75 112 110 L88 110 C86 75 88 55 100 40 Z" fill={CREAM} />
      <path d="M100 40 C108 50 110 58 111 66 L89 66 C90 58 92 50 100 40 Z" fill={ORANGE} />
      <path d="M88 100 L76 118 L88 112 Z M112 100 L124 118 L112 112 Z" fill={ORANGE} />
      <line x1="100" y1="112" x2="100" y2="165" stroke="#9AA3B8" strokeWidth="3" />
      <path d="M93 114 Q100 140 107 114" fill={GOLD} />
    </g>
  ),
  'single-sound': () => (
    <g>
      {[70, 92, 114].map((x, i) => (
        <g key={x} transform={`rotate(${(i - 1) * 10} ${x + 8} 130)`}>
          <rect x={x} y="85" width="18" height="60" rx="4" fill={i === 1 ? GOLD : ORANGE} />
          <line x1={x + 9} y1="85" x2={x + 9} y2="68" stroke="#9AA3B8" strokeWidth="3" />
        </g>
      ))}
      <Sparks cx={101} cy={62} r={14} count={8} />
    </g>
  ),
  bombs: () => (
    <g>
      <circle cx="98" cy="115" r="38" fill={ORANGE} />
      <circle cx="86" cy="102" r="9" fill="#ffffff" opacity="0.35" />
      <rect x="110" y="72" width="16" height="12" rx="3" fill={GOLD} transform="rotate(35 118 78)" />
      <path d="M124 70 Q138 55 132 45" stroke="#9AA3B8" strokeWidth="3" fill="none" />
      <Sparks cx={132} cy={44} r={16} count={8} />
    </g>
  ),
  garlands: () => (
    <g>
      <path d="M40 70 Q100 130 160 70" stroke="#9AA3B8" strokeWidth="3" fill="none" />
      {Array.from({ length: 9 }, (_, i) => {
        const t = i / 8;
        const x = 40 + t * 120;
        const y = 70 + Math.sin(t * Math.PI) * 30;
        return <rect key={i} x={x - 5} y={y} width="10" height="26" rx="3" fill={i % 2 ? GOLD : ORANGE} transform={`rotate(${(t - 0.5) * -50} ${x} ${y})`} />;
      })}
    </g>
  ),
  'aerial-shots': () => (
    <g>
      <rect x="70" y="120" width="60" height="45" rx="5" fill={ORANGE} />
      <g fill="#0B1636">
        {[80, 95, 110].map((x) => (
          <circle key={x} cx={x + 5} cy="132" r="5" />
        ))}
      </g>
      <Sparks cx={70} cy={60} r={24} count={12} />
      <Sparks cx={130} cy={48} r={20} color={CREAM} count={10} />
      <Sparks cx={105} cy={85} r={14} color={ORANGE} count={8} />
    </g>
  ),
  fountains: () => (
    <g>
      <path d="M88 165 L112 165 L106 100 L94 100 Z" fill={GOLD} />
      <path d="M100 98 C70 70 55 75 45 90 M100 98 C85 60 90 45 95 35 M100 98 C130 70 145 75 155 90 M100 98 C115 60 110 45 105 35" stroke={ORANGE} strokeWidth="4" fill="none" strokeLinecap="round" />
      <circle cx="45" cy="92" r="4" fill={GOLD} />
      <circle cx="155" cy="92" r="4" fill={GOLD} />
      <circle cx="100" cy="32" r="5" fill={CREAM} />
    </g>
  ),
  'kids-specials': () => (
    <g>
      <path d="M55 140 C75 110 95 160 115 125 C130 100 150 120 150 95" stroke={ORANGE} strokeWidth="10" fill="none" strokeLinecap="round" />
      {[[60, 70, GOLD], [90, 55, CREAM], [125, 65, ORANGE], [145, 140, GOLD], [75, 165, CREAM]].map(([x, y, c]) => (
        <circle key={`${x}-${y}`} cx={x} cy={y} r="7" fill={c} />
      ))}
    </g>
  ),
  pencils: () => (
    <g>
      {[0, 1, 2].map((i) => (
        <g key={i} transform={`rotate(${-20 + i * 20} 100 160)`}>
          <rect x="94" y="65" width="12" height="90" rx="3" fill={[ORANGE, GOLD, CREAM][i]} />
          <path d="M94 65 L100 52 L106 65 Z" fill="#9AA3B8" />
        </g>
      ))}
      <Sparks cx={100} cy={45} r={18} count={10} />
    </g>
  ),
  'gift-boxes': () => (
    <g>
      <rect x="55" y="95" width="90" height="70" rx="6" fill={ORANGE} />
      <rect x="50" y="80" width="100" height="22" rx="5" fill={GOLD} />
      <rect x="93" y="80" width="14" height="85" fill={CREAM} />
      <path d="M100 80 C80 55 65 70 80 80 Z M100 80 C120 55 135 70 120 80 Z" fill={CREAM} />
      <Sparks cx={150} cy={55} r={14} count={8} />
    </g>
  ),
};

// Alternate looks for the product gallery until real photos are uploaded:
// 0 = standard, 1 = close-up, 2 = warm night-sky background.
const VARIANTS = [
  { from: '#2A3B74', to: '#0B1636', transform: undefined },
  { from: '#1B2A5C', to: '#070E24', transform: 'translate(100 100) scale(1.45) translate(-100 -100)' },
  { from: '#5A2A1A', to: '#120A1E', transform: 'translate(100 104) scale(0.9) translate(-100 -100)' },
];

/**
 * Product image placeholder. Pass the category slug to pick the drawing.
 * When a product has real images, ProductCard / ProductGallery show those instead.
 */
export default function ProductArt({ categorySlug, label, className = '', variant = 0 }) {
  const gradientId = useId();
  const Shape = SHAPES[categorySlug] ?? SHAPES['gift-boxes'];
  const look = VARIANTS[variant % VARIANTS.length];
  return (
    <svg viewBox="0 0 200 200" className={className} role="img" aria-label={label ?? 'Product illustration'}>
      <defs>
        <radialGradient id={gradientId} cx="50%" cy="40%" r="75%">
          <stop offset="0%" stopColor={look.from} />
          <stop offset="100%" stopColor={look.to} />
        </radialGradient>
      </defs>
      <rect width="200" height="200" fill={`url(#${gradientId})`} />
      <circle cx="30" cy="30" r="1.8" fill={CREAM} opacity="0.6" />
      <circle cx="170" cy="40" r="1.4" fill={CREAM} opacity="0.5" />
      <circle cx="160" cy="170" r="1.6" fill={CREAM} opacity="0.4" />
      <circle cx="25" cy="150" r="1.2" fill={CREAM} opacity="0.5" />
      <g transform={look.transform}>
        <Shape />
      </g>
    </svg>
  );
}
