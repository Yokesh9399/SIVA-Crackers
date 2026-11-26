import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Plus, Pencil, Trash2, Search } from 'lucide-react';
import AdminPageHeader from '../components/AdminPageHeader.jsx';
import ConfirmDialog from '../components/ConfirmDialog.jsx';
import Modal from '../components/Modal.jsx';
import ImageUploader, { AdminImage } from '../components/ImageUploader.jsx';
import { SwitchField, TextAreaField } from '../components/Fields.jsx';
import { ActiveBadge, DemoBadge } from '../components/Badges.jsx';
import FormField, { FormMessage } from '../../components/auth/FormField.jsx';
import ErrorState from '../../components/ui/ErrorState.jsx';
import EmptyState from '../../components/ui/EmptyState.jsx';
import Skeleton from '../../components/ui/Skeleton.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import { useToast } from '../hooks/useToast.js';
import { adminService } from '../adminService.js';
import { formErrorMessage, serverFieldErrors } from '../../utils/authValidation.js';

const LABELS = { name: 'Name', slug: 'Web address', description: 'Description', logoUrl: 'Logo', displayOrder: 'Display order' };

function BrandForm({ brand, onClose, onSaved }) {
  const toast = useToast();
  const [form, setForm] = useState({
    name: brand?.name ?? '',
    slug: brand?.slug ?? '',
    description: brand?.description ?? '',
    logoUrl: brand?.logoUrl ?? null,
    displayOrder: String(brand?.displayOrder ?? 0),
    active: brand?.active ?? true,
  });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e?.target ? e.target.value : e }));

  async function submit(e) {
    e.preventDefault();
    if (saving) return;
    const problems = {};
    if (!form.name.trim()) problems.name = 'Please enter a brand name.';
    if (form.slug && !/^[a-z0-9]+(-[a-z0-9]+)*$/.test(form.slug)) problems.slug = 'Use only lower-case letters, numbers and single hyphens.';
    if (!/^\d{1,4}$/.test(form.displayOrder)) problems.displayOrder = 'Enter a whole number from 0 to 9999.';
    setErrors(problems);
    setFormError('');
    if (Object.keys(problems).length) return;
    setSaving(true);
    const body = { name: form.name.trim(), slug: form.slug.trim(), description: form.description.trim(), logoUrl: form.logoUrl ?? '',
      displayOrder: Number(form.displayOrder), active: form.active };
    try {
      const saved = brand ? await adminService.updateBrand(brand.id, body) : await adminService.createBrand(body);
      toast.success(brand ? `Brand “${saved.name}” was saved.` : `Brand “${saved.name}” was added.`);
      onSaved();
    } catch (err) {
      setErrors(serverFieldErrors(err, LABELS));
      setFormError(formErrorMessage(err));
      setSaving(false);
    }
  }

  return (
    <Modal
      title={brand ? 'Edit brand' : 'Add brand'}
      onClose={onClose}
      closeDisabled={saving}
      footer={
        <>
          <button type="button" className="admin-btn-secondary" onClick={onClose} disabled={saving}>Cancel</button>
          <button type="submit" form="brand-form" className="btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
        </>
      }
    >
      <form id="brand-form" onSubmit={submit} noValidate className="space-y-4">
        <FormMessage>{formError}</FormMessage>
        <FormField label="Name" value={form.name} onChange={set('name')} error={errors.name} maxLength={80} />
        <FormField label="Web address" optional value={form.slug} onChange={set('slug')} error={errors.slug} maxLength={100} hint="Leave empty to create it from the name." />
        <TextAreaField label="Description" optional rows={3} value={form.description} onChange={set('description')} error={errors.description} maxLength={500} />
        <ImageUploader label="Logo" folder="brands" value={form.logoUrl} onChange={set('logoUrl')} />
        {errors.logoUrl && <p className="text-xs font-medium text-red-600">{errors.logoUrl}</p>}
        <FormField label="Display order" inputMode="numeric" value={form.displayOrder} onChange={set('displayOrder')} error={errors.displayOrder} maxLength={4} hint="Lower numbers come first." />
        <SwitchField label="Active" description="Inactive brands and their products are hidden from the website." checked={form.active} onChange={set('active')} />
      </form>
    </Modal>
  );
}

/** /admin/brands */
export default function BrandsPage() {
  const toast = useToast();
  const [params, setParams] = useSearchParams();
  const [search, setSearch] = useState('');
  const [editing, setEditing] = useState(params.get('new') ? 'new' : null);
  const [toDelete, setToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [reload, setReload] = useState(0);
  const list = useAsync(() => adminService.listBrands(), [reload]);

  const q = search.trim().toLowerCase();
  const rows = (list.data ?? []).filter((b) => !q || b.name.toLowerCase().includes(q) || b.slug.includes(q));

  const closeForm = () => {
    setEditing(null);
    if (params.get('new')) setParams({}, { replace: true });
  };

  async function confirmDelete() {
    setDeleting(true);
    try {
      await adminService.deleteBrand(toDelete.id);
      toast.success(`Brand “${toDelete.name}” was deleted.`);
      setToDelete(null);
      setReload((n) => n + 1);
    } catch (err) {
      toast.error(err.message);
      setToDelete(null);
    } finally {
      setDeleting(false);
    }
  }

  return (
    <>
      <AdminPageHeader
        title="Brands"
        description="Manufacturers shown on the Brands page and product pages."
        crumbs={[{ label: 'Brands' }]}
        actions={
          <button type="button" className="btn-primary" onClick={() => setEditing('new')}>
            <Plus className="size-4" aria-hidden="true" /> Add brand
          </button>
        }
      />
      <label className="relative mb-4 block max-w-sm">
        <span className="sr-only">Search brands</span>
        <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" aria-hidden="true" />
        <input type="search" className="admin-input pl-9" placeholder="Search brands…" value={search} onChange={(e) => setSearch(e.target.value)} />
      </label>

      {list.error ? (
        <ErrorState message={list.error.message} onRetry={list.retry} />
      ) : list.loading && !list.data ? (
        <div className="space-y-2">{Array.from({ length: 5 }, (_, i) => <Skeleton key={i} className="h-16" />)}</div>
      ) : rows.length === 0 ? (
        <EmptyState title={q ? 'No brands match your search' : 'No brands yet'} message={q ? 'Try another name.' : 'Add the brands you stock.'} />
      ) : (
        <ul className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
          {rows.map((b) => (
            <li key={b.id} className="admin-card flex gap-3 p-4">
              <AdminImage src={b.logoUrl} alt="" className="size-14 shrink-0 rounded-full" />
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <h2 className="font-semibold break-words">{b.name}</h2>
                  <ActiveBadge active={b.active} />
                  <DemoBadge show={b.isDemo} />
                </div>
                <p className="text-xs text-slate-500">
                  <Link to={`/admin/products?brandId=${b.id}`} className="hover:text-orange-600 hover:underline">
                    {b.productCount} {b.productCount === 1 ? 'product' : 'products'}
                  </Link>{' '}
                  · {b.visibleProductCount} on website
                </p>
              </div>
              <div className="flex shrink-0 flex-col gap-1">
                <button type="button" className="grid size-10 place-items-center rounded-lg text-slate-500 hover:bg-slate-100" onClick={() => setEditing(b)} aria-label={`Edit ${b.name}`}>
                  <Pencil className="size-4" aria-hidden="true" />
                </button>
                <button type="button" className="grid size-10 place-items-center rounded-lg text-slate-500 hover:bg-red-50 hover:text-red-600" onClick={() => setToDelete(b)} aria-label={`Delete ${b.name}`}>
                  <Trash2 className="size-4" aria-hidden="true" />
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {editing && (
        <BrandForm
          brand={editing === 'new' ? null : editing}
          onClose={closeForm}
          onSaved={() => {
            closeForm();
            setReload((n) => n + 1);
          }}
        />
      )}
      {toDelete && (
        <ConfirmDialog
          title={toDelete.productCount > 0 ? 'This brand cannot be deleted' : 'Delete brand?'}
          message={
            toDelete.productCount > 0 ? (
              <p>
                <strong>{toDelete.name}</strong> still has {toDelete.productCount} products, so it cannot be deleted. Move those products to
                another brand first, or switch the brand to inactive to hide it.
              </p>
            ) : (
              <p><strong>{toDelete.name}</strong> and its logo will be deleted permanently. This cannot be undone.</p>
            )
          }
          busy={deleting}
          onConfirm={toDelete.productCount > 0 ? null : confirmDelete}
          onCancel={() => setToDelete(null)}
        />
      )}
    </>
  );
}
