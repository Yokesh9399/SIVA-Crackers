import { MessageCircle } from 'lucide-react';
import { useSettings } from '../../hooks/useSettings.js';
import { buildWhatsAppUrl, generalEnquiryMessage, productEnquiryMessage } from '../../utils/whatsapp.js';

/**
 * "Enquire on WhatsApp" button.
 *
 * - Opens WhatsApp with a pre-filled GENERAL ENQUIRY message (product info only).
 * - Never sends anything automatically, never includes quantities or order wording.
 * - Hidden entirely if the owner turns enquiries off in settings.
 * - Disabled with an explanation while the shop's WhatsApp number is not configured.
 */
export default function WhatsAppEnquiryButton({ product, label = 'Enquire on WhatsApp', className = '', compact = false }) {
  const { settings } = useSettings();
  if (!settings?.whatsappEnquiryEnabled) return null;

  const shopName = settings.shopName ?? 'SIVA Crackers';
  const message = product ? productEnquiryMessage(product, shopName) : generalEnquiryMessage(shopName);
  const url = buildWhatsAppUrl(settings.whatsappNumber, message);
  const notice = settings.whatsappNotice ?? 'General enquiries only. Not for orders.';

  const content = (
    <>
      <MessageCircle className="size-4 shrink-0" aria-hidden="true" />
      <span className={compact ? 'sr-only sm:not-sr-only' : ''}>{label}</span>
    </>
  );

  if (!url) {
    return (
      <button
        type="button"
        disabled
        className={`btn-whatsapp w-full ${className}`}
        title="The shop's WhatsApp number will be added soon"
        aria-label={`${label} (WhatsApp number to be updated)`}
      >
        {content}
      </button>
    );
  }

  return (
    <a
      href={url}
      target="_blank"
      rel="noopener noreferrer"
      className={`btn-whatsapp w-full ${className}`}
      title={notice}
      aria-label={`${label}${product ? ` about ${product.name}` : ''} (opens WhatsApp, general enquiry only)`}
    >
      {content}
    </a>
  );
}
