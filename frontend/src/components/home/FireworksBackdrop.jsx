// Decorative animated firework bursts and twinkling stars for the hero (pure SVG + CSS, no images).

const BURSTS = [
  [
    { x: '78%', y: '28%', size: 220, color: '#F5B921', delay: '0s' },
    { x: '90%', y: '62%', size: 140, color: '#F26A1B', delay: '1.1s' },
    { x: '64%', y: '70%', size: 110, color: '#FAD26A', delay: '2s' },
  ],
  [
    { x: '82%', y: '35%', size: 200, color: '#F26A1B', delay: '0.4s' },
    { x: '68%', y: '22%', size: 120, color: '#F5B921', delay: '1.6s' },
    { x: '92%', y: '72%', size: 130, color: '#FAD26A', delay: '2.4s' },
  ],
  [
    { x: '75%', y: '40%', size: 240, color: '#FAD26A', delay: '0.2s' },
    { x: '92%', y: '20%', size: 110, color: '#F26A1B', delay: '1.4s' },
  ],
];

const STARS = [
  ['8%', '18%'], ['22%', '72%'], ['35%', '12%'], ['48%', '82%'], ['55%', '30%'],
  ['12%', '48%'], ['60%', '10%'], ['40%', '55%'], ['70%', '88%'], ['96%', '45%'],
];

function Burst({ size, color }) {
  const rays = 16;
  return (
    <svg viewBox="-50 -50 100 100" width={size} height={size} aria-hidden="true">
      {Array.from({ length: rays }, (_, i) => {
        const a = (i / rays) * Math.PI * 2;
        return (
          <g key={i}>
            <line
              x1={Math.cos(a) * 14}
              y1={Math.sin(a) * 14}
              x2={Math.cos(a) * 44}
              y2={Math.sin(a) * 44}
              stroke={color}
              strokeWidth="1.6"
              strokeLinecap="round"
              opacity={i % 2 ? 0.55 : 1}
            />
            <circle cx={Math.cos(a) * 47} cy={Math.sin(a) * 47} r="1.6" fill={color} />
          </g>
        );
      })}
    </svg>
  );
}

export default function FireworksBackdrop({ variant = 0 }) {
  const bursts = BURSTS[variant % BURSTS.length];
  return (
    <div className="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">
      {/* soft glow */}
      <div className="absolute -top-24 right-0 size-[28rem] rounded-full bg-gold-400/10 blur-3xl" />
      <div className="absolute -bottom-32 left-1/3 size-[22rem] rounded-full bg-orange-500/10 blur-3xl" />

      {/* bursts are hidden on small phones to keep the text readable */}
      <div className="hidden sm:block">
        {bursts.map((b) => (
          <div
            key={`${b.x}-${b.y}`}
            className="absolute -translate-x-1/2 -translate-y-1/2 animate-burst"
            style={{ left: b.x, top: b.y, animationDelay: b.delay }}
          >
            <Burst size={b.size} color={b.color} />
          </div>
        ))}
      </div>

      {STARS.map(([x, y], i) => (
        <span
          key={`${x}-${y}`}
          className="absolute size-1 animate-twinkle rounded-full bg-gold-300"
          style={{ left: x, top: y, animationDelay: `${(i % 5) * 0.45}s` }}
        />
      ))}
    </div>
  );
}
