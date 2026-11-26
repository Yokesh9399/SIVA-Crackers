import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Save } from 'lucide-react';
import AdminPageHeader from '../components/AdminPageHeader.jsx';
import FormField, { FormMessage } from '../../components/auth/FormField.jsx';
import { SelectField, SwitchField, TextAreaField } from '../components/Fields.jsx';
import { ProductImagesField } from '../components/ImageUploader.jsx';
import { DemoBadge } from '../components/Badges.jsx';
import ErrorState from '../../components/ui/ErrorState.jsx';
import PageLoader from '../../components/ui/PageLoader.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import { useToast } from '../hooks/useToast.js';
import { adminService } from '../adminService.js';
import { formErrorMessage, serverFieldErrors } from '../../utils/authValidation.js';
import { validateProduct } from '../productValidation.js';

const LABELS = {
  name: 'Name', slug: 'Web address', sku: 'Product code', categoryId: 'Category', brandId: 'Brand', description: 'Description',
  packSize: 'Pack size', unit: 'Unit', mrp: 'MRP', price: 'Price', stockQuantity: 'Stock quantity', reorderLevel: '“Few left” level',
  images: 'Photos', featured: 'Featured', visibleOnWebsite: 'Show on website', active: 'Active',
};

const UNITS = [
  ['BOX', 'Box'],
  ['PACKET', 'Packet'],
  ['PIECE', 'Piece'],
  ['BUNDLE', 'Bundle'],
];

const EMPTY = {
  name: '', slug: '', sku: '', categoryId: '', brandId: '', description: '', packSize: '', unit: 'BOX', mrp: '', price: '',
  stockQuantity: '0', reorderLevel: '10', featured: false, visibleOnWebsite: true, active: true, images: [],
};

function fromProduct(p) {
  return {
    name: p.name, slug: p.slug, sku: p.sku, categoryId: String(p.category.id), brandId: String(p.brand.id),
    description: p.description ?? '', packSize: p.packSize, unit: p.unit, mrp: String(p.mrp), price: String(p.price),
    stockQuantity: String(p.stockQuantity), reorderLevel: String(p.reorderLevel), featured: p.featured,
    visibleOnWebsite: p.visibleOnWebsite, active: p.active, images: p.images.map((i) => ({ url: i.url, alt: i.alt ?? '' })),
  };
}

function toRequest(f) {
  return {
    name: f.name.trim(), slug: f.slug.trim(), sku: f.sku.trim(), categoryId: Number(f.categoryId), brandId: Number(f.brandId),
    description: f.description.trim(), packSize: f.packSize.trim(), unit: f.unit, mrp: f.mrp, price: f.price,
    stockQuantity: Number(f.stockQuantity), reorderLevel: Number(f.reorderLevel), featured: f.featured,
    visibleOnWebsite: f.visibleOnWebsite, active: f.active, images: f.images.map((i) => ({ url: i.url, alt: i.alt.trim() })),
  };
}

/** /admin/products/new and /admin/products/:id/edit – loads the data, then shows the form. */
export default function ProductFormPage() {
  const { id } = useParams();
  const editing = Boolean(id);
  const product = useAsync(() => (editing ? adminService.getProduct(id) : Promise.resolve(null)), [id]);
  const categories = useAsync(() => adminService.listCategories(), []);
  const brands = useAsync(() => adminService.listBrands(), []);

  const title = editing ? 'Edit product' : 'Add product';
  const crumbs = [{ label: 'Products', to: '/admin/products' }, { label: title }];
  const loadError = product.error || categories.error || brands.error;

  if (loadError) {
    return (
      <>
        <AdminPageHeader title={title} crumbs={crumbs} />
        <ErrorState
          message={loadError.status === 404 ? 'This product no longer exists.' : loadError.message}
          onRetry={product.error?.status === 404 ? undefined : () => { product.retry(); categories.retry(); brands.retry(); }}
        />
      </>
    );
  }
  if (product.loading || !categories.data || !brands.data) return <PageLoader />;

  return (
    <ProductForm
      key={id ?? 'new'}
      id={id}
      product={product.data}
      initial={product.data ? fromProduct(product.data) : { ...EMPTY }}
      categories={categories.data}
      brands={brands.data}
      title={title}
      crumbs={crumbs}
    />
  );
}

function ProductForm({ id, product, initial, categories, brands, title, crumbs }) {
  const editing = Boolean(id);
  const navigate = useNavigate();
  const toast = useToast();
  const [form, setForm] = useState(initial);
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const set = (field) => (e) => setForm((f) => ({ ...f, [field]: e?.target ? e.target.value : e }));

  async function submit(e) {
    e.preventDefault();
    if (saving) return;
    const problems = validateProduct(form);
    setErrors(problems);
    setFormError(Object.keys(problems).length ? 'Please correct the highlighted fields.' : '');
    if (Object.keys(problems).length) return;
    setSaving(true);
    try {
      const saved = editing ? await adminService.updateProduct(id, toRequest(form)) : await adminService.createProduct(toRequest(form));
      toast.success(editing ? `“${saved.name}” was saved.` : `“${saved.name}” was added.`);
      navigate(`/admin/products/${saved.id}`, { replace: true });
    } catch (err) {
      setErrors(serverFieldErrors(err, LABELS));
      setFormError(formErrorMessage(err));
      setSaving(false);
    }
  }

  const noParents = categories.length === 0 || brands.length === 0;

  return (
    <>
      <AdminPageHeader
        title={title}
        description={editing ? `Changes are shown on the website as soon as you save.` : 'New products are real catalogue items (not demo data).'}
        crumbs={crumbs}
      />
      {editing && product?.isDemo && (
        <p className="mb-4 rounded-xl bg-violet-50 p-3 text-sm text-violet-900 ring-1 ring-violet-200">
          <DemoBadge show /> This is a sample product. It stays marked as demo data; add your real product instead and delete this one.
        </p>
      )}
      {noParents && (
        <FormMessage tone="info">
          Add at least one <Link to="/admin/categories?new=1" className="underline">category</Link> and one{' '}
          <Link to="/admin/brands?new=1" className="underline">brand</Link> before adding products.
        </FormMessage>
      )}

      <form onSubmit={submit} noValidate className="mt-4 grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <section className="admin-card space-y-4 p-5" aria-labelledby="basic">
            <h2 id="basic" className="text-lg font-bold">Basic details</h2>
            <FormMessage>{formError}</FormMessage>
            <FormField label="Product name" value={form.name} onChange={set('name')} error={errors.name} maxLength={150} />
            <div className="grid gap-4 sm:grid-cols-2">
              <FormField label="Product code" value={form.sku} onChange={set('sku')} error={errors.sku} maxLength={40} hint="Shown to visitors, must be unique." />
              <FormField
                label="Web address"
                optional
                value={form.slug}
                onChange={set('slug')}
                error={errors.slug}
                maxLength={170}
                hint="Leave empty to create it from the name."
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <SelectField label="Category" value={form.categoryId} onChange={set('categoryId')} error={errors.categoryId}>
                <option value="">Choose a category…</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                    {!c.active ? ' (inactive)' : ''}
                  </option>
                ))}
              </SelectField>
              <SelectField label="Brand" value={form.brandId} onChange={set('brandId')} error={errors.brandId}>
                <option value="">Choose a brand…</option>
                {brands.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name}
                    {!b.active ? ' (inactive)' : ''}
                  </option>
                ))}
              </SelectField>
            </div>
            <TextAreaField label="Description" optional value={form.description} onChange={set('description')} error={errors.description} maxLength={2000} rows={5} />
          </section>

          <section className="admin-card p-5" aria-label="Photos">
            <ProductImagesField images={form.images} onChange={set('images')} error={errors.images} />
          </section>
        </div>

        <div className="space-y-6">
          <section className="admin-card space-y-4 p-5" aria-labelledby="pricing">
            <h2 id="pricing" className="text-lg font-bold">Pack and price</h2>
            <div className="grid grid-cols-2 gap-3">
              <FormField label="Pack size" value={form.packSize} onChange={set('packSize')} error={errors.packSize} maxLength={50} placeholder="Box of 10" />
              <SelectField label="Unit" value={form.unit} onChange={set('unit')} error={errors.unit}>
                {UNITS.map(([v, l]) => (
                  <option key={v} value={v}>{l}</option>
                ))}
              </SelectField>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <FormField label="MRP (₹)" inputMode="decimal" value={form.mrp} onChange={set('mrp')} error={errors.mrp} maxLength={11} />
              <FormField label="Price (₹)" inputMode="decimal" value={form.price} onChange={set('price')} error={errors.price} maxLength={11} />
            </div>
            <p className="text-xs text-slate-500">The website shows the price as indicative; final prices are confirmed at the shop.</p>
          </section>

          <section className="admin-card space-y-4 p-5" aria-labelledby="stock">
            <h2 id="stock" className="text-lg font-bold">Availability</h2>
            <div className="grid grid-cols-2 gap-3">
              <FormField label="Stock quantity" inputMode="numeric" value={form.stockQuantity} onChange={set('stockQuantity')} error={errors.stockQuantity} maxLength={7} />
              <FormField label="“Few left” at" inputMode="numeric" value={form.reorderLevel} onChange={set('reorderLevel')} error={errors.reorderLevel} maxLength={7} />
            </div>
            <p className="text-xs text-slate-500">0 shows “Out of stock”; at or below the “few left” number shows “Few left”.</p>
          </section>

          <section className="admin-card space-y-1 p-3" aria-label="Visibility">
            <SwitchField label="Show on website" description="Off = hidden from visitors, kept here." checked={form.visibleOnWebsite} onChange={set('visibleOnWebsite')} />
            <SwitchField label="Active" description="Off = discontinued (never shown)." checked={form.active} onChange={set('active')} />
            <SwitchField label="Featured on homepage" checked={form.featured} onChange={set('featured')} />
          </section>

          <div className="flex flex-wrap gap-2">
            <button type="submit" className="btn-primary flex-1" disabled={saving || noParents}>
              <Save className="size-4" aria-hidden="true" /> {saving ? 'Saving…' : editing ? 'Save changes' : 'Add product'}
            </button>
            <Link to={editing ? `/admin/products/${id}` : '/admin/products'} className="admin-btn-secondary">
              Cancel
            </Link>
          </div>
        </div>
      </form>
    </>
  );
}
