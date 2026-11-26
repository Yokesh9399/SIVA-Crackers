import { useState } from 'react';
import { Plus, Pencil, Trash2, Save } from 'lucide-react';
import AdminPageHeader from '../components/AdminPageHeader.jsx';
import ConfirmDialog from '../components/ConfirmDialog.jsx';
import Modal from '../components/Modal.jsx';
import { SelectField, SwitchField, TextAreaField } from '../components/Fields.jsx';
import { ActiveBadge, Badge } from '../components/Badges.jsx';
import FormField, { FormMessage } from '../../components/auth/FormField.jsx';
import ErrorState from '../../components/ui/ErrorState.jsx';
import PageLoader from '../../components/ui/PageLoader.jsx';
import Skeleton from '../../components/ui/Skeleton.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import { useToast } from '../hooks/useToast.js';
import { adminService } from '../adminService.js';
import { useSettingsEditor } from '../useSettingsEditor.js';
import { formErrorMessage, serverFieldErrors } from '../../utils/authValidation.js';

const BANNER_LABELS = { eyebrow: 'Small heading', title: 'Title', subtitle: 'Text', ctaLabel: 'Button text', ctaLink: 'Button link', theme: 'Colour', displayOrder: 'Display order' };
const INTERNAL_LINK = /^\/(?!\/)[A-Za-z0-9\-._~/?=&%#]*$/;

function BannerForm({ banner, onClose, onSaved }) {
  const toast = useToast();
  const [form, setForm] = useState({
    eyebrow: banner?.eyebrow ?? '', title: banner?.title ?? '', subtitle: banner?.subtitle ?? '', ctaLabel: banner?.ctaLabel ?? '',
    ctaLink: banner?.ctaLink ?? '', theme: banner?.theme ?? 'gold', displayOrder: String(banner?.displayOrder ?? 0), active: banner?.active ?? true,
  });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e?.target ? e.target.value : e }));

  async function submit(e) {
    e.preventDefault();
    if (saving) return;
    const p = {};
    if (!form.title.trim()) p.title = 'Please enter a title.';
    if (form.ctaLink.trim() && !INTERNAL_LINK.test(form.ctaLink.trim())) p.ctaLink = 'Use a page of this website, e.g. /products or /price-list.';
    if (form.ctaLabel.trim() && !form.ctaLink.trim()) p.ctaLink = 'Add the page the button opens.';
    if (form.ctaLink.trim() && !form.ctaLabel.trim()) p.ctaLabel = 'Add the button text.';
    if (!/^\d{1,4}$/.test(form.displayOrder)) p.displayOrder = 'Enter a whole number from 0 to 9999.';
    setErrors(p);
    setFormError('');
    if (Object.keys(p).length) return;
    setSaving(true);
    const body = { ...Object.fromEntries(['eyebrow', 'title', 'subtitle', 'ctaLabel', 'ctaLink'].map((k) => [k, form[k].trim()])),
      theme: form.theme, displayOrder: Number(form.displayOrder), active: form.active };
    try {
      if (banner) await adminService.updateBanner(banner.id, body);
      else await adminService.createBanner(body);
      toast.success(banner ? 'Slide saved.' : 'Slide added.');
      onSaved();
    } catch (err) {
      setErrors(serverFieldErrors(err, BANNER_LABELS));
      setFormError(formErrorMessage(err));
      setSaving(false);
    }
  }

  return (
    <Modal
      title={banner ? 'Edit slide' : 'Add slide'}
      onClose={onClose}
      closeDisabled={saving}
      footer={
        <>
          <button type="button" className="admin-btn-secondary" onClick={onClose} disabled={saving}>Cancel</button>
          <button type="submit" form="banner-form" className="btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
        </>
      }
    >
      <form id="banner-form" onSubmit={submit} noValidate className="space-y-4">
        <FormMessage>{formError}</FormMessage>
        <FormField label="Small heading" optional value={form.eyebrow} onChange={set('eyebrow')} error={errors.eyebrow} maxLength={80} />
        <FormField label="Title" value={form.title} onChange={set('title')} error={errors.title} maxLength={120} />
        <TextAreaField label="Text" optional rows={2} value={form.subtitle} onChange={set('subtitle')} error={errors.subtitle} maxLength={300} />
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label="Button text" optional value={form.ctaLabel} onChange={set('ctaLabel')} error={errors.ctaLabel} maxLength={40} />
          <FormField label="Button link" optional value={form.ctaLink} onChange={set('ctaLink')} error={errors.ctaLink} maxLength={200} placeholder="/products" hint="A page of this website." />
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <SelectField label="Colour" value={form.theme} onChange={set('theme')} error={errors.theme}>
            <option value="gold">Gold</option>
            <option value="orange">Orange</option>
            <option value="blue">Blue</option>
          </SelectField>
          <FormField label="Display order" inputMode="numeric" value={form.displayOrder} onChange={set('displayOrder')} error={errors.displayOrder} maxLength={4} />
        </div>
        <SwitchField label="Show on homepage" checked={form.active} onChange={set('active')} />
      </form>
    </Modal>
  );
}

function Banners() {
  const toast = useToast();
  const [reload, setReload] = useState(0);
  const [editing, setEditing] = useState(null);
  const [toDelete, setToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const list = useAsync(() => adminService.listBanners(), [reload]);

  async function confirmDelete() {
    setDeleting(true);
    try {
      await adminService.deleteBanner(toDelete.id);
      toast.success('Slide deleted.');
      setToDelete(null);
      setReload((n) => n + 1);
    } catch (err) {
      toast.error(err.message);
    } finally {
      setDeleting(false);
    }
  }

  return (
    <section className="admin-card p-5" aria-labelledby="slides-title">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 id="slides-title" className="text-lg font-bold">Homepage slides</h2>
          <p className="text-sm text-slate-500">The rotating banner at the top of the homepage.</p>
        </div>
        <button type="button" className="btn-primary" onClick={() => setEditing('new')}>
          <Plus className="size-4" aria-hidden="true" /> Add slide
        </button>
      </div>
      {list.error ? (
        <ErrorState message={list.error.message} onRetry={list.retry} />
      ) : !list.data ? (
        <Skeleton className="h-32" />
      ) : list.data.length === 0 ? (
        <p className="rounded-xl bg-slate-50 p-4 text-sm text-slate-600">No slides yet. The homepage shows its standard welcome slide.</p>
      ) : (
        <ul className="divide-y divide-slate-100">
          {list.data.map((b) => (
            <li key={b.id} className="flex items-start gap-3 py-3">
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="font-semibold break-words">{b.title}</p>
                  <ActiveBadge active={b.active} />
                  <Badge color="blue">{b.theme}</Badge>
                </div>
                {b.subtitle && <p className="line-clamp-2 text-sm text-slate-600">{b.subtitle}</p>}
                {b.ctaLabel && <p className="text-xs text-slate-500">Button “{b.ctaLabel}” → {b.ctaLink}</p>}
              </div>
              <button type="button" className="grid size-10 shrink-0 place-items-center rounded-lg text-slate-500 hover:bg-slate-100" onClick={() => setEditing(b)} aria-label={`Edit slide ${b.title}`}>
                <Pencil className="size-4" aria-hidden="true" />
              </button>
              <button type="button" className="grid size-10 shrink-0 place-items-center rounded-lg text-slate-500 hover:bg-red-50 hover:text-red-600" onClick={() => setToDelete(b)} aria-label={`Delete slide ${b.title}`}>
                <Trash2 className="size-4" aria-hidden="true" />
              </button>
            </li>
          ))}
        </ul>
      )}
      {editing && (
        <BannerForm
          banner={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null);
            setReload((n) => n + 1);
          }}
        />
      )}
      {toDelete && (
        <ConfirmDialog
          title="Delete slide?"
          message={<p>The slide “{toDelete.title}” will be deleted permanently. To keep it for later, switch off “Show on homepage” instead.</p>}
          busy={deleting}
          onConfirm={confirmDelete}
          onCancel={() => setToDelete(null)}
        />
      )}
    </section>
  );
}

function HomepageText() {
  const { form, setForm, set, errors, formError, saving, save, loadError, retry } = useSettingsEditor();
  if (loadError) return <ErrorState message={loadError.message} onRetry={retry} />;
  if (!form) return <PageLoader />;

  const setHighlight = (i, key) => (e) =>
    setForm((f) => ({ ...f, highlights: f.highlights.map((h, j) => (j === i ? { ...h, [key]: e.target.value } : h)) }));

  return (
    <form
      className="admin-card space-y-4 p-5"
      aria-labelledby="text-title"
      noValidate
      onSubmit={(e) => {
        e.preventDefault();
        save();
      }}
    >
      <div>
        <h2 id="text-title" className="text-lg font-bold">Homepage text</h2>
        <p className="text-sm text-slate-500">Only write facts about the shop – avoid claims you cannot support.</p>
      </div>
      <FormMessage>{formError}</FormMessage>
      <FormField label="Tagline" optional value={form.tagline} onChange={set('tagline')} error={errors.tagline} maxLength={200} />
      <TextAreaField label="Announcement bar" optional rows={2} value={form.announcementText} onChange={set('announcementText')} error={errors.announcementText} maxLength={300} />
      <SwitchField label="Show the announcement bar" checked={form.announcementActive} onChange={set('announcementActive')} />
      <TextAreaField label="About the shop" optional rows={5} value={form.aboutText} onChange={set('aboutText')} error={errors.aboutText} maxLength={2000} />
      <fieldset>
        <legend className="mb-2 text-sm font-medium text-navy-900">Highlights (“Why visit us”, at most 8)</legend>
        <ul className="space-y-3">
          {form.highlights.map((h, i) => (
            <li key={i} className="grid gap-2 rounded-xl p-3 ring-1 ring-slate-200 sm:grid-cols-[1fr_2fr_auto]">
              <input className="admin-input" aria-label={`Highlight ${i + 1} title`} value={h.title} maxLength={80} onChange={setHighlight(i, 'title')} placeholder="Title" />
              <input className="admin-input" aria-label={`Highlight ${i + 1} text`} value={h.text} maxLength={300} onChange={setHighlight(i, 'text')} placeholder="Text" />
              <button type="button" className="admin-btn-secondary" onClick={() => setForm((f) => ({ ...f, highlights: f.highlights.filter((_, j) => j !== i) }))} aria-label={`Remove highlight ${i + 1}`}>
                <Trash2 className="size-4" aria-hidden="true" />
              </button>
            </li>
          ))}
        </ul>
        {form.highlights.length < 8 && (
          <button type="button" className="admin-btn-secondary mt-3" onClick={() => setForm((f) => ({ ...f, highlights: [...f.highlights, { title: '', text: '' }] }))}>
            <Plus className="size-4" aria-hidden="true" /> Add highlight
          </button>
        )}
        {errors.highlights && <p className="mt-1 text-xs font-medium text-red-600">{errors.highlights}</p>}
      </fieldset>
      <button type="submit" className="btn-primary" disabled={saving}>
        <Save className="size-4" aria-hidden="true" /> {saving ? 'Saving…' : 'Save homepage text'}
      </button>
    </form>
  );
}

/** /admin/content – homepage slides and text. */
export default function ContentPage() {
  return (
    <>
      <AdminPageHeader title="Website Content" description="What visitors see on the homepage." crumbs={[{ label: 'Website Content' }]} />
      <div className="grid gap-6 xl:grid-cols-2">
        <Banners />
        <HomepageText />
      </div>
    </>
  );
}
