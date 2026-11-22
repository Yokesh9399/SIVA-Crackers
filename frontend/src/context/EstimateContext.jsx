import { useCallback, useEffect, useMemo, useState } from 'react';
import { ESTIMATE_STORAGE_KEY, EstimateContext, MAX_QTY } from './estimate-context.js';
import { readJson, writeJson } from '../utils/storage.js';

/**
 * My Estimate List
 * ----------------
 * LEGAL: this is a personal planning list ONLY. It is stored in the visitor's
 * own browser (localStorage) and is never sent to the shop or any server.
 * It is not an order, booking or reservation and has no checkout or payment.
 *
 * Only { slug, qty } pairs are stored; product names and prices are always
 * re-read from the catalogue so the list never holds stale or duplicated data.
 */

const clampQty = (qty) => Math.min(MAX_QTY, Math.max(1, Math.round(Number(qty) || 1)));

function load() {
  const saved = readJson(ESTIMATE_STORAGE_KEY, []);
  if (!Array.isArray(saved)) return [];
  return saved
    .filter((i) => i && typeof i.slug === 'string')
    .map((i) => ({ slug: i.slug, qty: clampQty(i.qty) }));
}

export function EstimateProvider({ children }) {
  const [items, setItems] = useState(load);

  // Save on every change.
  useEffect(() => {
    writeJson(ESTIMATE_STORAGE_KEY, items);
  }, [items]);

  // Keep several open tabs in step.
  useEffect(() => {
    const onStorage = (e) => e.key === ESTIMATE_STORAGE_KEY && setItems(load());
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, []);

  const add = useCallback((slug, qty = 1) => {
    setItems((list) => {
      const existing = list.find((i) => i.slug === slug);
      if (existing) return list.map((i) => (i.slug === slug ? { ...i, qty: clampQty(i.qty + qty) } : i));
      return [...list, { slug, qty: clampQty(qty) }];
    });
  }, []);

  const setQty = useCallback((slug, qty) => {
    setItems((list) => list.map((i) => (i.slug === slug ? { ...i, qty: clampQty(qty) } : i)));
  }, []);

  const remove = useCallback((slug) => setItems((list) => list.filter((i) => i.slug !== slug)), []);
  const clear = useCallback(() => setItems([]), []);

  const value = useMemo(
    () => ({
      items,
      count: items.length,
      qtyOf: (slug) => items.find((i) => i.slug === slug)?.qty ?? 0,
      add,
      setQty,
      remove,
      clear,
    }),
    [items, add, setQty, remove, clear],
  );

  return <EstimateContext.Provider value={value}>{children}</EstimateContext.Provider>;
}
