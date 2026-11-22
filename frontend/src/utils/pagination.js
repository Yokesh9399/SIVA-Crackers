/** Page numbers to show, with gaps: getPageItems(5, 12) → [1, 'gap-4', 4, 5, 6, 'gap-12', 12] */
export function getPageItems(current, total) {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
  const pages = new Set([1, total, current - 1, current, current + 1]);
  const sorted = [...pages].filter((p) => p >= 1 && p <= total).sort((a, b) => a - b);
  const items = [];
  sorted.forEach((p, i) => {
    if (i > 0 && p - sorted[i - 1] > 1) items.push(`gap-${p}`);
    items.push(p);
  });
  return items;
}

/** "Showing 13–24 of 48" numbers for a 0-based page response. */
export function resultRange({ page, size, totalElements, content }) {
  if (!totalElements) return { from: 0, to: 0, total: 0 };
  const from = page * size + 1;
  return { from, to: from + content.length - 1, total: totalElements };
}
