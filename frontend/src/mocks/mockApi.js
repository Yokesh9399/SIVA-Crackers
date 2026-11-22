// A small fake API used during Phases 1–3, before the Spring Boot backend exists.
//
// Every function returns a Promise and the same JSON shape the real REST API
// will return (see docs/PHASE_0_PLAN.md, section 9). Because the services call
// these functions instead of reading JSON directly, components never know
// whether the data is mock or real. In Phase 4 the services switch to Axios.

import productsData from './data/products.json';
import categoriesData from './data/categories.json';
import brandsData from './data/brands.json';
import settingsData from './data/settings.json';
import bannersData from './data/banners.json';

const LATENCY_MS = 250;
const LOW_STOCK_LEVEL = 10;

const delay = (value) => new Promise((resolve) => setTimeout(() => resolve(structuredClone(value)), LATENCY_MS));

const categoryById = new Map(categoriesData.map((c) => [c.id, c]));
const brandById = new Map(brandsData.map((b) => [b.id, b]));

// Shape a raw product row like the future ProductDTO.
function toProductDto(p) {
  const category = categoryById.get(p.categoryId);
  const brand = brandById.get(p.brandId);
  return {
    id: p.id,
    name: p.name,
    slug: p.slug,
    sku: p.sku,
    description: p.description,
    packSize: p.packSize,
    unit: p.unit,
    mrp: p.mrp,
    price: p.price,
    stockStatus: p.stock <= 0 ? 'OUT_OF_STOCK' : p.stock <= LOW_STOCK_LEVEL ? 'LOW_STOCK' : 'IN_STOCK',
    featured: p.featured,
    createdAt: p.createdAt,
    images: p.images ?? [],
    isDemo: Boolean(p.isDemo),
    category: { id: category.id, name: category.name, slug: category.slug },
    brand: { id: brand.id, name: brand.name, slug: brand.slug },
  };
}

const allProducts = productsData.map(toProductDto);
const productBySlug = new Map(allProducts.map((p) => [p.slug, p]));

class NotFoundError extends Error {
  constructor(message) {
    super(message);
    this.name = 'NotFoundError';
    this.status = 404;
  }
}

const reject = (error) => new Promise((_, rejectFn) => setTimeout(() => rejectFn(error), LATENCY_MS));

const categoryOrder = (p) => categoryById.get(p.category.id)?.displayOrder ?? 999;

const SORTERS = {
  // Default: grouped by category (shop's display order), then product order within it.
  default: (a, b) => categoryOrder(a) - categoryOrder(b) || a.id - b.id,
  'name,asc': (a, b) => a.name.localeCompare(b.name),
  'name,desc': (a, b) => b.name.localeCompare(a.name),
  'price,asc': (a, b) => a.price - b.price || a.name.localeCompare(b.name),
  'price,desc': (a, b) => b.price - a.price || a.name.localeCompare(b.name),
  newest: (a, b) => b.createdAt.localeCompare(a.createdAt) || b.id - a.id,
};

/** Every word typed must appear in the product name, brand name or category name. */
function matchesSearch(product, search) {
  const words = search.trim().toLowerCase().split(/\s+/).filter(Boolean);
  if (!words.length) return true;
  const haystack = `${product.name} ${product.brand.name} ${product.category.name}`.toLowerCase();
  return words.every((w) => haystack.includes(w));
}

/**
 * Mirrors GET /api/public/products
 * @param {object} params
 *   search      text matched against product, brand and category names
 *   categories  array of category slugs (OR within the list)
 *   brands      array of brand slugs (OR within the list)
 *   minPrice / maxPrice  indicative price bounds (inclusive)
 *   inStock     true = hide out-of-stock items
 *   sort        default | name,asc | name,desc | price,asc | price,desc | newest
 *   page        0-based page index
 *   size        page size
 * Different filter types combine with AND.
 */
export function getProducts(params = {}) {
  const {
    search = '',
    categories = [],
    brands = [],
    minPrice,
    maxPrice,
    inStock = false,
    sort = 'default',
    page = 0,
    size = 12,
  } = params;

  let list = allProducts.filter((p) => {
    if (!matchesSearch(p, search)) return false;
    if (categories.length && !categories.includes(p.category.slug)) return false;
    if (brands.length && !brands.includes(p.brand.slug)) return false;
    if (minPrice != null && p.price < minPrice) return false;
    if (maxPrice != null && p.price > maxPrice) return false;
    if (inStock && p.stockStatus === 'OUT_OF_STOCK') return false;
    return true;
  });

  list = [...list].sort(SORTERS[sort] ?? SORTERS.default);

  const totalElements = list.length;
  const totalPages = Math.max(1, Math.ceil(totalElements / size));
  const content = list.slice(page * size, page * size + size);

  return delay({ content, page, size, totalElements, totalPages });
}

/** Mirrors GET /api/public/products/featured */
export function getFeaturedProducts(limit = 8) {
  return delay(allProducts.filter((p) => p.featured).slice(0, limit));
}

/** Mirrors GET /api/public/products/new-arrivals */
export function getNewArrivals(limit = 8) {
  return delay([...allProducts].sort(SORTERS.newest).slice(0, limit));
}

/**
 * Mirrors GET /api/public/products/{slug} – full ProductDetailDTO.
 * Adds category safety notes. Rejects with status 404 for unknown slugs.
 */
export function getProductBySlug(slug) {
  const product = productBySlug.get(slug);
  if (!product) return reject(new NotFoundError(`Product "${slug}" not found`));
  const category = categoryById.get(product.category.id);
  return delay({ ...product, category: { ...product.category, description: category.description }, safetyNotes: category.safetyNotes ?? [] });
}

/** Mirrors GET /api/public/products/{slug}/related – same category first, then same brand. */
export function getRelatedProducts(slug, limit = 4) {
  const product = productBySlug.get(slug);
  if (!product) return delay([]);
  const others = allProducts.filter((p) => p.slug !== slug);
  const sameCategory = others.filter((p) => p.category.id === product.category.id);
  const sameBrand = others.filter((p) => p.brand.id === product.brand.id && p.category.id !== product.category.id);
  return delay([...sameCategory, ...sameBrand].slice(0, limit));
}

/** Mirrors GET /api/public/products?slugs=a,b – used by the Estimate List. Unknown slugs are skipped. */
export function getProductsBySlugs(slugs = []) {
  return delay(slugs.map((s) => productBySlug.get(s)).filter(Boolean));
}

/** Mirrors GET /api/public/price-list – every product grouped by category, in display order. */
export function getPriceList() {
  const groups = [...categoriesData]
    .filter((c) => c.active)
    .sort((a, b) => a.displayOrder - b.displayOrder)
    .map((c) => ({
      category: { id: c.id, name: c.name, slug: c.slug },
      products: allProducts.filter((p) => p.category.id === c.id).sort(SORTERS.default),
    }))
    .filter((g) => g.products.length > 0);
  return delay(groups);
}

/** Mirrors GET /api/public/price-range – lowest and highest indicative price. */
export function getPriceRange() {
  const prices = allProducts.map((p) => p.price);
  return delay({ min: Math.min(...prices), max: Math.max(...prices) });
}

/** Mirrors GET /api/public/categories (with product counts) */
export function getCategories() {
  const list = categoriesData
    .filter((c) => c.active)
    .sort((a, b) => a.displayOrder - b.displayOrder)
    .map((c) => ({ ...c, productCount: allProducts.filter((p) => p.category.id === c.id).length }));
  return delay(list);
}

/** Mirrors GET /api/public/brands (with product counts) */
export function getBrands() {
  const list = brandsData
    .filter((b) => b.active)
    .sort((a, b) => a.displayOrder - b.displayOrder)
    .map((b) => ({ ...b, productCount: allProducts.filter((p) => p.brand.id === b.id).length }));
  return delay(list);
}

/** Mirrors GET /api/public/settings */
export function getSettings() {
  return delay(settingsData);
}

/** Mirrors GET /api/public/banners */
export function getBanners() {
  return delay(bannersData);
}
