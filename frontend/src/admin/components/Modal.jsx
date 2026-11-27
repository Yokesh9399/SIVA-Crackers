import { useEffect, useId, useRef } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';

/**
 * Accessible dialog: Escape closes, focus moves inside and returns afterwards, Tab stays inside,
 * the page behind does not scroll, and the dialog always fits the screen (its body scrolls).
 */
export default function Modal({ title, onClose, children, footer, size = 'md', closeDisabled = false }) {
  const titleId = useId();
  const panel = useRef(null);
  const latest = useRef({ onClose, closeDisabled });
  useEffect(() => {
    latest.current = { onClose, closeDisabled };
  });

  // Runs once when the dialog opens (callbacks are read from `latest`, so typing never moves the focus).
  useEffect(() => {
    const previous = document.activeElement;
    const node = panel.current;
    const focusables = () =>
      [...node.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])')].filter(
        (el) => !el.disabled && el.offsetParent !== null,
      );
    (focusables().find((el) => el.dataset.autofocus !== undefined) ?? focusables()[0] ?? node).focus();

    const onKey = (e) => {
      if (e.key === 'Escape' && !latest.current.closeDisabled) {
        e.stopPropagation();
        latest.current.onClose();
      }
      if (e.key === 'Tab') {
        const items = focusables();
        if (!items.length) return;
        const first = items[0];
        const last = items[items.length - 1];
        if (e.shiftKey && document.activeElement === first) {
          e.preventDefault();
          last.focus();
        } else if (!e.shiftKey && document.activeElement === last) {
          e.preventDefault();
          first.focus();
        }
      }
    };
    node.addEventListener('keydown', onKey);
    const overflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      node.removeEventListener('keydown', onKey);
      document.body.style.overflow = overflow;
      previous?.focus?.();
    };
  }, []);

  const width = { sm: 'max-w-md', md: 'max-w-xl', lg: 'max-w-3xl' }[size];

  // Rendered on <body> (never inside a page's own <form>), and a form submitted inside the dialog never
  // reaches a form of the page behind it (React events bubble through portals).
  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-navy-950/60 p-0 sm:items-center sm:p-4"
      onSubmit={(e) => e.stopPropagation()}
    >
      <div
        ref={panel}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        className={`flex max-h-[92dvh] w-full ${width} flex-col rounded-t-2xl bg-white shadow-2xl sm:rounded-2xl`}
      >
        <div className="flex items-center justify-between gap-3 border-b border-slate-100 px-5 py-4">
          <h2 id={titleId} className="text-lg font-bold">
            {title}
          </h2>
          <button
            type="button"
            onClick={onClose}
            disabled={closeDisabled}
            className="-m-2 grid size-10 place-items-center rounded-full text-slate-500 hover:bg-slate-100 hover:text-navy-900"
            aria-label="Close dialog"
          >
            <X className="size-5" aria-hidden="true" />
          </button>
        </div>
        <div className="min-h-0 flex-1 overflow-y-auto px-5 py-4">{children}</div>
        {footer && <div className="flex flex-wrap justify-end gap-2 border-t border-slate-100 px-5 py-3">{footer}</div>}
      </div>
    </div>,
    document.body,
  );
}
