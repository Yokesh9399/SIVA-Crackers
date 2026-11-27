import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Minus, Plus, Search, Trash2, UserPlus, X, FileText, Printer, ReceiptIndianRupee } from 'lucide-react';
import AdminPageHeader from '../components/AdminPageHeader.jsx';
import { StockBadge } from '../components/Badges.jsx';
import { TextAreaField } from '../components/Fields.jsx';
import { FormMessage } from '../../components/auth/FormField.jsx';
import ErrorState from '../../components/ui/ErrorState.jsx';
import Skeleton from '../../components/ui/Skeleton.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import { useToast } from '../hooks/useToast.js';
import { adminService, invoiceUrl } from '../adminService.js';
import { fmtMoney, plural } from '../format.js';
import { discountPaise, newRequestKey, subtotalPaise, toPaise, toRupees } from '../billing.js';
import { CustomerForm } from './CustomersPage.jsx';
import { formErrorMessage } from '../../utils/authValidation.js';

const rupees = (paise) => fmtMoney(paise / 100);

/** Product search for the counter: active products only, with price and stock. */
function ProductSearch({ cart, onAdd }) {
  const [text, setText] = useState('');
  const [query, setQuery] = useState('');
  useEffect(() => {
    const t = setTimeout(() => setQuery(text.trim()), 300);
    return () => clearTimeout(t);
  }, [text]);
  const list = useAsync(() => adminService.listStock({ search: query, size: 25, sort: 'name,asc' }), [query]);
  const rows = (list.data?.content ?? []).filter((p) => p.active);

  return (
    <section aria-labelledby="pos-products" className="admin-card p-4">
      <h2 id="pos-products" className="mb-3 font-semibold">Add products</h2>
      <label className="relative block">
        <span className="sr-only">Search products by name or code</span>
        <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" aria-hidden="true" />
        <input type="search" className="admin-input pl-9" placeholder="Search name or code…" value={text} maxLength={100} onChange={(e) => setText(e.target.value)} autoFocus />
      </label>
      <div className="mt-3 max-h-[28rem] overflow-y-auto">
        {list.error ? (
          <ErrorState message={list.error.message} onRetry={list.retry} />
        ) : !list.data ? (
          <div className="space-y-2">{Array.from({ length: 4 }, (_, i) => <Skeleton key={i} className="h-14" />)}</div>
        ) : rows.length === 0 ? (
          <p className="py-6 text-center text-sm text-slate-500">No active products match.</p>
        ) : (
          <ul className="divide-y divide-slate-100">
            {rows.map((p) => {
              const inCart = cart.find((l) => l.productId === p.productId)?.quantity ?? 0;
              const left = p.stockQuantity - inCart;
              return (
                <li key={p.productId} className="flex items-center gap-3 py-2">
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium break-words">{p.name}</p>
                    <p className="text-xs text-slate-500">
                      {p.sku}{p.packSize ? ` · ${p.packSize}` : ''} · <strong className="text-navy-900">{fmtMoney(p.price)}</strong>
                    </p>
                    <StockBadge status={p.stockStatus} quantity={p.stockQuantity} />
                  </div>
                  <button
                    type="button"
                    className="admin-btn-secondary shrink-0"
                    disabled={left <= 0}
                    onClick={() => onAdd(p)}
                    aria-label={`Add ${p.name} to the bill`}
                  >
                    <Plus className="size-4" aria-hidden="true" /> Add
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </section>
  );
}

/** Pick an existing customer, add a new one, or leave as a walk-in customer. */
function CustomerPicker({ customer, onChange }) {
  const [text, setText] = useState('');
  const [query, setQuery] = useState('');
  const [adding, setAdding] = useState(false);
  useEffect(() => {
    const t = setTimeout(() => setQuery(text.trim()), 300);
    return () => clearTimeout(t);
  }, [text]);
  const list = useAsync(() => (query ? adminService.listCustomers(query) : Promise.resolve([])), [query]);

  if (customer) {
    return (
      <div className="flex items-center justify-between gap-2 rounded-lg bg-slate-50 p-3">
        <div className="min-w-0 text-sm">
          <p className="font-medium break-words">{customer.fullName}</p>
          <p className="text-xs text-slate-500">{[customer.phone, customer.email].filter(Boolean).join(' · ')}</p>
        </div>
        <button type="button" className="grid size-10 shrink-0 place-items-center rounded-lg text-slate-500 hover:bg-slate-200" onClick={() => onChange(null)} aria-label="Remove customer from the bill">
          <X className="size-4" aria-hidden="true" />
        </button>
      </div>
    );
  }
  return (
    <div>
      <div className="flex gap-2">
        <label className="relative block flex-1">
          <span className="sr-only">Find a customer by name or mobile</span>
          <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" aria-hidden="true" />
          <input type="search" className="admin-input pl-9" placeholder="Name or mobile (optional)" value={text} maxLength={100} onChange={(e) => setText(e.target.value)} />
        </label>
        <button type="button" className="admin-btn-secondary shrink-0" onClick={() => setAdding(true)}>
          <UserPlus className="size-4" aria-hidden="true" /> New
        </button>
      </div>
      {query && list.data && (
        <ul className="mt-2 divide-y divide-slate-100 rounded-lg ring-1 ring-slate-200">
          {list.data.length === 0 ? (
            <li className="p-3 text-sm text-slate-500">No customer found. Use “New” to add one.</li>
          ) : (
            list.data.slice(0, 6).map((c) => (
              <li key={c.id}>
                <button type="button" className="w-full p-3 text-left text-sm hover:bg-slate-50" onClick={() => { onChange(c); setText(''); }}>
                  <span className="font-medium">{c.fullName}</span>
                  <span className="block text-xs text-slate-500">{[c.phone, c.email].filter(Boolean).join(' · ')}</span>
                </button>
              </li>
            ))
          )}
        </ul>
      )}
      <p className="mt-1 text-xs text-slate-500">Leave empty for a walk-in customer.</p>
      {adding && <CustomerForm onClose={() => setAdding(false)} onSaved={(c) => { setAdding(false); onChange(c); setText(''); }} />}
    </div>
  );
}

function SavedBill({ bill, onNew }) {
  return (
    <section className="admin-card mx-auto max-w-lg p-6 text-center" aria-live="polite">
      <ReceiptIndianRupee className="mx-auto size-10 text-emerald-600" aria-hidden="true" />
      <h2 className="mt-2 text-xl font-bold">Bill {bill.billNumber} saved</h2>
      <p className="mt-1 text-sm text-slate-600">
        Total <strong>{fmtMoney(bill.total)}</strong> · {bill.paymentMethod.label} · {bill.paymentStatus === 'PAID' ? 'Paid' : 'Payment pending'}
        {bill.changeDue != null && Number(bill.changeDue) > 0 && <> · Change to return <strong>{fmtMoney(bill.changeDue)}</strong></>}
      </p>
      {bill.replayed && <p className="mt-2 text-xs text-amber-700">This bill had already been saved – it was not billed twice.</p>}
      <div className="mt-5 flex flex-wrap justify-center gap-2">
        <a className="admin-btn-secondary" href={invoiceUrl(bill.id)} target="_blank" rel="noopener noreferrer"><Printer className="size-4" aria-hidden="true" /> Print invoice</a>
        <a className="admin-btn-secondary" href={invoiceUrl(bill.id, true)}><FileText className="size-4" aria-hidden="true" /> Download PDF</a>
        <button type="button" className="btn-primary" onClick={onNew} autoFocus><Plus className="size-4" aria-hidden="true" /> New bill</button>
      </div>
      <Link to="/admin/bills" className="mt-4 inline-block text-sm font-medium text-orange-600 hover:underline">Bill history</Link>
    </section>
  );
}

const EMPTY = { discountType: '', discountValue: '', paymentStatus: 'PAID', amountReceived: '', notes: '' };

/** /admin/billing – the in-store counter. Prices are the products' current prices; the server checks everything again. */
export default function BillingPage() {
  const toast = useToast();
  const methods = useAsync(() => adminService.paymentMethods(), []);
  const active = useMemo(() => (methods.data ?? []).filter((m) => m.active), [methods.data]);
  const [cart, setCart] = useState([]);
  const [customer, setCustomer] = useState(null);
  const [form, setForm] = useState(EMPTY);
  const [method, setMethod] = useState('');
  const [requestKey, setRequestKey] = useState(newRequestKey);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [saved, setSaved] = useState(null);
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const paymentMethod = method || active[0]?.code || '';
  const subtotal = subtotalPaise(cart);
  const discount = discountPaise(form.discountType, form.discountValue, subtotal);
  const total = subtotal - (discount.paise ?? 0);
  const received = form.amountReceived === '' ? null : toPaise(form.amountReceived);
  const receivedError =
    form.amountReceived !== '' && !/^\d{1,10}(\.\d{1,2})?$/.test(form.amountReceived) ? 'Enter an amount with at most 2 decimal places.'
      : form.paymentStatus === 'PAID' && received != null && received < total ? `Less than the total (${rupees(total)}).` : '';

  // Any change to the bill gets a new key, so only a true retry of the same bill reuses the old one.
  const changeCart = (fn) => {
    setCart(fn);
    setRequestKey(newRequestKey());
    setError('');
  };
  const add = (p) =>
    changeCart((c) => {
      const line = c.find((l) => l.productId === p.productId);
      if (line) return c.map((l) => (l === line ? { ...l, quantity: Math.min(l.stock, l.quantity + 1) } : l));
      return [...c, { productId: p.productId, name: p.name, sku: p.sku, price: p.price, stock: p.stockQuantity, quantity: 1 }];
    });
  const setQty = (id, q) =>
    changeCart((c) => c.map((l) => (l.productId === id ? { ...l, quantity: Math.max(1, Math.min(l.stock, Number.isFinite(q) ? q : 1)) } : l)));
  const remove = (id) => changeCart((c) => c.filter((l) => l.productId !== id));

  function reset() {
    setCart([]);
    setCustomer(null);
    setForm(EMPTY);
    setRequestKey(newRequestKey());
    setSaved(null);
    setError('');
  }

  async function submit(e) {
    e.preventDefault();
    if (saving) return;
    if (!cart.length) return setError('Add at least one product.');
    if (discount.error || receivedError) return setError('Please correct the highlighted fields.');
    if (!paymentMethod) return setError('Choose a payment method.');
    setSaving(true);
    setError('');
    try {
      const bill = await adminService.createBill({
        requestKey,
        customerId: customer?.id ?? null,
        items: cart.map((l) => ({ productId: l.productId, quantity: l.quantity })),
        discountType: form.discountType || null,
        discountValue: form.discountType && form.discountValue !== '' ? form.discountValue : null,
        paymentMethod,
        paymentStatus: form.paymentStatus,
        amountReceived: form.amountReceived === '' ? null : form.amountReceived,
        notes: form.notes.trim() || null,
      });
      setSaved(bill);
      toast.success(`Bill ${bill.billNumber} saved.`);
    } catch (err) {
      // 409 = stock changed meanwhile; show the server's message (e.g. "only 2 of X in stock").
      const fields = (err.fieldErrors ?? []).map((f) => (f.field === 'items' ? f.message : `${f.field} ${f.message}`));
      setError(fields.length ? `${fields.join('. ')}.` : formErrorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  const header = (
    <AdminPageHeader
      title="Billing / POS"
      description="Bills for customers buying in person at the shop. Payment is taken at the counter – nothing is paid online."
      crumbs={[{ label: 'Billing / POS' }]}
      actions={<Link to="/admin/bills" className="admin-btn-secondary">Bill history</Link>}
    />
  );

  if (saved) return <>{header}<SavedBill bill={saved} onNew={reset} /></>;

  return (
    <>
      {header}
      <div className={`grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.1fr)] ${cart.length ? 'pb-20 lg:pb-0' : ''}`}>
        <ProductSearch cart={cart} onAdd={add} />
        <form onSubmit={submit} noValidate className="admin-card space-y-4 p-4" aria-labelledby="pos-bill">
          <h2 id="pos-bill" className="font-semibold">Current bill</h2>
          {cart.length === 0 ? (
            <p className="rounded-lg bg-slate-50 p-6 text-center text-sm text-slate-500">No products yet. Search and add products on the left.</p>
          ) : (
            <ul className="divide-y divide-slate-100" aria-label="Products on this bill">
              {cart.map((l) => (
                <li key={l.productId} className="flex flex-wrap items-center gap-2 py-2">
                  <div className="min-w-0 flex-1 basis-40">
                    <p className="text-sm font-medium break-words">{l.name}</p>
                    <p className="text-xs text-slate-500">{fmtMoney(l.price)} each · {l.stock} in stock</p>
                  </div>
                  <div className="flex items-center gap-1">
                    <button type="button" className="grid size-9 place-items-center rounded-lg ring-1 ring-slate-200 disabled:opacity-40" onClick={() => setQty(l.productId, l.quantity - 1)} disabled={l.quantity <= 1} aria-label={`One less ${l.name}`}>
                      <Minus className="size-4" aria-hidden="true" />
                    </button>
                    <input
                      type="number" inputMode="numeric" min={1} max={l.stock} className="admin-input w-16 px-2 text-center"
                      value={l.quantity} onChange={(e) => setQty(l.productId, parseInt(e.target.value, 10))} aria-label={`Quantity of ${l.name}`}
                    />
                    <button type="button" className="grid size-9 place-items-center rounded-lg ring-1 ring-slate-200 disabled:opacity-40" onClick={() => setQty(l.productId, l.quantity + 1)} disabled={l.quantity >= l.stock} aria-label={`One more ${l.name}`}>
                      <Plus className="size-4" aria-hidden="true" />
                    </button>
                  </div>
                  <p className="w-24 text-right text-sm font-semibold">{rupees(toPaise(l.price) * l.quantity)}</p>
                  <button type="button" className="grid size-9 place-items-center rounded-lg text-slate-500 hover:bg-red-50 hover:text-red-600" onClick={() => remove(l.productId)} aria-label={`Remove ${l.name}`}>
                    <Trash2 className="size-4" aria-hidden="true" />
                  </button>
                </li>
              ))}
            </ul>
          )}

          <div>
            <p className="mb-1 text-sm font-medium text-navy-900">Customer</p>
            <CustomerPicker customer={customer} onChange={(c) => { setCustomer(c); setRequestKey(newRequestKey()); }} />
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <label className="block text-sm font-medium text-navy-900">
              Discount
              <select className="admin-input mt-1" value={form.discountType} onChange={(e) => { setForm((f) => ({ ...f, discountType: e.target.value, discountValue: '' })); setRequestKey(newRequestKey()); }}>
                <option value="">No discount</option>
                <option value="AMOUNT">Amount (₹)</option>
                <option value="PERCENT">Percent (%)</option>
              </select>
            </label>
            {form.discountType && (
              <label className="block text-sm font-medium text-navy-900">
                {form.discountType === 'PERCENT' ? 'Discount %' : 'Discount ₹'}
                <input
                  className={`admin-input mt-1 ${discount.error ? 'border-red-400' : ''}`} inputMode="decimal" value={form.discountValue}
                  onChange={(e) => { set('discountValue')(e); setRequestKey(newRequestKey()); }} aria-invalid={discount.error ? 'true' : undefined} aria-describedby="discount-hint"
                />
                <span id="discount-hint" className={`mt-1 block text-xs ${discount.error ? 'font-medium text-red-600' : 'font-normal text-slate-500'}`}>
                  {discount.error || 'At most 20% of the subtotal.'}
                </span>
              </label>
            )}
          </div>

          <dl className="space-y-1 rounded-lg bg-slate-50 p-3 text-sm">
            <div className="flex justify-between"><dt>Subtotal</dt><dd>{rupees(subtotal)}</dd></div>
            <div className="flex justify-between"><dt>Discount</dt><dd>− {rupees(discount.paise ?? 0)}</dd></div>
            <div className="flex justify-between border-t border-slate-200 pt-1 text-lg font-bold"><dt>Total</dt><dd data-testid="pos-total">{rupees(total)}</dd></div>
          </dl>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <label className="block text-sm font-medium text-navy-900">
              Payment method
              {methods.error ? (
                <span className="mt-1 block text-xs text-red-600">{methods.error.message}</span>
              ) : (
                <select className="admin-input mt-1" value={paymentMethod} onChange={(e) => { setMethod(e.target.value); setRequestKey(newRequestKey()); }}>
                  {active.map((m) => <option key={m.code} value={m.code}>{m.label}</option>)}
                </select>
              )}
            </label>
            <label className="block text-sm font-medium text-navy-900">
              Payment status
              <select className="admin-input mt-1" value={form.paymentStatus} onChange={(e) => { set('paymentStatus')(e); setRequestKey(newRequestKey()); }}>
                <option value="PAID">Paid now</option>
                <option value="PENDING">Pending (pay later at the shop)</option>
              </select>
            </label>
            {form.paymentStatus === 'PAID' && (
              <label className="block text-sm font-medium text-navy-900">
                Amount received <span className="font-normal text-slate-400">(optional)</span>
                <input
                  className={`admin-input mt-1 ${receivedError ? 'border-red-400' : ''}`} inputMode="decimal" value={form.amountReceived}
                  onChange={(e) => { set('amountReceived')(e); setRequestKey(newRequestKey()); }} aria-invalid={receivedError ? 'true' : undefined} aria-describedby="received-hint"
                />
                <span id="received-hint" className={`mt-1 block text-xs ${receivedError ? 'font-medium text-red-600' : 'font-normal text-slate-500'}`}>
                  {receivedError || (received != null && received >= total ? `Change to return: ₹${toRupees(received - total)}` : 'For cash: to work out the change.')}
                </span>
              </label>
            )}
          </div>
          <TextAreaField label="Notes" optional rows={2} value={form.notes} onChange={set('notes')} maxLength={300} />

          <FormMessage>{error}</FormMessage>
          <div className="flex flex-wrap justify-end gap-2">
            <button type="button" className="admin-btn-secondary" onClick={reset} disabled={saving || (!cart.length && !customer)}>Clear</button>
            <button type="submit" className="btn-primary" disabled={saving || !cart.length || !paymentMethod}>
              {saving ? 'Saving…' : `Save bill · ${rupees(total)}`}
            </button>
          </div>
        </form>
      </div>
      {cart.length > 0 && (
        // Phones: the bill is below the product list, so keep its total in view.
        <div className="fixed inset-x-0 bottom-0 z-40 border-t border-slate-200 bg-white/95 p-3 shadow-lg backdrop-blur lg:hidden">
          <button
            type="button"
            className="btn-primary w-full justify-between"
            onClick={() => document.getElementById('pos-bill')?.scrollIntoView({ behavior: 'smooth', block: 'start' })}
          >
            <span>{plural(cart.reduce((n, l) => n + l.quantity, 0), 'item')} · {rupees(total)}</span>
            <span>View bill</span>
          </button>
        </div>
      )}
    </>
  );
}
