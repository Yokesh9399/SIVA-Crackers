import { useEffect, useState } from 'react';
import { adminService } from './adminService.js';
import { useToast } from './hooks/useToast.js';
import { formErrorMessage, serverFieldErrors } from '../utils/authValidation.js';

const FIELDS = [
  'shopName', 'tagline', 'addressLine1', 'area', 'city', 'state', 'pincode', 'phone', 'alternatePhone', 'whatsappNumber',
  'email', 'businessHours', 'mapEmbedUrl', 'licenceDetails', 'announcementText', 'aboutText', 'priceDisclaimer', 'legalNotice',
  'whatsappNotice',
];

export const SETTINGS_LABELS = {
  shopName: 'Shop name', tagline: 'Tagline', addressLine1: 'Street address', area: 'Area', city: 'City', state: 'State',
  pincode: 'Pincode', phone: 'Phone', alternatePhone: 'Alternate phone', whatsappNumber: 'WhatsApp number', email: 'Email',
  businessHours: 'Opening hours', mapEmbedUrl: 'Map link', licenceDetails: 'Licence details', announcementText: 'Announcement',
  aboutText: 'About text', priceDisclaimer: 'Price note', legalNotice: 'Legal notice', whatsappNotice: 'WhatsApp note',
  highlights: 'Highlights', safetyTips: 'Safety tips',
};

function toForm(s) {
  const f = Object.fromEntries(FIELDS.map((k) => [k, s[k] ?? '']));
  return {
    ...f,
    whatsappEnquiryEnabled: s.whatsappEnquiryEnabled,
    mapIsApproximate: s.mapIsApproximate,
    announcementActive: s.announcementActive,
    highlights: s.highlights.map((h) => ({ title: h.title, text: h.text })),
    safetyTips: s.safetyTips.join('\n'),
  };
}

function toRequest(f) {
  return {
    ...Object.fromEntries(FIELDS.map((k) => [k, f[k].trim()])),
    whatsappEnquiryEnabled: f.whatsappEnquiryEnabled,
    mapIsApproximate: f.mapIsApproximate,
    announcementActive: f.announcementActive,
    highlights: f.highlights.map((h) => ({ title: h.title.trim(), text: h.text.trim() })).filter((h) => h.title || h.text),
    safetyTips: f.safetyTips.split('\n').map((t) => t.trim()).filter(Boolean),
  };
}

/**
 * Loads the business settings, keeps an editable copy and saves the whole record
 * (PUT /api/admin/settings replaces everything, so each page edits its part and keeps the rest).
 */
export function useSettingsEditor() {
  const toast = useToast();
  const [form, setForm] = useState(null);
  const [loadError, setLoadError] = useState(null);
  const [attempt, setAttempt] = useState(0);
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let cancelled = false;
    adminService.getSettings().then(
      (s) => !cancelled && setForm(toForm(s)),
      (err) => !cancelled && setLoadError(err),
    );
    return () => {
      cancelled = true;
    };
  }, [attempt]);

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e?.target ? e.target.value : e }));

  async function save(validate) {
    if (saving) return;
    const problems = validate ? validate(form) : {};
    setErrors(problems);
    setFormError(Object.keys(problems).length ? 'Please correct the highlighted fields.' : '');
    if (Object.keys(problems).length) return;
    setSaving(true);
    try {
      setForm(toForm(await adminService.updateSettings(toRequest(form))));
      toast.success('Saved. The website now shows the new details.');
    } catch (err) {
      setErrors(serverFieldErrors(err, SETTINGS_LABELS));
      setFormError(formErrorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  return {
    form, setForm, set, errors, formError, saving, save, loadError,
    retry: () => {
      setLoadError(null);
      setAttempt((n) => n + 1);
    },
  };
}
