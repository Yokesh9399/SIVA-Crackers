// Client-side checks that mirror the server rules, so mistakes are shown before submitting.
// The server always checks again; these only improve the experience.

export const PASSWORD_RULES = '8–72 characters with at least one letter and one number';

const EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
const PHONE = /^(\+?91[\s-]?|0)?[6-9][0-9]{4}[\s-]?[0-9]{5}$/;

export function validateEmail(value) {
  if (!value.trim()) return 'Please enter your email address.';
  if (!EMAIL.test(value.trim())) return 'Please enter a valid email address.';
  return null;
}

export function validatePhone(value, { required = false } = {}) {
  if (!value.trim()) return required ? 'Please enter your mobile number.' : null;
  if (!PHONE.test(value.trim())) return 'Please enter a 10-digit Indian mobile number.';
  return null;
}

export function validateName(value) {
  if (!value.trim()) return 'Please enter your name.';
  if (value.trim().length > 100) return 'Name must be at most 100 characters.';
  return null;
}

export function validatePassword(value) {
  if (!value) return 'Please enter a password.';
  const bytes = new TextEncoder().encode(value).length;
  if (value.length < 8 || bytes > 72 || !/[A-Za-z]/.test(value) || !/[0-9]/.test(value)) {
    return `Password must be ${PASSWORD_RULES}.`;
  }
  return null;
}

/** Server field errors [{ field, message }] → { field: "Field message" }. */
export function serverFieldErrors(error, labels = {}) {
  const out = {};
  for (const { field, message } of error?.fieldErrors ?? []) {
    if (!out[field]) out[field] = `${labels[field] ?? 'This field'} ${message}.`;
  }
  return out;
}

/** Banner text for a failed form: field problems are shown next to the fields themselves. */
export function formErrorMessage(error) {
  return error?.fieldErrors?.length ? 'Please correct the highlighted fields.' : error?.message;
}

/** Only allow returning to a page on this site (never an external or protocol-relative URL). */
export function safeReturnPath(path, fallback = '/account') {
  if (typeof path !== 'string' || !path.startsWith('/') || path.startsWith('//') || path.startsWith('/\\')) return fallback;
  if (path.startsWith('/login') || path.startsWith('/register')) return fallback;
  return path;
}
