import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { FileText, Plus, Printer, Search } from 'lucide-react';
import AdminPageHeader from '../components/AdminPageHeader.jsx';
import DateRange from '../components/DateRange.jsx';
import Modal from '../components/Modal.jsx';
import { Badge } from '../components/Badges.jsx';
import { TextAreaField, SelectField } from '../components/Fields.jsx';
import { FormMessage } from '../../components/auth/FormField.jsx';
import Pagination from '../../components/ui/Pagination.jsx';
import ErrorState from '../../components/ui/ErrorState.jsx';
import EmptyState from '../../components/ui/EmptyState.jsx';
import Skeleton from '../../components/ui/Skeleton.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import { useAuth } from '../../hooks/useAuth.js';
import { useToast } from '../hooks/useToast.js';
import { adminService, invoiceUrl } from '../adminService.js';
import { canVoid } from '../roles.js';
import { fmtDateTime, fmtMoney } from '../format.js';
import { formErrorMessage, serverFieldErrors } from '../../utils/authValidation.js';

export function BillStatusBadges({ status, paymentStatus }) {
  if (status === 'VOIDED') return <Badge color="red">Voided</Badge>;
  return paymentStatus === 'PAID' ? <Badge color="green">Paid</Badge> : <Badge color="amber">Payment pending</Badge>;
}

/** Bill details with print / PDF, void (owner and manager) and "mark as paid" for pending bills. */
function BillDetail({ billId, methods, onClose, onChanged }) {
  const toast = useToast();
  const { user } = useAuth();
  const [reload, setReload] = useState(0);
  const bill = useAsync(() => adminService.getBill(billId), [billId, reload]);
  const [mode, setMode] = useState(null); // 'void' | 'pay'
  const [reason, setReason] = useState('');
  const [payMethod, setPayMethod] = useState('');
  const [received, setReceived] = useState('');
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);
  const b = bill.data;
  const activeMethods = methods.filter((m) => m.active);

  async function act(e) {
    e.preventDefault();
    if (busy) return;
    if (mode === 'void' && !reason.trim()) return setErrors({ reason: 'Please give a reason for voiding this bill.' });
    setBusy(true);
    setErrors({});
    setFormError('');
    try {
      if (mode === 'void') {
        await adminService.voidBill(billId, reason.trim());
        toast.success(`Bill ${b.billNumber} was voided and its stock was returned.`);
      } else {
        await adminService.markBillPaid(billId, { paymentMethod: payMethod || activeMethods[0]?.code, amountReceived: received || null });
        toast.success(`Bill ${b.billNumber} is now paid.`);
      }
      setMode(null);
      setReload((n) => n + 1);
      onChanged();
    } catch (err) {
      setErrors(serverFieldErrors(err, { reason: 'Reason', amountReceived: 'Amount received', paymentMethod: 'Payment method' }));
      setFormError(formErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal title={b ? `Bill ${b.billNumber}` : 'Bill'} onClose={onClose} size="lg" closeDisabled={busy}>
      {bill.error ? (
        <ErrorState message={bill.error.message} onRetry={bill.retry} />
      ) : !b ? (
        <Skeleton className="h-48" />
      ) : (
        <div className="space-y-4 text-sm">
          <div className="flex flex-wrap items-center gap-2">
            <BillStatusBadges status={b.status} paymentStatus={b.paymentStatus} />
            <span className="text-slate-500">{fmtDateTime(b.createdAt)} · by {b.createdByName ?? '—'}</span>
          </div>
          <p>
            Customer: <strong>{b.customerName ?? 'Walk-in customer'}</strong>
            {b.customerPhone && <span className="text-slate-500"> · {b.customerPhone}</span>}
          </p>
          {b.status === 'VOIDED' && (
            <p className="rounded-lg bg-red-50 p-3 text-red-700">
              Voided {fmtDateTime(b.voidedAt)} by {b.voidedByName ?? '—'}: {b.voidReason}
            </p>
          )}
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead className="border-b border-slate-100 text-xs text-slate-500 uppercase">
                <tr>
                  <th scope="col" className="py-2 pr-2">Product</th>
                  <th scope="col" className="py-2 pr-2 text-right">Price</th>
                  <th scope="col" className="py-2 pr-2 text-right">Qty</th>
                  <th scope="col" className="py-2 text-right">Amount</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {b.items.map((i) => (
                  <tr key={i.lineNo}>
                    <td className="py-2 pr-2">{i.productName}<span className="block text-xs text-slate-400">{i.productSku}{i.packSize ? ` · ${i.packSize}` : ''}</span></td>
                    <td className="py-2 pr-2 text-right whitespace-nowrap">{fmtMoney(i.unitPrice)}</td>
                    <td className="py-2 pr-2 text-right">{i.quantity}</td>
                    <td className="py-2 text-right whitespace-nowrap">{fmtMoney(i.lineTotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <dl className="ml-auto max-w-xs space-y-1">
            <div className="flex justify-between"><dt>Subtotal</dt><dd>{fmtMoney(b.subtotal)}</dd></div>
            <div className="flex justify-between"><dt>Discount</dt><dd>− {fmtMoney(b.discountAmount)}</dd></div>
            <div className="flex justify-between border-t border-slate-200 pt-1 text-base font-bold"><dt>Total</dt><dd>{fmtMoney(b.total)}</dd></div>
            <div className="flex justify-between text-slate-600"><dt>Payment</dt><dd>{b.paymentMethod.label}</dd></div>
            {b.amountReceived != null && <div className="flex justify-between text-slate-600"><dt>Received</dt><dd>{fmtMoney(b.amountReceived)}</dd></div>}
            {b.changeDue != null && <div className="flex justify-between text-slate-600"><dt>Change</dt><dd>{fmtMoney(b.changeDue)}</dd></div>}
          </dl>
          {b.notes && <p className="text-slate-600">Notes: {b.notes}</p>}

          <div className="flex flex-wrap gap-2 border-t border-slate-100 pt-4">
            <a className="admin-btn-secondary" href={invoiceUrl(b.id)} target="_blank" rel="noopener noreferrer"><Printer className="size-4" aria-hidden="true" /> Print invoice</a>
            <a className="admin-btn-secondary" href={invoiceUrl(b.id, true)}><FileText className="size-4" aria-hidden="true" /> Download PDF</a>
            {b.status === 'FINALIZED' && b.paymentStatus === 'PENDING' && !mode && (
              <button type="button" className="admin-btn-secondary" onClick={() => setMode('pay')}>Mark as paid</button>
            )}
            {b.status === 'FINALIZED' && canVoid(user?.role) && !mode && (
              <button type="button" className="admin-btn-secondary text-red-700" onClick={() => setMode('void')}>Void bill</button>
            )}
          </div>

          {mode && (
            <form onSubmit={act} noValidate className="space-y-3 rounded-lg bg-slate-50 p-4">
              <FormMessage>{formError}</FormMessage>
              {mode === 'void' ? (
                <>
                  <p>Voiding cancels this bill and puts its {b.items.reduce((n, i) => n + i.quantity, 0)} items back into stock. The bill stays in the history marked “Voided”. This cannot be undone.</p>
                  <TextAreaField label="Reason" rows={2} value={reason} onChange={(e) => setReason(e.target.value)} error={errors.reason} maxLength={300} data-autofocus />
                </>
              ) : (
                <div className="grid gap-3 sm:grid-cols-2">
                  <SelectField label="Paid with" value={payMethod || activeMethods[0]?.code || ''} onChange={(e) => setPayMethod(e.target.value)} error={errors.paymentMethod}>
                    {activeMethods.map((m) => <option key={m.code} value={m.code}>{m.label}</option>)}
                  </SelectField>
                  <label className="block text-sm font-medium text-navy-900">
                    Amount received <span className="font-normal text-slate-400">(optional)</span>
                    <input className="admin-input mt-1" inputMode="decimal" value={received} onChange={(e) => setReceived(e.target.value)} />
                    {errors.amountReceived && <span className="mt-1 block text-xs font-medium text-red-600">{errors.amountReceived}</span>}
                  </label>
                </div>
              )}
              <div className="flex justify-end gap-2">
                <button type="button" className="admin-btn-secondary" onClick={() => { setMode(null); setErrors({}); setFormError(''); }} disabled={busy}>Cancel</button>
                <button type="submit" className={mode === 'void' ? 'btn-primary bg-red-600 hover:bg-red-700' : 'btn-primary'} disabled={busy}>
                  {busy ? 'Saving…' : mode === 'void' ? 'Void bill' : 'Mark as paid'}
                </button>
              </div>
            </form>
          )}
        </div>
      )}
    </Modal>
  );
}

/** /admin/bills – every in-store bill with filters. Totals count finalized bills only. */
export default function BillsPage() {
  const [params, setParams] = useSearchParams();
  const filters = {
    from: params.get('from') ?? '',
    to: params.get('to') ?? '',
    search: params.get('search') ?? '',
    paymentMethod: params.get('paymentMethod') ?? '',
    paymentStatus: params.get('paymentStatus') ?? '',
    status: params.get('status') ?? '',
    page: Math.max(1, Number(params.get('page')) || 1),
    size: '25',
  };
  const [searchText, setSearchText] = useState(filters.search);
  const [open, setOpen] = useState(null);
  const [reload, setReload] = useState(0);
  const methods = useAsync(() => adminService.paymentMethods(), []);

  const update = (changes) => {
    const next = new URLSearchParams(params);
    for (const [k, v] of Object.entries(changes)) {
      if (v === '' || v == null) next.delete(k);
      else next.set(k, String(v));
    }
    if (!('page' in changes)) next.delete('page');
    setParams(next, { replace: 'search' in changes });
  };
  useEffect(() => {
    if (searchText === filters.search) return undefined;
    const t = setTimeout(() => update({ search: searchText.trim() }), 350);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchText]);

  const list = useAsync(() => adminService.listBills({ ...filters, page: filters.page - 1 }), [params.toString(), reload]);
  const d = list.data;

  return (
    <>
      <AdminPageHeader
        title="Bill history"
        description="Every in-store bill. Voided bills stay here for the record but never count as sales."
        crumbs={[{ label: 'Billing / POS', to: '/admin/billing' }, { label: 'Bill history' }]}
        actions={<Link to="/admin/billing" className="btn-primary"><Plus className="size-4" aria-hidden="true" /> New bill</Link>}
      />
      <section aria-label="Filters" className="admin-card mb-4 grid grid-cols-1 gap-3 p-4 md:grid-cols-2 lg:grid-cols-6">
        <div className="md:col-span-2">
          <DateRange from={filters.from} to={filters.to} onChange={(r) => update(r)} idPrefix="bills" />
        </div>
        <label className="relative block text-xs font-medium text-slate-600 md:col-span-2 lg:col-span-1">
          Search
          <Search className="pointer-events-none absolute bottom-3 left-3 size-4 text-slate-400" aria-hidden="true" />
          <input type="search" className="admin-input mt-1 pl-9" placeholder="Bill no., name, mobile…" value={searchText} maxLength={100} onChange={(e) => setSearchText(e.target.value)} />
        </label>
        <label className="block text-xs font-medium text-slate-600">
          Payment method
          <select className="admin-input mt-1" value={filters.paymentMethod} onChange={(e) => update({ paymentMethod: e.target.value })}>
            <option value="">All methods</option>
            {(methods.data ?? []).map((m) => <option key={m.code} value={m.code}>{m.label}</option>)}
          </select>
        </label>
        <label className="block text-xs font-medium text-slate-600">
          Payment
          <select className="admin-input mt-1" value={filters.paymentStatus} onChange={(e) => update({ paymentStatus: e.target.value })}>
            <option value="">Paid and pending</option>
            <option value="PAID">Paid</option>
            <option value="PENDING">Pending</option>
          </select>
        </label>
        <label className="block text-xs font-medium text-slate-600">
          Bill status
          <select className="admin-input mt-1" value={filters.status} onChange={(e) => update({ status: e.target.value })}>
            <option value="">All bills</option>
            <option value="FINALIZED">Finalized</option>
            <option value="VOIDED">Voided</option>
          </select>
        </label>
      </section>

      {list.error ? (
        <ErrorState message={list.error.message} onRetry={list.retry} />
      ) : !d ? (
        <div className="space-y-2">{Array.from({ length: 6 }, (_, i) => <Skeleton key={i} className="h-14" />)}</div>
      ) : (
        <>
          <dl className="mb-4 grid grid-cols-2 gap-3 lg:grid-cols-4">
            <div className="admin-card p-4"><dt className="text-xs text-slate-500">Sales (finalized bills)</dt><dd className="text-xl font-bold">{fmtMoney(d.finalizedTotal)}</dd></div>
            <div className="admin-card p-4"><dt className="text-xs text-slate-500">Finalized bills</dt><dd className="text-xl font-bold">{d.finalizedCount}</dd></div>
            <div className="admin-card p-4"><dt className="text-xs text-slate-500">Payment pending</dt><dd className="text-xl font-bold">{fmtMoney(d.pendingTotal)}</dd></div>
            <div className="admin-card p-4"><dt className="text-xs text-slate-500">Voided bills</dt><dd className="text-xl font-bold">{d.voidedCount}</dd></div>
          </dl>
          {d.page.totalElements === 0 ? (
            <EmptyState title="No bills found" message="Bills appear here once they are made on the Billing / POS page." />
          ) : (
            <>
              <p className="mb-2 text-sm text-slate-600" aria-live="polite">{d.page.totalElements} bills</p>
              <div className="admin-card hidden overflow-x-auto md:block">
                <table className="w-full text-left text-sm">
                  <thead className="border-b border-slate-100 text-xs tracking-wide text-slate-500 uppercase">
                    <tr>
                      <th scope="col" className="px-4 py-3">Bill</th>
                      <th scope="col" className="px-4 py-3">When</th>
                      <th scope="col" className="px-4 py-3">Customer</th>
                      <th scope="col" className="px-4 py-3 text-right">Total</th>
                      <th scope="col" className="px-4 py-3">Payment</th>
                      <th scope="col" className="px-4 py-3">By</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {d.page.content.map((b) => (
                      <tr key={b.id}>
                        <td className="px-4 py-3">
                          <button type="button" className="font-medium text-orange-600 hover:underline" onClick={() => setOpen(b.id)}>{b.billNumber}</button>
                          <span className="block text-xs text-slate-400">{b.itemCount} items</span>
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap text-slate-600">{fmtDateTime(b.createdAt)}</td>
                        <td className="px-4 py-3">{b.customerName ?? <span className="text-slate-400">Walk-in</span>}<span className="block text-xs text-slate-400">{b.customerPhone}</span></td>
                        <td className={`px-4 py-3 text-right font-semibold whitespace-nowrap ${b.status === 'VOIDED' ? 'text-slate-400 line-through' : ''}`}>{fmtMoney(b.total)}</td>
                        <td className="px-4 py-3"><BillStatusBadges status={b.status} paymentStatus={b.paymentStatus} /><span className="block text-xs text-slate-500">{b.paymentMethod}</span></td>
                        <td className="px-4 py-3 text-xs text-slate-600">{b.createdByName ?? '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <ul className="space-y-2 md:hidden">
                {d.page.content.map((b) => (
                  <li key={b.id}>
                    <button type="button" className="admin-card w-full p-3 text-left text-sm" onClick={() => setOpen(b.id)}>
                      <span className="flex items-start justify-between gap-2">
                        <span className="font-medium text-orange-600">{b.billNumber}</span>
                        <BillStatusBadges status={b.status} paymentStatus={b.paymentStatus} />
                      </span>
                      <span className="mt-1 block">{b.customerName ?? 'Walk-in'} · <strong className={b.status === 'VOIDED' ? 'line-through' : ''}>{fmtMoney(b.total)}</strong> · {b.paymentMethod}</span>
                      <span className="mt-1 block text-xs text-slate-400">{fmtDateTime(b.createdAt)} · {b.createdByName ?? '—'}</span>
                    </button>
                  </li>
                ))}
              </ul>
              <div className="mt-6">
                <Pagination page={filters.page} totalPages={d.page.totalPages} onChange={(page) => update({ page })} />
              </div>
            </>
          )}
        </>
      )}
      {open && <BillDetail billId={open} methods={methods.data ?? []} onClose={() => setOpen(null)} onChanged={() => setReload((n) => n + 1)} />}
    </>
  );
}
