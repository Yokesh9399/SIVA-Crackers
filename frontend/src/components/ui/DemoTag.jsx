/**
 * Small "Demo" pill marking an item as demonstration data.
 * Renders nothing when `show` is false (real data from the owner).
 */
export default function DemoTag({ show = true, dark = false, className = '' }) {
  if (!show) return null;
  return (
    <span
      title="Demonstration data – not a real product, brand, price or stock level"
      className={`inline-flex items-center rounded-full px-2 py-0.5 text-[0.65rem] font-bold tracking-wider uppercase ${
        dark ? 'bg-white/15 text-gold-300' : 'bg-violet-100 text-violet-700 ring-1 ring-violet-200'
      } ${className}`}
    >
      Demo
    </span>
  );
}
