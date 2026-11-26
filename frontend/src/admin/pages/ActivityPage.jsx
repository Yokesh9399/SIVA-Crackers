import { useState } from 'react';
import AdminPageHeader from '../components/AdminPageHeader.jsx';
import Pagination from '../../components/ui/Pagination.jsx';
import ErrorState from '../../components/ui/ErrorState.jsx';
import EmptyState from '../../components/ui/EmptyState.jsx';
import Skeleton from '../../components/ui/Skeleton.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import { adminService } from '../adminService.js';
import { fmtDateTime } from '../format.js';

const AREAS = [
  ['', 'Everything'], ['STAFF', 'Staff'], ['PRODUCT', 'Products'], ['CATEGORY', 'Categories'], ['BRAND', 'Brands'],
  ['SUPPLIER', 'Suppliers'], ['EXPENSE', 'Expenses'], ['EXPENSE_CATEGORY', 'Expense categories'], ['CUSTOMER', 'Customers'],
];

const ACTION_TEXT = (a) => a.toLowerCase().replace(/_/g, ' ').replace(/^\w/, (c) => c.toUpperCase());

/** /admin/activity – OWNER only: who changed what (never passwords). */
export default function ActivityPage() {
  const [area, setArea] = useState('');
  const [page, setPage] = useState(1);
  const log = useAsync(() => adminService.auditLog({ entityType: area, page: page - 1, size: 50 }), [area, page]);

  return (
    <>
      <AdminPageHeader
        title="Activity log"
        description="Sensitive changes with the staff member who made them. Stock changes are listed in the stock history."
        crumbs={[{ label: 'Activity log' }]}
      />
      <label className="mb-4 block max-w-xs text-sm font-medium">Area
        <select className="admin-input mt-1" value={area} onChange={(e) => { setArea(e.target.value); setPage(1); }}>
          {AREAS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
        </select>
      </label>
      {log.error ? (
        <ErrorState message={log.error.message} onRetry={log.retry} />
      ) : !log.data ? (
        <div className="space-y-2">{Array.from({ length: 6 }, (_, i) => <Skeleton key={i} className="h-12" />)}</div>
      ) : log.data.totalElements === 0 ? (
        <EmptyState title="Nothing recorded yet" message="Changes to staff, suppliers, expenses, customers and deletions appear here." />
      ) : (
        <>
          <ol className="admin-card divide-y divide-slate-100">
            {log.data.content.map((e) => (
              <li key={e.id} className="flex flex-col gap-0.5 p-3 text-sm sm:flex-row sm:gap-4">
                <span className="shrink-0 text-xs text-slate-500 sm:w-44">{fmtDateTime(e.occurredAt)}</span>
                <span className="min-w-0 flex-1">
                  <strong>{ACTION_TEXT(e.action)}</strong>
                  {e.details && <span className="block break-words text-slate-600">{e.details}</span>}
                </span>
                <span className="shrink-0 text-xs text-slate-500">{e.actorName ?? 'System'}</span>
              </li>
            ))}
          </ol>
          <div className="mt-6"><Pagination page={page} totalPages={log.data.totalPages} onChange={setPage} /></div>
        </>
      )}
    </>
  );
}
