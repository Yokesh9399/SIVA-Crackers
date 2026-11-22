import { Info } from 'lucide-react';
import PageHeader from '../components/ui/PageHeader.jsx';
import ContactBlock from '../components/home/ContactBlock.jsx';
import VisitSteps from '../components/business/VisitSteps.jsx';
import { useDocumentTitle } from '../hooks/useDocumentTitle.js';
import { useSettings } from '../hooks/useSettings.js';

/**
 * /contact – reuses the homepage ContactBlock (address, phone, email, hours,
 * general WhatsApp enquiry and the approximate area map). All unknown details
 * show "To be updated" until the owner provides them.
 */
export default function ContactPage() {
  useDocumentTitle('Contact Us');
  const { settings } = useSettings();

  return (
    <>
      <PageHeader
        title="Contact Us"
        subtitle="Visit our shop in Eachanari, Coimbatore, or send us a general question."
        crumbs={[{ label: 'Contact Us' }]}
      />

      <ContactBlock />

      <div className="container-page space-y-12 pb-14">
        <section aria-labelledby="enquiry-title" className="rounded-3xl bg-white p-6 ring-1 ring-slate-200/70 sm:p-8">
          <h2 id="enquiry-title" className="text-xl font-bold">
            General enquiries
          </h2>
          <ul className="mt-4 space-y-2 text-sm text-slate-700">
            <li>• Ask about products, pack sizes, brands or shop timings by phone, WhatsApp or email once those details are added above.</li>
            <li>• Enquiries are for information only. We do not take, confirm or process firecracker orders, bookings or payments online or by WhatsApp.</li>
            <li>• For prices, see the Price List – final prices are confirmed at the shop.</li>
          </ul>
          {settings?.whatsappNotice && (
            <p className="mt-4 flex gap-2 rounded-xl bg-gold-400/15 p-3 text-xs text-navy-900">
              <Info className="mt-0.5 size-4 shrink-0 text-orange-600" aria-hidden="true" />
              {settings.whatsappNotice}
            </p>
          )}
        </section>

        <VisitSteps />
      </div>
    </>
  );
}
