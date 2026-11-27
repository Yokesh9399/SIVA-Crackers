import { useState } from 'react';
import { Plus, Pencil, Ban, Search, Tags } from 'lucide-react';
import AdminPageHeader from '../components/AdminPageHeader.jsx';
import Modal from '../components/Modal.jsx';
import DateRange from '../components/DateRange.jsx';
import { SelectField, SwitchField, TextAreaField } from '../components/Fields.jsx';
import { Badge } from '../components/Badges.jsx';
import FormField, { FormMessage } from '../../components/auth/FormField.jsx';
import Pagination from '../../components/ui/Pagination.jsx';
import ErrorState from '../../components/ui/ErrorState.jsx';
import EmptyState from '../../components/ui/EmptyState.jsx';
import Skeleton from '../../components/ui/Skeleton.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import { useToast } from '../hooks/useToast.js';
import { adminService } from '../adminService.js';
import { daysAgoIst, fmtDate, fmtDateTime, fmtMoney, todayIst, plural } from '../format.js';
import { formErrorMessage, serverFieldErrors } from '../../utils/authValidation.js';

const LABELS = { categoryId: 'Category', expenseDate: 'Date', amount: 'Amount', description: 'Description', paidTo: 'Paid to', reference: 'Bill / receipt no.', notes: 'Notes', reason: 'Reason', name: 'Name' };
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

function ExpenseForm({ expense, categories, onClose, onSaved }) {
  const toast = useToast();
  const [form, setForm] = useState({
    categoryId: String(expense?.category.id ?? categories.find((c) => c.active)?.id ?? ''),
    expenseDate: expense?.expenseDate ?? todayIst(), amount: expense ? String(expense.amount) : '',
    description: expense?.description ?? '', paidTo: expense?.paidTo ?? '', reference: expense?.reference ?? '', notes: expense?.notes ?? '',
  });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  async function submit(e) {
    e.preventDefault();
    if (saving) return;
    const p = {};
    if (!form.categoryId) p.categoryId = 'Choose a category.';
    if (!form.expenseDate) p.expenseDate = 'Choose the date.';
    else if (form.expenseDate > todayIst()) p.expenseDate = 'The date cannot be in the future.';
    if (!/^\d{1,10}(\.\d{1,2})?$/.test(form.amount) || Number(form.amount) <= 0) p.amount = 'Enter an amount above 0 (at most 2 decimal places).';
    if (!form.description.trim()) p.description = 'Describe the expense.';
    setErrors(p);
    setFormError('');
    if (Object.keys(p).length) return;
    setSaving(true);
    const body = { ...Object.fromEntries(Object.entries(form).map(([k, v]) => [k, v.trim()])), categoryId: Number(form.categoryId) };
    try {
      if (expense) await adminService.updateExpense(expense.id, body);
      else await adminService.createExpense(body);
      toast.success(expense ? 'Expense saved.' : 'Expense recorded.');
      onSaved();
    } catch (err) {
      setErrors(serverFieldErrors(err, LABELS));
      setFormError(formErrorMessage(err));
      setSaving(false);
    }
  }

  return (
    <Modal
      title={expense ? 'Edit expense' : 'Record expense'}
      onClose={onClose}
      closeDisabled={saving}
      footer={
        <>
          <button type="button" className="admin-btn-secondary" onClick={onClose} disabled={saving}>Cancel</button>
          <button type="submit" form="expense-form" className="btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
        </>
      }
    >
      <form id="expense-form" onSubmit={submit} noValidate className="space-y-4">
        <FormMessage>{formError}</FormMessage>
        <div className="grid gap-4 sm:grid-cols-2">
          <SelectField label="Category" value={form.categoryId} onChange={set('categoryId')} error={errors.categoryId}>
            <option value="">Choose…</option>
            {categories.filter((c) => c.active || String(c.id) === form.categoryId).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </SelectField>
          <label className="block">
            <span className="mb-1 block text-sm font-medium text-navy-900">Date</span>
            <input type="date" className={`admin-input ${errors.expenseDate ? 'border-red-400' : ''}`} max={todayIst()} value={form.expenseDate} onChange={set('expenseDate')} aria-invalid={errors.expenseDate ? 'true' : undefined} />
            {errors.expenseDate && <span className="mt-1 block text-xs font-medium text-red-600">{errors.expenseDate}</span>}
          </label>
        </div>
        <FormField label="Amount (₹)" inputMode="decimal" value={form.amount} onChange={set('amount')} error={errors.amount} maxLength={13} />
        <FormField label="Description" value={form.description} onChange={set('description')} error={errors.description} maxLength={200} placeholder="e.g. Electricity bill for October" />
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label="Paid to" optional value={form.paidTo} onChange={set('paidTo')} error={errors.paidTo} maxLength={120} />
          <FormField label="Bill / receipt no." optional value={form.reference} onChange={set('reference')} error={errors.reference} maxLength={60} />
        </div>
        <TextAreaField label="Notes" optional rows={2} value={form.notes} onChange={set('notes')} error={errors.notes} maxLength={500} />
      </form>
    </Modal>
  );
}

function CancelForm({ expense, onClose, onSaved }) {
  const toast = useToast();
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  async function submit(e) {
    e.preventDefault();
    if (saving) return;
    if (!reason.trim()) {
      setError('Please give a reason – it is kept in the activity log.');
      return;
    }
    setSaving(true);
    try {
      await adminService.cancelExpense(expense.id, reason.trim());
      toast.success('Expense cancelled. It is kept for the record but no longer counted.');
      onSaved();
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  }
  return (
    <Modal
      title="Cancel expense?"
      size="sm"
      onClose={onClose}
      closeDisabled={saving}
      footer={
        <>
          <button type="button" className="admin-btn-secondary" onClick={onClose} disabled={saving} data-autofocus>Keep it</button>
          <button type="submit" form="cancel-form" className="admin-btn-danger" disabled={saving}>{saving ? 'Please wait…' : 'Cancel expense'}</button>
        </>
      }
    >
      <form id="cancel-form" onSubmit={submit} noValidate className="space-y-3">
        <p className="text-sm text-slate-700">
          {fmtDate(expense.expenseDate)} · {expense.category.name} · <strong>{fmtMoney(expense.amount)}</strong> – {expense.description}
        </p>
        <p className="text-xs text-slate-500">Cancelled expenses are never deleted: they stay in the list (marked cancelled) and are no longer counted in totals or reports.</p>
        <TextAreaField label="Reason" rows={2} value={reason} onChange={(e) => setReason(e.target.value)} error={error} maxLength={300} />
      </form>
    </Modal>
  );
}

function CategoriesDialog({ categories, onClose, onSaved }) {
  const toast = useToast();
  const [name, setName] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  async function add(e) {
    e.preventDefault();
    if (!name.trim()) {
      setError('Enter a name.');
      return;
    }
    setBusy(true);
    try {
      await adminService.createExpenseCategory({ name: name.trim(), displayOrder: categories.length + 1, active: true });
      toast.success(`Category “${name.trim()}” added.`);
      setName('');
      setError('');
      onSaved();
    } catch (err) {
      setError(serverFieldErrors(err, LABELS).name ?? err.message);
    } finally {
      setBusy(false);
    }
  }
  async function toggle(c) {
    setBusy(true);
    try {
      await adminService.updateExpenseCategory(c.id, { name: c.name, displayOrder: c.displayOrder, active: !c.active });
      onSaved();
    } catch (err) {
      toast.error(err.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal title="Expense categories" onClose={onClose} footer={<button type="button" className="admin-btn-secondary" onClick={onClose}>Done</button>}>
      <ul className="divide-y divide-slate-100">
        {categories.map((c) => (
          <li key={c.id}>
            <SwitchField label={c.name} description={c.active ? 'Offered for new expenses' : 'Hidden for new expenses (kept on old ones)'} checked={c.active} disabled={busy} onChange={() => toggle(c)} />
          </li>
        ))}
      </ul>
      <form onSubmit={add} noValidate className="mt-4 flex flex-wrap items-end gap-2">
        <div className="min-w-0 flex-1">
          <FormField label="New category" value={name} onChange={(e) => setName(e.target.value)} error={error} maxLength={60} />
        </div>
        <button type="submit" className="btn-primary" disabled={busy}><Plus className="size-4" aria-hidden="true" /> Add</button>
      </form>
    </Modal>
  );
}

/** /admin/expenses – OWNER and MANAGER. */
export default function ExpensesPage() {
  const [filters, setFilters] = useState({ from: daysAgoIst(29), to: todayIst(), categoryId: '', status: '', search: '', sort: 'date,desc' });
  const [page, setPage] = useState(1);
  const [year, setYear] = useState(Number(todayIst().slice(0, 4)));
  const [reload, setReload] = useState(0);
  const [editing, setEditing] = useState(null);
  const [cancelling, setCancelling] = useState(null);
  const [managing, setManaging] = useState(false);
  const categories = useAsync(() => adminService.expenseCategories(), [reload]);
  const list = useAsync(() => adminService.listExpenses({ ...filters, page: page - 1, size: 25 }), [filters, page, reload]);
  const monthly = useAsync(() => adminService.monthlyExpenses(year), [year, reload]);
  const change = (c) => {
    setFilters((f) => ({ ...f, ...c }));
    setPage(1);
  };
  const saved = () => {
    setEditing(null);
    setCancelling(null);
    setReload((n) => n + 1);
  };
  const maxMonth = Math.max(1, ...(monthly.data?.months ?? []).map((m) => Number(m.total)));

  return (
    <>
      <AdminPageHeader
        title="Expenses"
        description="Shop running costs. Each entry records who added it; changes and cancellations are kept in the activity log."
        crumbs={[{ label: 'Expenses' }]}
        actions={
          <>
            <button type="button" className="admin-btn-secondary" onClick={() => setManaging(true)} disabled={!categories.data}><Tags className="size-4" aria-hidden="true" /> Categories</button>
            <button type="button" className="btn-primary" onClick={() => setEditing('new')} disabled={!categories.data}><Plus className="size-4" aria-hidden="true" /> Record expense</button>
          </>
        }
      />
      <section aria-label="Filters" className="admin-card mb-4 grid grid-cols-1 gap-3 p-4 md:grid-cols-2 xl:grid-cols-4">
        <div className="md:col-span-2"><DateRange from={filters.from} to={filters.to} onChange={change} idPrefix="expenses" /></div>
        <label className="block text-xs font-medium text-slate-600">Category
          <select className="admin-input mt-1" value={filters.categoryId} onChange={(e) => change({ categoryId: e.target.value })}>
            <option value="">All categories</option>
            {categories.data?.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </label>
        <label className="block text-xs font-medium text-slate-600">Status
          <select className="admin-input mt-1" value={filters.status} onChange={(e) => change({ status: e.target.value })}>
            <option value="">Active and cancelled</option>
            <option value="ACTIVE">Active only</option>
            <option value="CANCELLED">Cancelled only</option>
          </select>
        </label>
        <label className="relative block text-xs font-medium text-slate-600 md:col-span-1 xl:col-span-2">Search
          <Search className="pointer-events-none absolute bottom-3 left-3 size-4 text-slate-400" aria-hidden="true" />
          <input type="search" className="admin-input mt-1 pl-9" placeholder="Description, paid to or bill no.…" value={filters.search} maxLength={100} onChange={(e) => change({ search: e.target.value })} />
        </label>
        <label className="block text-xs font-medium text-slate-600">Sort
          <select className="admin-input mt-1" value={filters.sort} onChange={(e) => change({ sort: e.target.value })}>
            <option value="date,desc">Newest date first</option>
            <option value="date,asc">Oldest date first</option>
            <option value="amount,desc">Largest amount first</option>
            <option value="amount,asc">Smallest amount first</option>
          </select>
        </label>
      </section>

      {list.data && (
        <div className="admin-card mb-4 flex flex-wrap items-center gap-x-6 gap-y-1 p-4">
          <p><span className="text-2xl font-extrabold">{fmtMoney(list.data.activeTotal)}</span> <span className="text-sm text-slate-500">total of {plural(list.data.activeCount, 'active expense')} in this view</span></p>
          <p className="text-xs text-slate-500">Cancelled expenses are not counted.</p>
        </div>
      )}

      {list.error ? (
        <ErrorState message={list.error.message} onRetry={list.retry} />
      ) : !list.data ? (
        <div className="space-y-2">{Array.from({ length: 5 }, (_, i) => <Skeleton key={i} className="h-14" />)}</div>
      ) : list.data.page.totalElements === 0 ? (
        <EmptyState title="No expenses in this view" message="Change the dates or filters, or record an expense." />
      ) : (
        <>
          <ul className="space-y-2">
            {list.data.page.content.map((e) => {
              const cancelled = e.status === 'CANCELLED';
              return (
                <li key={e.id} className={`admin-card flex flex-wrap items-start gap-3 p-3 ${cancelled ? 'opacity-70' : ''}`}>
                  <div className="min-w-0 flex-1 basis-56">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className={`font-medium break-words ${cancelled ? 'line-through' : ''}`}>{e.description}</p>
                      <Badge color="blue">{e.category.name}</Badge>
                      {cancelled && <Badge color="red">Cancelled</Badge>}
                    </div>
                    <p className="text-xs text-slate-500">
                      {fmtDate(e.expenseDate)}{e.paidTo ? ` · ${e.paidTo}` : ''}{e.reference ? ` · Bill ${e.reference}` : ''} · by {e.recordedByName ?? '—'}
                    </p>
                    {cancelled && <p className="text-xs text-red-700">Cancelled {fmtDateTime(e.cancelledAt)}: {e.cancelReason}</p>}
                  </div>
                  <p className="text-lg font-bold whitespace-nowrap">{fmtMoney(e.amount)}</p>
                  {!cancelled && (
                    <div className="flex gap-1">
                      <button type="button" className="grid size-10 place-items-center rounded-lg text-slate-500 hover:bg-slate-100" onClick={() => setEditing(e)} aria-label={`Edit expense ${e.description}`}><Pencil className="size-4" aria-hidden="true" /></button>
                      <button type="button" className="grid size-10 place-items-center rounded-lg text-slate-500 hover:bg-red-50 hover:text-red-600" onClick={() => setCancelling(e)} aria-label={`Cancel expense ${e.description}`}><Ban className="size-4" aria-hidden="true" /></button>
                    </div>
                  )}
                </li>
              );
            })}
          </ul>
          <div className="mt-6"><Pagination page={page} totalPages={list.data.page.totalPages} onChange={setPage} /></div>
        </>
      )}

      <section className="admin-card mt-6 p-5" aria-labelledby="monthly-title">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 id="monthly-title" className="text-lg font-bold">Monthly summary</h2>
          <label className="text-sm">Year{' '}
            <select className="admin-input inline-block w-auto" value={year} onChange={(e) => setYear(Number(e.target.value))}>
              {Array.from({ length: 6 }, (_, i) => Number(todayIst().slice(0, 4)) - i).map((y) => <option key={y} value={y}>{y}</option>)}
            </select>
          </label>
        </div>
        {monthly.data ? (
          <>
            <ul className="mt-4 grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3">
              {monthly.data.months.map((m) => (
                <li key={m.month} className="text-sm">
                  <div className="flex justify-between"><span>{MONTHS[m.month - 1]}</span><span className="font-semibold">{fmtMoney(m.total)}</span></div>
                  <div className="mt-1 h-2 overflow-hidden rounded-full bg-slate-100" aria-hidden="true">
                    <div className="h-full rounded-full bg-gradient-to-r from-gold-400 to-orange-500" style={{ width: `${(Number(m.total) / maxMonth) * 100}%` }} />
                  </div>
                </li>
              ))}
            </ul>
            <p className="mt-3 text-sm">Year total: <strong>{fmtMoney(monthly.data.yearTotal)}</strong></p>
          </>
        ) : monthly.error ? <ErrorState message={monthly.error.message} onRetry={monthly.retry} /> : <Skeleton className="mt-4 h-32" />}
      </section>

      {editing && categories.data && <ExpenseForm expense={editing === 'new' ? null : editing} categories={categories.data} onClose={() => setEditing(null)} onSaved={saved} />}
      {cancelling && <CancelForm expense={cancelling} onClose={() => setCancelling(null)} onSaved={saved} />}
      {managing && categories.data && <CategoriesDialog categories={categories.data} onClose={() => setManaging(false)} onSaved={() => setReload((n) => n + 1)} />}
    </>
  );
}
