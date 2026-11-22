import { useEffect, useState } from 'react';
import { Search, X } from 'lucide-react';

const DEBOUNCE_MS = 350;

/**
 * Instant search box. Updates the results 350 ms after typing stops,
 * or immediately on Enter. Searches product, brand and category names.
 *
 * `value` is the search currently in the URL; `onSearch(text, { replace })` updates it.
 */
export default function CatalogueSearch({ value, onSearch, id = 'catalogue-search' }) {
  const [text, setText] = useState(value);
  const [synced, setSynced] = useState(value);
  const [lastSent, setLastSent] = useState(value);

  // When the URL changes from outside (e.g. "Clear all", Back button), show the new value.
  // Adjusting state during render is React's recommended pattern for this.
  if (value !== synced) {
    setSynced(value);
    if (value !== lastSent) setText(value);
  }

  useEffect(() => {
    const trimmed = text.trim();
    if (trimmed === value) return undefined;
    const timer = setTimeout(() => {
      setLastSent(trimmed);
      onSearch(trimmed, { replace: true });
    }, DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [text, value, onSearch]);

  const submit = (event) => {
    event.preventDefault();
    const trimmed = text.trim();
    setLastSent(trimmed);
    onSearch(trimmed);
  };

  const clear = () => {
    setText('');
    setLastSent('');
    onSearch('');
  };

  return (
    <form role="search" onSubmit={submit} className="relative w-full">
      <label htmlFor={id} className="sr-only">
        Search by product, brand or category
      </label>
      <Search className="pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2 text-slate-400" aria-hidden="true" />
      <input
        id={id}
        type="search"
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder="Search product, brand or category…"
        autoComplete="off"
        className="w-full rounded-full border border-slate-200 bg-white py-2.5 pr-10 pl-10 text-sm text-navy-900 shadow-sm placeholder:text-slate-400 focus:border-orange-500 focus:ring-2 focus:ring-orange-500/20 focus:outline-none [&::-webkit-search-cancel-button]:hidden"
      />
      {text && (
        <button
          type="button"
          onClick={clear}
          className="absolute top-1/2 right-2 -translate-y-1/2 rounded-full p-1.5 text-slate-400 hover:bg-slate-100 hover:text-navy-900"
          aria-label="Clear search"
        >
          <X className="size-4" aria-hidden="true" />
        </button>
      )}
    </form>
  );
}
