// WhatsApp enquiry helpers.
//
// LEGAL NOTE: WhatsApp is used for GENERAL ENQUIRIES ONLY. The messages below
// contain product information, never quantities, totals, delivery details or
// order language. Nothing is submitted automatically: the link only opens
// WhatsApp with a pre-filled message that the customer chooses to send.
// Do not add ordering or booking text here until the legal position is cleared.

import { formatCurrency } from './format.js';

/** Normalises an Indian number to the international form WhatsApp expects (91XXXXXXXXXX). */
export function normaliseWhatsAppNumber(number) {
  if (!number) return null;
  const digits = String(number).replace(/\D/g, '');
  if (digits.length === 10) return `91${digits}`;
  if (digits.length === 12 && digits.startsWith('91')) return digits;
  return null; // unknown format: treat as not configured rather than guess
}

/** Returns a wa.me link, or null when the number is missing/invalid. */
export function buildWhatsAppUrl(number, message) {
  const normalised = normaliseWhatsAppNumber(number);
  if (!normalised) return null;
  return `https://wa.me/${normalised}?text=${encodeURIComponent(message)}`;
}

/** Pre-filled text for a product enquiry. */
export function productEnquiryMessage(product, shopName = 'SIVA Crackers') {
  const lines = [
    `Hello ${shopName}, I have a general enquiry about this product:`,
    '',
    `Product: ${product.name}`,
    product.brand?.name && `Brand: ${product.brand.name}`,
    product.category?.name && `Category: ${product.category.name}`,
    product.packSize && `Pack size: ${product.packSize}`,
    product.price != null && `Indicative price on website: ${formatCurrency(product.price)}`,
    '',
    'Could you share more details about this item?',
    '(General enquiry only. Not an order.)',
  ];
  return lines.filter((line) => line !== false && line !== undefined && line !== null).join('\n');
}

/** Pre-filled text for a general shop enquiry. */
export function generalEnquiryMessage(shopName = 'SIVA Crackers') {
  return `Hello ${shopName}, I have a general enquiry about your products. (General enquiry only. Not an order.)`;
}
