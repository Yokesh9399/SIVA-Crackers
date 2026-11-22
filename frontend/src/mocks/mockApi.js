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

const SORTERS = {
  'name,asc': (a, b) => a.name.localeCompare(b.name),
  'name,desc': (a, b) => b.name.localeCompare(a.name),
  'price,asc': (a, b) => a.price - b.price,
  'price,desc': (a, b) => b.price - a.price,
  newest: (a, b) => b.createdAt.localeCompare(a.createdAt) || b.id - a.id,
};

/**
 * Mirrors GET /api/public/products
 * @param {object} params search, categoryIds[], brandIds[], minPrice, maxPrice, inStock, sort, page (0-based), size
 */
export function getProducts(params = {}) {
  const {
    search = '',
    categoryIds = [],
    brandIds = [],
    minPrice,
    maxPrice,
    inStock = false,
    sort = 'name,asc',
    page = 0,
    size = 12,
  } = params;

  const term = search.trim().toLowerCase();
  let list = allProducts.filter((p) => {
    if (term && !p.name.toLowerCase().includes(term)) return false;
    if (categoryIds.length && !categoryIds.includes(p.category.id)) return false;
    if (brandIds.length && !brandIds.includes(p.brand.id)) return false;
    if (minPrice != null && p.price < minPrice) return false;
    if (maxPrice != null && p.price > maxPrice) return false;
    if (inStock && p.stockStatus === 'OUT_OF_STOCK') return false;
    return true;
  });

  list = [...list].sort(SORTERS[sort] ?? SORTERS['name,asc']);

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
