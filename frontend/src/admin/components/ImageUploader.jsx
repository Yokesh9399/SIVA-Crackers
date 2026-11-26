import { useEffect, useId, useRef, useState } from 'react';
import { ImagePlus, Loader2, RefreshCw, Trash2, ArrowLeft, ArrowRight } from 'lucide-react';
import { adminService, checkImageFile } from '../adminService.js';

const ACCEPT = 'image/jpeg,image/png,image/webp';

/** Shows a stored image and falls back to a neutral placeholder if the file is missing. */
export function AdminImage({ src, alt = '', className = '' }) {
  const [failed, setFailed] = useState(false);
  if (!src || failed) {
    return (
      <span className={`grid place-items-center bg-slate-100 text-slate-400 ${className}`} aria-label={alt || 'No image'} role="img">
        <ImagePlus className="size-1/3 max-h-8 max-w-8" aria-hidden="true" />
      </span>
    );
  }
  return <img src={src} alt={alt} loading="lazy" onError={() => setFailed(true)} className={`object-cover ${className}`} />;
}

/** Picks a file, checks it, uploads it right away and returns the stored URL. */
function useUpload(folder, onUploaded) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [preview, setPreview] = useState(null);

  useEffect(() => () => preview && URL.revokeObjectURL(preview), [preview]);

  async function upload(file) {
    setError('');
    const problem = checkImageFile(file);
    if (problem) {
      setError(problem);
      return;
    }
    const local = URL.createObjectURL(file);
    setPreview(local);
    setBusy(true);
    try {
      onUploaded(await adminService.uploadImage(folder, file));
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
      setPreview(null);
    }
  }
  return { busy, error, preview, upload };
}

/** One image (brand logo, category picture) with preview, replace and remove. */
export default function ImageUploader({ label, folder, value, onChange, hint = 'JPEG, PNG or WebP, up to 2 MB.' }) {
  const inputId = useId();
  const input = useRef(null);
  const { busy, error, preview, upload } = useUpload(folder, onChange);
  const shown = preview ?? value;

  return (
    <div>
      <p className="mb-1 text-sm font-medium text-navy-900">
        {label} <span className="font-normal text-slate-400">(optional)</span>
      </p>
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative size-24 overflow-hidden rounded-xl ring-1 ring-slate-200">
          <AdminImage src={shown} alt={label} className="size-full" />
          {busy && (
            <span className="absolute inset-0 grid place-items-center bg-white/70">
              <Loader2 className="size-6 animate-spin text-orange-500" aria-label="Uploading" />
            </span>
          )}
        </div>
        <div className="flex flex-wrap gap-2">
          <input
            ref={input}
            id={inputId}
            type="file"
            accept={ACCEPT}
            className="sr-only"
            onChange={(e) => {
              upload(e.target.files?.[0]);
              e.target.value = '';
            }}
          />
          <button type="button" className="admin-btn-secondary" disabled={busy} onClick={() => input.current?.click()}>
            {value ? <RefreshCw className="size-4" aria-hidden="true" /> : <ImagePlus className="size-4" aria-hidden="true" />}
            {value ? 'Replace' : 'Upload'}
          </button>
          {value && !busy && (
            <button type="button" className="admin-btn-secondary" onClick={() => onChange(null)}>
              <Trash2 className="size-4" aria-hidden="true" /> Remove
            </button>
          )}
        </div>
      </div>
      {error ? <p className="mt-1 text-xs font-medium text-red-600" role="alert">{error}</p> : <p className="mt-1 text-xs text-slate-500">{hint}</p>}
    </div>
  );
}

/** Product photos: up to `max` images, each with a description (alt text); the first is the main photo. */
export function ProductImagesField({ images, onChange, max = 6, error: fieldError }) {
  const input = useRef(null);
  const { busy, error, preview, upload } = useUpload('products', (url) => onChange([...images, { url, alt: '' }]));

  const move = (from, to) => {
    const next = [...images];
    const [item] = next.splice(from, 1);
    next.splice(to, 0, item);
    onChange(next);
  };

  return (
    <div>
      <p className="mb-1 text-sm font-medium text-navy-900">
        Product photos <span className="font-normal text-slate-400">(optional, up to {max})</span>
      </p>
      <p className="mb-3 text-xs text-slate-500">
        The first photo is the main one. JPEG, PNG or WebP up to 2 MB. Location and camera details are removed automatically.
        Without photos the website shows an illustration.
      </p>
      <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        {images.map((img, i) => (
          <li key={img.url} className="flex min-w-0 gap-3 rounded-xl p-2 ring-1 ring-slate-200">
            <AdminImage src={img.url} alt={img.alt || `Photo ${i + 1}`} className="size-20 shrink-0 rounded-lg" />
            <div className="min-w-0 flex-1 space-y-2">
              <label className="block text-xs font-medium text-slate-600">
                Description {i === 0 && <span className="text-orange-600">· main photo</span>}
                <input
                  className="admin-input mt-1 py-1.5"
                  value={img.alt}
                  maxLength={200}
                  placeholder="e.g. Box front"
                  onChange={(e) => onChange(images.map((x, j) => (j === i ? { ...x, alt: e.target.value } : x)))}
                />
              </label>
              <div className="flex gap-1">
                <button type="button" className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 disabled:opacity-30" disabled={i === 0} onClick={() => move(i, i - 1)} aria-label={`Move photo ${i + 1} earlier`}>
                  <ArrowLeft className="size-4" aria-hidden="true" />
                </button>
                <button type="button" className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 disabled:opacity-30" disabled={i === images.length - 1} onClick={() => move(i, i + 1)} aria-label={`Move photo ${i + 1} later`}>
                  <ArrowRight className="size-4" aria-hidden="true" />
                </button>
                <button type="button" className="ml-auto rounded-lg p-2 text-red-600 hover:bg-red-50" onClick={() => onChange(images.filter((_, j) => j !== i))} aria-label={`Remove photo ${i + 1}`}>
                  <Trash2 className="size-4" aria-hidden="true" />
                </button>
              </div>
            </div>
          </li>
        ))}
        {busy && (
          <li className="flex items-center gap-3 rounded-xl p-2 ring-1 ring-slate-200">
            <span className="relative size-20 overflow-hidden rounded-lg">
              {preview && <img src={preview} alt="" className="size-full object-cover opacity-60" />}
              <Loader2 className="absolute inset-0 m-auto size-6 animate-spin text-orange-500" aria-hidden="true" />
            </span>
            <span className="text-sm text-slate-600" role="status">Uploading…</span>
          </li>
        )}
      </ul>
      {images.length < max && (
        <>
          <input
            ref={input}
            type="file"
            accept={ACCEPT}
            className="sr-only"
            aria-label="Choose a product photo"
            onChange={(e) => {
              upload(e.target.files?.[0]);
              e.target.value = '';
            }}
          />
          <button type="button" className="admin-btn-secondary mt-3" disabled={busy} onClick={() => input.current?.click()}>
            <ImagePlus className="size-4" aria-hidden="true" /> Add photo
          </button>
        </>
      )}
      {(error || fieldError) && (
        <p className="mt-1 text-xs font-medium text-red-600" role="alert">
          {error || fieldError}
        </p>
      )}
    </div>
  );
}
