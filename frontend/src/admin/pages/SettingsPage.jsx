import { Save } from 'lucide-react';
import AdminPageHeader from '../components/AdminPageHeader.jsx';
import { SwitchField, TextAreaField } from '../components/Fields.jsx';
import FormField, { FormMessage } from '../../components/auth/FormField.jsx';
import ErrorState from '../../components/ui/ErrorState.jsx';
import PageLoader from '../../components/ui/PageLoader.jsx';
import { useSettingsEditor } from '../useSettingsEditor.js';
import PaymentMethodsCard from './PaymentMethodsCard.jsx';

const MOBILE = /^[6-9]\d{9}$/;

function validate(f) {
  const e = {};
  if (!f.shopName.trim()) e.shopName = 'Please enter the shop name.';
  for (const k of ['phone', 'alternatePhone', 'whatsappNumber']) {
    if (f[k].trim() && !MOBILE.test(f[k].trim())) e[k] = 'Enter a 10-digit mobile number (digits only).';
  }
  if (f.pincode.trim() && !/^\d{6}$/.test(f.pincode.trim())) e.pincode = 'Enter a 6-digit pincode.';
  if (f.email.trim() && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(f.email.trim())) e.email = 'Enter a valid email address.';
  if (f.mapEmbedUrl.trim() && !f.mapEmbedUrl.trim().startsWith('https://www.google.com/maps')) e.mapEmbedUrl = 'Paste a Google Maps link starting with https://www.google.com/maps';
  if (!f.legalNotice.trim()) e.legalNotice = 'The legal notice is required.';
  if (f.safetyTips.split('\n').filter((t) => t.trim()).length > 20) e.safetyTips = 'At most 20 safety tips.';
  return e;
}

/** /admin/settings – the shop's real details. Empty fields show “To be updated” on the website. */
export default function SettingsPage() {
  const { form, set, errors, formError, saving, save, loadError, retry } = useSettingsEditor();
  const header = (
    <AdminPageHeader
      title="Business Settings"
      description="Contact details and notices shown across the website. Leave a field empty if it is not confirmed – the website then shows “To be updated”."
      crumbs={[{ label: 'Business Settings' }]}
    />
  );
  if (loadError) return <>{header}<ErrorState message={loadError.message} onRetry={retry} /></>;
  if (!form) return <PageLoader />;

  return (
    <>
      {header}
      <form
        noValidate
        onSubmit={(e) => {
          e.preventDefault();
          save(validate);
        }}
        className="grid gap-6 xl:grid-cols-2"
      >
        <div className="space-y-6">
          <section className="admin-card space-y-4 p-5" aria-labelledby="shop-title">
            <h2 id="shop-title" className="text-lg font-bold">Shop and address</h2>
            <FormMessage>{formError}</FormMessage>
            <FormField label="Shop name" value={form.shopName} onChange={set('shopName')} error={errors.shopName} maxLength={100} />
            <FormField label="Street address" optional value={form.addressLine1} onChange={set('addressLine1')} error={errors.addressLine1} maxLength={200} />
            <div className="grid gap-4 sm:grid-cols-2">
              <FormField label="Area" optional value={form.area} onChange={set('area')} error={errors.area} maxLength={100} />
              <FormField label="City" optional value={form.city} onChange={set('city')} error={errors.city} maxLength={100} />
              <FormField label="State" optional value={form.state} onChange={set('state')} error={errors.state} maxLength={100} />
              <FormField label="Pincode" optional inputMode="numeric" value={form.pincode} onChange={set('pincode')} error={errors.pincode} maxLength={6} />
            </div>
            <FormField label="Google Maps link" optional value={form.mapEmbedUrl} onChange={set('mapEmbedUrl')} error={errors.mapEmbedUrl} maxLength={500} />
            <SwitchField label="Map shows the approximate area" description="Keep on until the exact shop location is confirmed." checked={form.mapIsApproximate} onChange={set('mapIsApproximate')} />
          </section>
          <section className="admin-card space-y-4 p-5" aria-labelledby="contact-title">
            <h2 id="contact-title" className="text-lg font-bold">Contact</h2>
            <div className="grid gap-4 sm:grid-cols-2">
              <FormField label="Phone" optional inputMode="numeric" value={form.phone} onChange={set('phone')} error={errors.phone} maxLength={10} />
              <FormField label="Alternate phone" optional inputMode="numeric" value={form.alternatePhone} onChange={set('alternatePhone')} error={errors.alternatePhone} maxLength={10} />
              <FormField label="WhatsApp number" optional inputMode="numeric" value={form.whatsappNumber} onChange={set('whatsappNumber')} error={errors.whatsappNumber} maxLength={10} />
              <FormField label="Email" optional type="email" value={form.email} onChange={set('email')} error={errors.email} maxLength={254} />
            </div>
            <SwitchField label="Allow WhatsApp enquiries" description="General questions only – no orders or bookings." checked={form.whatsappEnquiryEnabled} onChange={set('whatsappEnquiryEnabled')} />
            <FormField label="Opening hours" optional value={form.businessHours} onChange={set('businessHours')} error={errors.businessHours} maxLength={200} />
            <FormField label="Licence details" optional value={form.licenceDetails} onChange={set('licenceDetails')} error={errors.licenceDetails} maxLength={300} />
          </section>
        </div>
        <div className="space-y-6">
          <section className="admin-card space-y-4 p-5" aria-labelledby="notice-title">
            <h2 id="notice-title" className="text-lg font-bold">Notices</h2>
            <TextAreaField label="Price note" optional rows={2} value={form.priceDisclaimer} onChange={set('priceDisclaimer')} error={errors.priceDisclaimer} maxLength={500} />
            <TextAreaField label="Legal notice" rows={3} value={form.legalNotice} onChange={set('legalNotice')} error={errors.legalNotice} maxLength={1000} />
            <TextAreaField label="WhatsApp note" optional rows={2} value={form.whatsappNotice} onChange={set('whatsappNotice')} error={errors.whatsappNotice} maxLength={500} />
            <TextAreaField label="Safety tips" optional rows={7} hint="One tip per line (at most 20)." value={form.safetyTips} onChange={set('safetyTips')} error={errors.safetyTips} />
          </section>
          <button type="submit" className="btn-primary w-full sm:w-auto" disabled={saving}>
            <Save className="size-4" aria-hidden="true" /> {saving ? 'Saving…' : 'Save settings'}
          </button>
        </div>
      </form>
      <PaymentMethodsCard />
    </>
  );
}
