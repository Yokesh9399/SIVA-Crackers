import { Clock, Mail, MapPin, Phone } from 'lucide-react';
import { useSettings } from '../../hooks/useSettings.js';
import { formatAddress, telHref } from '../../utils/format.js';
import PlaceholderValue from '../ui/PlaceholderValue.jsx';
import SectionHeading from '../ui/SectionHeading.jsx';
import WhatsAppEnquiryButton from '../product/WhatsAppEnquiryButton.jsx';
import Reveal from '../ui/Reveal.jsx';

function Row({ icon: Icon, label, children }) {
  return (
    <li className="flex gap-4">
      <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-gold-400/15 text-orange-600">
        <Icon className="size-5" aria-hidden="true" />
      </span>
      <span>
        <span className="block text-xs font-semibold tracking-wider text-slate-500 uppercase">{label}</span>
        <span className="block text-sm text-navy-900">{children}</span>
      </span>
    </li>
  );
}

/** Contact details and area map. Missing details show "To be updated". */
export default function ContactBlock() {
  const { settings } = useSettings();
  if (!settings) return null;

  return (
    <section className="container-page py-14 sm:py-20" aria-labelledby="contact-heading">
      <SectionHeading
        eyebrow="Visit us"
        title={<span id="contact-heading">Find our shop</span>}
        subtitle="All purchases are made in person at our shop in Eachanari, Coimbatore."
      />

      <div className="grid gap-6 lg:grid-cols-5">
        <Reveal className="lg:col-span-2">
          <div className="h-full rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200/70 sm:p-8">
            <ul className="space-y-5">
              <Row icon={MapPin} label="Address">
                {formatAddress(settings.address)}
                {!settings.address?.line1 && (
                  <span className="block text-xs text-slate-500">
                    Street address: <PlaceholderValue value={null} />
                  </span>
                )}
              </Row>
              <Row icon={Phone} label="Phone">
                {settings.phone ? (
                  <a href={telHref(settings.phone)} className="hover:text-orange-600">
                    {settings.phone}
                  </a>
                ) : (
                  <PlaceholderValue value={null} />
                )}
              </Row>
              <Row icon={Mail} label="Email">
                <PlaceholderValue value={settings.email} />
              </Row>
              <Row icon={Clock} label="Shop hours">
                <PlaceholderValue value={settings.businessHours} />
              </Row>
            </ul>

            <div className="mt-8 space-y-2">
              <WhatsAppEnquiryButton label="Send a general enquiry" />
              {settings.whatsappEnquiryEnabled && (
                <p className="text-center text-xs text-slate-500">{settings.whatsappNotice}</p>
              )}
            </div>
          </div>
        </Reveal>

        <Reveal className="lg:col-span-3" delay={100}>
          <div className="relative h-72 overflow-hidden rounded-3xl bg-slate-200 shadow-sm ring-1 ring-slate-200/70 sm:h-full sm:min-h-[22rem]">
            {settings.mapEmbedUrl ? (
              <iframe
                title="Map of Eachanari, Coimbatore"
                src={settings.mapEmbedUrl}
                className="absolute inset-0 size-full border-0"
                loading="lazy"
                referrerPolicy="no-referrer-when-downgrade"
              />
            ) : (
              <div className="grid size-full place-items-center text-sm text-slate-500">Map to be updated</div>
            )}
            {settings.mapIsApproximate && (
              <p className="absolute top-3 left-3 rounded-full bg-navy-900/85 px-3 py-1.5 text-xs font-medium text-white backdrop-blur">
                Area map · exact shop location to be updated
              </p>
            )}
          </div>
        </Reveal>
      </div>
    </section>
  );
}
