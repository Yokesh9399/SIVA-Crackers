import { AlertTriangle } from 'lucide-react';
import Modal from './Modal.jsx';

/**
 * "Are you sure?" for destructive actions. The safe choice (Cancel) has the focus.
 * Without `onConfirm` it is just an explanation with a Close button (e.g. a delete that is not allowed).
 */
export default function ConfirmDialog({ title, message, confirmLabel = 'Delete', busy = false, onConfirm, onCancel }) {
  return (
    <Modal
      title={title}
      onClose={onCancel}
      size="sm"
      closeDisabled={busy}
      footer={
        <>
          <button type="button" onClick={onCancel} disabled={busy} data-autofocus className="admin-btn-secondary">
            {onConfirm ? 'Cancel' : 'Close'}
          </button>
          {onConfirm && (
            <button type="button" onClick={onConfirm} disabled={busy} className="admin-btn-danger">
              {busy ? 'Please wait…' : confirmLabel}
            </button>
          )}
        </>
      }
    >
      <div className="flex gap-3">
        <span className="grid size-10 shrink-0 place-items-center rounded-full bg-red-50 text-red-600">
          <AlertTriangle className="size-5" aria-hidden="true" />
        </span>
        <div className="text-sm text-slate-700">{message}</div>
      </div>
    </Modal>
  );
}
