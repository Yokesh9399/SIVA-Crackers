const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 });
const inrWhole = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });

/** 1125 → "₹1,125", 12.5 → "₹12.50". Returns "" for missing values. */
export function formatCurrency(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '';
  const n = Number(value);
  return Number.isInteger(n) ? inrWhole.format(n) : inr.format(n);
}

/** Whole-number discount percentage, or null when there is no real discount. */
export function discountPercent(mrp, price) {
  if (!mrp || !price || price >= mrp) return null;
  return Math.round(((mrp - price) / mrp) * 100);
}

/** Joins the known parts of the shop address. Unknown (null) parts are skipped. */
export function formatAddress(address) {
  if (!address) return '';
  const { line1, area, city, state, pincode } = address;
  const cityLine = [city, pincode].filter(Boolean).join(' - ');
  return [line1, area, cityLine, state].filter(Boolean).join(', ');
}

const UNIT_LABELS = { BOX: 'Box', PACKET: 'Packet', PIECE: 'Piece', BUNDLE: 'Bundle' };

/** BOX → "Box" */
export function formatUnit(unit) {
  return UNIT_LABELS[unit] ?? unit ?? '';
}

/** 22 Nov 2026 */
export function formatDate(date = new Date()) {
  return new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }).format(date);
}

/** Digits only, for tel: links. */
export function telHref(phone) {
  return phone ? `tel:${phone.replace(/[^\d+]/g, '')}` : undefined;
}
