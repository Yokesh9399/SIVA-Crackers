import { useEffect, useState } from 'react';
import { Download, FileText, Printer, Info, Search } from 'lucide-react';
import AdminPageHeader from '../components/AdminPageHeader.jsx';
import DateRange from '../components/DateRange.jsx';
import { StockBadge } from '../components/Badges.jsx';
import ErrorState from '../../components/ui/ErrorState.jsx';
import Skeleton from '../../components/ui/Skeleton.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import { adminService, reportCsvUrl, reportPdfUrl } from '../adminService.js';
import { daysAgoIst, fmtChange, fmtDateTime, fmtMoney, todayIst, plural } from '../format.js';

const TABS = [
  ['summary', 'Summary'],
  ['billing', 'Billing'],
  ['product-sales', 'Product sales'],
  ['stock-movements', 'Stock movements'],
  ['low-stock', 'Low stock'],
  ['suppliers', 'Suppliers'],
  ['expenses', 'Expenses'],
];

function Card({ label, value }) {
  return (
    <div className="admin-card p-4">
      <p className="text-xl font-extrabold break-words">{value}</p>
      <p className="text-xs text-slate-500">{label}</p>
    </div>
  );
}

function Table({ head, rows, empty = 'No data for this period.' }) {
  if (!rows.length) return <p className="admin-card p-6 text-center text-sm text-slate-500">{empty}</p>;
  return (
    <div className="admin-card overflow-x-auto">
      <table className="w-full min-w-[36rem] text-left text-sm">
        <thead className="border-b border-slate-100 text-xs tracking-wide text-slate-500 uppercase">
          <tr>{head.map((h, i) => <th key={h} scope="col" className={`px-4 py-3 ${i > 0 ? 'text-right' : ''}`}>{h}</th>)}</tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {rows.map((r, i) => (
            <tr key={i} className={r.total ? 'bg-slate-50 font-semibold' : ''}>
              {r.cells.map((c, j) => <td key={j} className={`px-4 py-2.5 ${j > 0 ? 'text-right whitespace-nowrap' : ''}`}>{c}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function Summary({ range, groupBy }) {
  const r = useAsync(() => adminService.report('summary', { ...range, groupBy }), [range, groupBy]);
  if (r.error) return <ErrorState message={r.error.message} onRetry={r.retry} />;
  if (!r.data) return <Skeleton className="h-48" />;
  const t = r.data.totals;
  return (
    <>
      <div className="mb-4 grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Card label={`Revenue (${plural(t.bills, 'finalized bill')})`} value={fmtMoney(t.revenue)} />
        <Card label="Discounts given" value={fmtMoney(t.discounts)} />
        <Card label="Units received" value={t.stockInUnits} />
        <Card label="Units out" value={t.stockOutUnits} />
        <Card label="Adjustments (net units)" value={fmtChange(t.adjustmentUnits || 0).replace('−0', '0')} />
        <Card label="Purchase value recorded" value={fmtMoney(t.purchaseValue)} />
        <Card label={`Expenses (${t.expenseCount})`} value={fmtMoney(t.expenseTotal)} />
      </div>
      <Table
        head={['Period', 'Bills', 'Revenue', 'In', 'Out', 'Adjust', 'Purchases', 'Expenses']}
        rows={[...r.data.rows, ...(r.data.rows.length ? [{ ...t, total: true }] : [])].map((x) => ({
          total: x.total,
          cells: [x.period, x.bills, fmtMoney(x.revenue), x.stockInUnits, x.stockOutUnits, x.adjustmentUnits, fmtMoney(x.purchaseValue), fmtMoney(x.expenseTotal)],
        }))}
      />
    </>
  );
}

function Billing({ range }) {
  const r = useAsync(() => adminService.report('billing', range), [range]);
  if (r.error) return <ErrorState message={r.error.message} onRetry={r.retry} />;
  if (!r.data) return <Skeleton className="h-48" />;
  const d = r.data;
  return (
    <>
      <div className="mb-4 grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Card label={`Revenue (${plural(d.finalizedBills, 'finalized bill')})`} value={fmtMoney(d.revenue)} />
        <Card label="Discounts given" value={fmtMoney(d.discounts)} />
        <Card label={`Payment pending (${plural(d.pendingBills, 'bill')})`} value={fmtMoney(d.pendingTotal)} />
        <Card label={`Voided (${plural(d.voidedBills, 'bill')}, not counted)`} value={fmtMoney(d.voidedTotal)} />
      </div>
      <Table
        head={['Payment method', 'Bills', 'Total']}
        rows={[
          ...d.byPaymentMethod.map((m) => ({ cells: [m.label, m.bills, fmtMoney(m.total)] })),
          ...(d.byPaymentMethod.length ? [{ total: true, cells: ['Total', d.finalizedBills, fmtMoney(d.revenue)] }] : []),
        ]}
        empty="No finalized bills in this period."
      />
    </>
  );
}

function ProductSales({ range, search, sort }) {
  const r = useAsync(() => adminService.report('product-sales', { ...range, search, sort }), [range, search, sort]);
  if (r.error) return <ErrorState message={r.error.message} onRetry={r.retry} />;
  if (!r.data) return <Skeleton className="h-48" />;
  return (
    <>
      <p className="mb-2 text-xs text-slate-500">Sales per product at the listed price, before bill-level discounts (the Billing tab shows revenue after discounts).</p>
    <Table
      head={['Product', 'Bills', 'Units sold', 'Sales before discount']}
      rows={r.data.map((p) => ({ cells: [`${p.productName} (${p.productSku ?? '—'})`, p.bills, p.unitsSold, fmtMoney(p.revenue)] }))}
      empty="No products sold in this period."
    />
    </>
  );
}

function Movements({ range }) {
  const r = useAsync(() => adminService.report('stock-movements', { ...range, size: 100 }), [range]);
  if (r.error) return <ErrorState message={r.error.message} onRetry={r.retry} />;
  if (!r.data) return <Skeleton className="h-48" />;
  return (
    <>
      {r.data.totalElements > 100 && <p className="mb-2 text-sm text-slate-600">Showing the latest 100 of {r.data.totalElements}. Download the CSV for all.</p>}
      <Table
        head={['Product', 'When', 'Reason', 'Change', 'After', 'By']}
        rows={r.data.content.map((m) => ({ cells: [`${m.productName} (${m.productSku})`, fmtDateTime(m.createdAt), m.reasonLabel, fmtChange(m.quantityChange), m.newQuantity, m.performedByName ?? '—'] }))}
      />
    </>
  );
}

function LowStock() {
  const r = useAsync(() => adminService.report('low-stock', {}), []);
  if (r.error) return <ErrorState message={r.error.message} onRetry={r.retry} />;
  if (!r.data) return <Skeleton className="h-48" />;
  const rows = [...r.data.outOfStock, ...r.data.lowStock];
  return (
    <>
      <div className="mb-4 grid grid-cols-2 gap-3"><Card label="Out of stock" value={r.data.outOfStockCount} /><Card label="Few left" value={r.data.lowStockCount} /></div>
      <Table
        head={['Product', 'Status', 'In stock', 'Alert at']}
        rows={rows.map((p) => ({ cells: [`${p.name} (${p.sku})`, <StockBadge key="s" status={p.stockStatus} />, p.stockQuantity, p.reorderLevel] }))}
        empty="No active product is low or out of stock."
      />
    </>
  );
}

function Suppliers({ range }) {
  const r = useAsync(() => adminService.report('suppliers', range), [range]);
  if (r.error) return <ErrorState message={r.error.message} onRetry={r.retry} />;
  if (!r.data) return <Skeleton className="h-48" />;
  return (
    <Table
      head={['Supplier', 'Deliveries', 'Units', 'Purchase value', 'Last delivery']}
      rows={r.data.map((s) => ({ cells: [`${s.name}${s.active ? '' : ' (inactive)'}`, s.deliveries, s.unitsReceived, fmtMoney(s.purchaseValue), fmtDateTime(s.lastDeliveryAt)] }))}
      empty="No suppliers yet."
    />
  );
}

function Expenses({ range }) {
  const r = useAsync(() => adminService.report('expenses', range), [range]);
  if (r.error) return <ErrorState message={r.error.message} onRetry={r.retry} />;
  if (!r.data) return <Skeleton className="h-48" />;
  return (
    <>
      <div className="mb-4 grid grid-cols-2 gap-3"><Card label="Active expenses" value={r.data.count} /><Card label="Total" value={fmtMoney(r.data.total)} /></div>
      <Table
        head={['Category', 'Count', 'Total']}
        rows={[...r.data.byCategory.map((c) => ({ cells: [c.category, c.count, fmtMoney(c.total)] })), { total: true, cells: ['Total', r.data.count, fmtMoney(r.data.total)] }]}
      />
    </>
  );
}

/** /admin/reports – OWNER and MANAGER. Built only from recorded stock, supplier and expense data. */
export default function ReportsPage() {
  const [tab, setTab] = useState('summary');
  const [range, setRange] = useState({ from: daysAgoIst(29), to: todayIst() });
  const [groupBy, setGroupBy] = useState('DAY');
  const [salesSearchText, setSalesSearchText] = useState('');
  const [salesSearch, setSalesSearch] = useState('');
  const [salesSort, setSalesSort] = useState('revenue');
  useEffect(() => {
    const t = setTimeout(() => setSalesSearch(salesSearchText.trim()), 350);
    return () => clearTimeout(t);
  }, [salesSearchText]);
  const usesDates = tab !== 'low-stock';
  const exportParams =
    tab === 'summary' ? { ...range, groupBy }
      : tab === 'product-sales' ? { ...range, search: salesSearch, sort: salesSort }
        : usesDates ? range : {};

  return (
    <>
      <AdminPageHeader
        title="Reports"
        description="Sales from in-store bills, stock, suppliers and expenses for any period (Indian dates)."
        crumbs={[{ label: 'Reports' }]}
        actions={
          <>
            <button type="button" className="admin-btn-secondary print:hidden" onClick={() => window.print()}><Printer className="size-4" aria-hidden="true" /> Print</button>
            <a className="admin-btn-secondary print:hidden" href={reportPdfUrl(tab, exportParams)} download><FileText className="size-4" aria-hidden="true" /> Download PDF</a>
            <a className="btn-primary print:hidden" href={reportCsvUrl(tab, exportParams)} download><Download className="size-4" aria-hidden="true" /> Download CSV</a>
          </>
        }
      />
      <p className="mb-4 flex gap-2 rounded-xl bg-amber-50 p-3 text-xs text-amber-900 ring-1 ring-amber-200 print:hidden">
        <Info className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
        Revenue comes only from finalized in-store bills (voided bills are excluded). Expenses are shown separately. No profit is shown: product costs are not recorded for every sale, so a profit figure would be a guess. “Purchase value” is only the cost staff entered when receiving stock.
      </p>
      <div role="tablist" aria-label="Report" className="no-scrollbar mb-4 flex gap-1 overflow-x-auto print:hidden">
        {TABS.map(([id, label]) => (
          <button key={id} type="button" role="tab" aria-selected={tab === id} onClick={() => setTab(id)}
            className={`shrink-0 rounded-full px-4 py-2 text-sm font-semibold ${tab === id ? 'bg-navy-900 text-white' : 'bg-white text-navy-900 ring-1 ring-slate-200 hover:bg-slate-50'}`}>
            {label}
          </button>
        ))}
      </div>
      {usesDates && (
        <section aria-label="Period" className="admin-card mb-4 grid grid-cols-1 gap-3 p-4 md:grid-cols-3 print:hidden">
          <div className="md:col-span-2"><DateRange from={range.from} to={range.to} onChange={(r) => r.from && r.to && setRange(r)} idPrefix="report" /></div>
          {tab === 'product-sales' && (
            <div className="grid grid-cols-2 gap-2 md:col-span-3 lg:col-span-1">
              <label className="relative block text-xs font-medium text-slate-600">Product
                <Search className="pointer-events-none absolute bottom-3 left-3 size-4 text-slate-400" aria-hidden="true" />
                <input type="search" className="admin-input mt-1 pl-9" placeholder="Name or code…" value={salesSearchText} maxLength={100} onChange={(e) => setSalesSearchText(e.target.value)} />
              </label>
              <label className="block text-xs font-medium text-slate-600">Sort by
                <select className="admin-input mt-1" value={salesSort} onChange={(e) => setSalesSort(e.target.value)}>
                  <option value="revenue">Revenue</option>
                  <option value="units">Units sold</option>
                </select>
              </label>
            </div>
          )}
          {tab === 'summary' && (
            <label className="block text-xs font-medium text-slate-600">Group by
              <select className="admin-input mt-1" value={groupBy} onChange={(e) => setGroupBy(e.target.value)}>
                <option value="DAY">Day</option>
                <option value="MONTH">Month</option>
                <option value="YEAR">Year</option>
              </select>
            </label>
          )}
        </section>
      )}
      <p className="mb-3 hidden text-sm print:block">{TABS.find(([id]) => id === tab)[1]} report{usesDates ? ` · ${range.from} to ${range.to}` : ''}</p>
      <div role="tabpanel">
        {tab === 'summary' && <Summary range={range} groupBy={groupBy} />}
        {tab === 'billing' && <Billing range={range} />}
        {tab === 'product-sales' && <ProductSales range={range} search={salesSearch} sort={salesSort} />}
        {tab === 'stock-movements' && <Movements range={range} />}
        {tab === 'low-stock' && <LowStock />}
        {tab === 'suppliers' && <Suppliers range={range} />}
        {tab === 'expenses' && <Expenses range={range} />}
      </div>
    </>
  );
}
