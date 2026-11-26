// Bill arithmetic in whole paise (no floating-point rounding errors). The server recalculates everything;
// this only shows the counter staff the same numbers before saving.

export const MAX_DISCOUNT_RATE = 0.2;

export const toPaise = (rupees) => Math.round(Number(rupees) * 100);
export const toRupees = (paise) => (paise / 100).toFixed(2);

/** lines: [{ price, quantity }] → subtotal in paise */
export function subtotalPaise(lines) {
  return lines.reduce((sum, l) => sum + toPaise(l.price) * l.quantity, 0);
}

/**
 * Discount in paise, or an error message.
 * type: '' (none) | 'AMOUNT' | 'PERCENT'; value: string as typed.
 */
export function discountPaise(type, value, subtotal) {
  if (!type || value === '' || value == null) return { paise: 0 };
  if (!/^\d{1,10}(\.\d{1,2})?$/.test(String(value))) return { error: 'Enter a number with at most 2 decimal places.' };
  const v = Number(value);
  const max = Math.round(subtotal * MAX_DISCOUNT_RATE);
  const paise = type === 'PERCENT' ? Math.round((subtotal * v) / 100) : toPaise(v);
  if ((type === 'PERCENT' && v > 20) || paise > max) {
    return { error: `The discount can be at most 20% of the subtotal (₹${toRupees(max)}).` };
  }
  return { paise };
}

/** A random id for one bill, so a double click or retry can never bill twice. */
export function newRequestKey() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID();
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`;
}
