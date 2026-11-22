import { useSettings } from '../../hooks/useSettings.js';
import { formatAddress, formatDate } from '../../utils/format.js';
import { IS_DEMO_DATA } from '../../config/env.js';

/**
 * Only visible when printing: shop name, location, document title, date and
 * the price disclaimer. Unknown business details are simply left out.
 */
export default function PrintHeader({ title, note }) {
  const { settings } = useSettings();
  return (
    <div className="mb-6 hidden border-b-2 border-black pb-3 text-black print:block">
      <div className="flex items-end justify-between gap-6">
        <div>
          <p className="font-display text-2xl font-extrabold">{settings?.shopName ?? 'SIVA Crackers'}</p>
          <p className="text-xs">{formatAddress(settings?.address)}</p>
          {settings?.phone && <p className="text-xs">Phone: {settings.phone}</p>}
        </div>
        <div className="text-right">
          <p className="text-lg font-bold">{title}</p>
          <p className="text-xs">Printed on {formatDate()}</p>
        </div>
      </div>
      <p className="mt-2 text-[0.7rem]">
        {settings?.priceDisclaimer} This is not an invoice or an order. Firecrackers are sold only in person at the shop.
      </p>
      {note && <p className="mt-1 text-[0.7rem]">{note}</p>}
      {IS_DEMO_DATA && <p className="mt-1 text-[0.7rem] font-bold">DEMONSTRATION DATA – sample products and prices for testing only.</p>}
    </div>
  );
}
