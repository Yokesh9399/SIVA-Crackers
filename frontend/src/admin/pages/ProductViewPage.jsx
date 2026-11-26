import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Pencil, Trash2, ExternalLink } from 'lucide-react';
import AdminPageHeader from '../components/AdminPageHeader.jsx';
import ConfirmDialog from '../components/ConfirmDialog.jsx';
import { SwitchField } from '../components/Fields.jsx';
import { AdminImage } from '../components/ImageUploader.jsx';
import { StockBadge, VisibilityBadge, DemoBadge } from '../components/Badges.jsx';
import ErrorState from '../../components/ui/ErrorState.jsx';
import PageLoader from '../../components/ui/PageLoader.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import { useToast } from '../hooks/useToast.js';
import { adminService } from '../adminService.js';
import { formatCurrency, formatUnit } from '../../utils/format.js';

function Row({ label, children }) {
  return (
    <div className="grid grid-cols-3 gap-3 py-2.5 text-sm">
      <dt className="text-slate-500">{label}</dt>
      <dd className="col-span-2 min-w-0 break-words text-navy-900">{children}</dd>
    </div>
  );
}

const fmtDate = (iso) => (iso ? new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '—');

/** /admin/products/:id – read-only view with quick visibility switches. */
export default function ProductViewPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const { data: loaded, loading, error, retry } = useAsync(() => adminService.getProduct(id), [id]);
  const [override, setOverride] = useState(null);
  const [busy, setBusy] = useState(false);
  const [confirm, setConfirm] = useState(false);
  const p = override?.id === loaded?.id ? override : loaded;

  async function quick(changes, message) {
    setBusy(true);
    try {
      setOverride(await adminService.quickUpdateProduct(id, changes));
      toast.success(message);
    } catch (err) {
      toast.error(err.message);
    } finally {
      setBusy(false);
    }
  }

  async function remove() {
    setBusy(true);
    try {
      await adminService.deleteProduct(id);
      toast.success(`“${p.name}” was deleted.`);
      navigate('/admin/products', { replace: true });
    } catch (err) {
      toast.error(err.message);
      setBusy(false);
      setConfirm(false);
    }
  }

  const crumbs = [{ label: 'Products', to: '/admin/products' }, { label: p?.name ?? 'Product' }];
  if (error) {
    return (
      <>
        <AdminPageHeader title="Product" crumbs={crumbs} />
        <ErrorState message={error.status === 404 ? 'This product no longer exists.' : error.message} onRetry={error.status === 404 ? undefined : retry} />
      </>
    );
  }
  if (loading || !p) return <PageLoader />;

  return (
    <>
      <AdminPageHeader
        title={p.name}
        crumbs={crumbs}
        actions={
          <>
            {p.publiclyVisible && (
              <a href={`/products/${p.slug}`} target="_blank" rel="noopener" className="admin-btn-secondary">
                <ExternalLink className="size-4" aria-hidden="true" /> On website
              </a>
            )}
            <Link to={`/admin/products/${p.id}/edit`} className="btn-primary">
              <Pencil className="size-4" aria-hidden="true" /> Edit
            </Link>
            <button type="button" className="admin-btn-danger" onClick={() => setConfirm(true)}>
              <Trash2 className="size-4" aria-hidden="true" /> Delete
            </button>
          </>
        }
      />
      <div className="grid gap-6 lg:grid-cols-3">
        <section className="admin-card p-5 lg:col-span-2" aria-label="Details">
          <div className="mb-3 flex flex-wrap gap-2">
            <VisibilityBadge {...p} />
            <StockBadge status={p.stockStatus} quantity={p.stockQuantity} />
            <DemoBadge show={p.isDemo} />
          </div>
          <dl className="divide-y divide-slate-100">
            <Row label="Product code">{p.sku}</Row>
            <Row label="Web address">/products/{p.slug}</Row>
            <Row label="Category">{p.category.name}</Row>
            <Row label="Brand">{p.brand.name}</Row>
            <Row label="Pack">{p.packSize} · {formatUnit(p.unit)}</Row>
            <Row label="MRP / Price">
              {formatCurrency(p.mrp)} / <strong>{formatCurrency(p.price)}</strong>
            </Row>
            <Row label="Stock">{p.stockQuantity} (“few left” at {p.reorderLevel})</Row>
            <Row label="Description">{p.description || <span className="text-slate-400">No description</span>}</Row>
            <Row label="Added">{fmtDate(p.createdAt)}</Row>
            <Row label="Last changed">{fmtDate(p.updatedAt)}</Row>
          </dl>
        </section>
        <div className="space-y-6">
          <section className="admin-card space-y-1 p-3" aria-label="Quick changes">
            <SwitchField label="Show on website" checked={p.visibleOnWebsite} disabled={busy}
              onChange={(v) => quick({ visibleOnWebsite: v }, v ? 'Now shown on the website.' : 'Hidden from the website.')} />
            <SwitchField label="Active" description="Off = discontinued" checked={p.active} disabled={busy}
              onChange={(v) => quick({ active: v }, v ? 'Product is active again.' : 'Product marked inactive.')} />
            <SwitchField label="Featured on homepage" checked={p.featured} disabled={busy}
              onChange={(v) => quick({ featured: v }, v ? 'Added to featured products.' : 'Removed from featured products.')} />
          </section>
          <section className="admin-card p-5" aria-labelledby="photos">
            <h2 id="photos" className="mb-3 font-bold">Photos</h2>
            {p.images.length ? (
              <ul className="grid grid-cols-3 gap-2">
                {p.images.map((img) => (
                  <li key={img.url}>
                    <AdminImage src={img.url} alt={img.alt} className="aspect-square w-full rounded-lg" />
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-sm text-slate-500">No photos – the website shows an illustration.</p>
            )}
          </section>
        </div>
      </div>
      {confirm && (
        <ConfirmDialog
          title="Delete product?"
          message={<p><strong>{p.name}</strong> and its photos will be deleted permanently. This cannot be undone.</p>}
          busy={busy}
          onConfirm={remove}
          onCancel={() => setConfirm(false)}
        />
      )}
    </>
  );
}
