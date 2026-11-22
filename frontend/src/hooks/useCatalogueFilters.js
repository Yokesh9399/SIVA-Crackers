import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';

// Catalogue state lives in the URL so results can be bookmarked/shared and the
// browser Back button works. Example:
//   /products?search=rocket&category=rockets,aerial-shots&brand=minnal-pyro&min=100&max=500&stock=in&sort=price,asc&page=2&size=24

export const SORT_OPTIONS = [
  { value: 'default', label: 'Default (by category)' },
  { value: 'name,asc', label: 'Name: A to Z' },
  { value: 'name,desc', label: 'Name: Z to A' },
  { value: 'price,asc', label: 'Price: low to high' },
  { value: 'price,desc', label: 'Price: high to low' },
  { value: 'newest', label: 'Newest first' },
];

export const PAGE_SIZES = [12, 24, 48];
const DEFAULT_SIZE = PAGE_SIZES[0];

const list = (value) => (value ? value.split(',').map((s) => s.trim()).filter(Boolean) : []);

function price(value) {
  if (value === null || value === '') return null;
  const n = Number(value);
  return Number.isFinite(n) && n >= 0 ? n : null;
}

/** Reads and validates every filter from the query string. Bad values fall back to defaults. */
function parse(params) {
  const sort = params.get('sort');
  const size = Number(params.get('size'));
  const page = Number(params.get('page'));
  return {
    search: params.get('search')?.trim() ?? '',
    categories: list(params.get('category')),
    brands: list(params.get('brand')),
    minPrice: price(params.get('min')),
    maxPrice: price(params.get('max')),
    inStock: params.get('stock') === 'in',
    sort: SORT_OPTIONS.some((o) => o.value === sort) ? sort : 'default',
    size: PAGE_SIZES.includes(size) ? size : DEFAULT_SIZE,
    page: Number.isInteger(page) && page > 0 ? page : 1, // 1-based in the URL
  };
}

// Filter name → query-string key
const KEYS = {
  search: 'search',
  categories: 'category',
  brands: 'brand',
  minPrice: 'min',
  maxPrice: 'max',
  inStock: 'stock',
  sort: 'sort',
  size: 'size',
  page: 'page',
};

function serialise(key, value) {
  if (value === null || value === undefined || value === '' || value === false) return null;
  if (Array.isArray(value)) return value.length ? value.join(',') : null;
  if (key === 'inStock') return 'in';
  if (key === 'sort' && value === 'default') return null;
  if (key === 'size' && value === DEFAULT_SIZE) return null;
  if (key === 'page' && value === 1) return null;
  return String(value);
}

/**
 * @param {object} fixed  Filters locked by the page, e.g. { category: 'rockets' } on a category page.
 *                        They are applied to the API call but not shown as removable filters.
 */
export function useCatalogueFilters(fixed = {}) {
  const [searchParams, setSearchParams] = useSearchParams();
  const filters = useMemo(() => parse(searchParams), [searchParams]);

  /**
   * Change one or more filters. Any change other than `page` sends the user back to page 1.
   * `replace: true` avoids filling browser history (used while typing in the search box).
   */
  const update = useCallback(
    (changes, { replace = false } = {}) => {
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          Object.entries(changes).forEach(([name, value]) => {
            const key = KEYS[name];
            const text = serialise(name, value);
            if (text === null) next.delete(key);
            else next.set(key, text);
          });
          if (!('page' in changes)) next.delete('page');
          return next;
        },
        { replace },
      );
    },
    [setSearchParams],
  );

  /** Remove every filter but keep the chosen sort order and page size. */
  const clearAll = useCallback(() => {
    setSearchParams((prev) => {
      const next = new URLSearchParams();
      ['sort', 'size'].forEach((k) => prev.get(k) && next.set(k, prev.get(k)));
      return next;
    });
  }, [setSearchParams]);

  const toggleIn = useCallback(
    (name, value) => {
      const current = filters[name];
      update({ [name]: current.includes(value) ? current.filter((v) => v !== value) : [...current, value] });
    },
    [filters, update],
  );

  // Parameters sent to productService.getProducts (0-based page, like Spring's Pageable).
  const apiParams = useMemo(
    () => ({
      search: filters.search,
      categories: fixed.category ? [fixed.category] : filters.categories,
      brands: fixed.brand ? [fixed.brand] : filters.brands,
      minPrice: filters.minPrice,
      maxPrice: filters.maxPrice,
      inStock: filters.inStock,
      sort: filters.sort,
      page: filters.page - 1,
      size: filters.size,
    }),
    [filters, fixed.category, fixed.brand],
  );

  const activeCount =
    (filters.search ? 1 : 0) +
    (fixed.category ? 0 : filters.categories.length) +
    (fixed.brand ? 0 : filters.brands.length) +
    (filters.minPrice != null || filters.maxPrice != null ? 1 : 0) +
    (filters.inStock ? 1 : 0);

  return { filters, apiParams, update, toggleIn, clearAll, activeCount };
}
