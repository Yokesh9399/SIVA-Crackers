// Date and number formatting for the admin (Indian conventions).

const dateTime = new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Kolkata' });
const dateOnly = new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeZone: 'Asia/Kolkata' });
const money = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** ISO instant → "26 Nov 2026, 10:15 am"; "—" when missing. */
export const fmtDateTime = (iso) => (iso ? dateTime.format(new Date(iso)) : '—');

/** "2026-11-26" (a calendar date) → "26 Nov 2026". */
export const fmtDate = (ymd) => (ymd ? dateOnly.format(new Date(`${ymd}T12:00:00+05:30`)) : '—');

/** Always two decimals: ₹1,250.50 */
export const fmtMoney = (v) => (v === null || v === undefined ? '—' : money.format(Number(v)));

/** Today in Indian time as yyyy-mm-dd (for date inputs). */
export function todayIst() {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Kolkata' }).format(new Date());
}

export function daysAgoIst(days) {
  const d = new Date(`${todayIst()}T12:00:00+05:30`);
  d.setDate(d.getDate() - days);
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Kolkata' }).format(d);
}

/** Signed quantity: +40 / −5 */
export const fmtChange = (n) => (n > 0 ? `+${n}` : `−${Math.abs(n)}`);
