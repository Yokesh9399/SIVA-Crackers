/**
 * SIVA Crackers – browser regression check (Phase 4.7).
 *
 * Loads every public route in a same-origin iframe and checks it against the live API.
 * Read-only: it never logs in, submits a form or changes data.
 *
 * Run with the backend (demo data) and `npm run dev` started, then in the browser console at
 * http://localhost:5173:
 *
 *   const { runRegression } = await import('/scripts/regression-check.js');
 *   const report = await runRegression({ width: 1440 });       // desktop
 *   const mobile = await runRegression({ width: 375, quick: true }); // phone, main routes only
 *   console.table(report.failed);
 */

const FORBIDDEN_UI = /\b(add to cart|checkout|buy now|pay now|place order|book now|reserve now|proceed to pay)\b/i;
const LEGAL = ['disclaimer', 'privacy', 'terms'];
const STATIC_ROUTES = ['/', '/products', '/categories', '/brands', '/price-list', '/estimate', '/about', '/contact', '/safety', '/login', '/register'];

const api = (path) =>
  fetch(`/api${path}`, { credentials: 'same-origin' }).then((r) => {
    if (!r.ok) throw new Error(`${path} → ${r.status}`);
    return r.json();
  });

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const inr = (n) => new Intl.NumberFormat('en-IN').format(n);

function makeFrame(width) {
  const frame = document.createElement('iframe');
  frame.style.cssText = `position:fixed;left:-${width + 50}px;top:0;width:${width}px;height:900px;border:0`;
  frame.setAttribute('aria-hidden', 'true');
  document.body.appendChild(frame);
  return frame;
}

/** Navigates the iframe and waits until loaders/skeletons are gone. */
async function load(frame, path) {
  await new Promise((resolve) => {
    frame.onload = resolve;
    frame.src = path;
  });
  const started = Date.now();
  for (;;) {
    const doc = frame.contentDocument;
    const busy =
      !doc?.querySelector('h1') ||
      doc.querySelector('[role="status"][aria-label="Loading"]') ||
      doc.querySelector('.animate-pulse');
    if (!busy) break;
    if (Date.now() - started > 12000) break;
    await sleep(100);
  }
  await sleep(150);
  return frame.contentWindow;
}

function knownLink(path, data) {
  const [p] = path.split(/[?#]/);
  if (STATIC_ROUTES.includes(p) || p === '/account') return true;
  let m;
  if ((m = p.match(/^\/products\/([^/]+)$/))) return data.productSlugs.has(m[1]);
  if ((m = p.match(/^\/categories\/([^/]+)$/))) return data.categorySlugs.has(m[1]);
  if ((m = p.match(/^\/brands\/([^/]+)$/))) return data.brandSlugs.has(m[1]);
  if ((m = p.match(/^\/legal\/([^/]+)$/))) return LEGAL.includes(m[1]);
  return false;
}

export async function runRegression({ width = 1440, quick = false } = {}) {
  const results = [];
  const check = (group, name, ok, detail = '') => results.push({ group, name, ok: Boolean(ok), detail: ok ? '' : String(detail) });

  // ---- reference data from the API
  const [page, categories, brands, settings, priceList] = await Promise.all([
    api('/public/products?size=48'),
    api('/public/categories'),
    api('/public/brands'),
    api('/public/settings'),
    api('/public/price-list'),
  ]);
  const products = page.content;
  const data = {
    productSlugs: new Set(products.map((p) => p.slug)),
    categorySlugs: new Set(categories.map((c) => c.slug)),
    brandSlugs: new Set(brands.map((b) => b.slug)),
  };
  check('api', 'catalogue has products', products.length > 0 && page.totalElements === products.length, page.totalElements);
  check('api', 'settings: known facts only', settings.shopName === 'SIVA Crackers' && settings.address?.area === 'Eachanari');

  const frame = makeFrame(width);
  const links = new Set();

  async function visit(group, path, expect = {}) {
    const win = await load(frame, path);
    const doc = win.document;
    const text = doc.body.innerText;
    const label = `${path} @${width}`;
    const finalPath = win.location.pathname;
    check(group, `${label} path`, finalPath === (expect.path ?? path.split('?')[0]), finalPath);
    const h1 = doc.querySelector('h1')?.textContent.trim();
    check(group, `${label} has h1`, h1, 'no h1');
    if (expect.h1) check(group, `${label} h1 = ${expect.h1}`, h1 === expect.h1, h1);
    check(group, `${label} title`, /SIVA Crackers/.test(doc.title), doc.title);
    check(group, `${label} no error state`, !/Something went wrong|Unable to reach the server/.test(text), 'error text shown');
    check(group, `${label} no horizontal overflow`, doc.documentElement.scrollWidth <= doc.documentElement.clientWidth + 1,
      `${doc.documentElement.scrollWidth} > ${doc.documentElement.clientWidth}`);
    const broken = [...doc.images].filter((img) => img.complete && img.naturalWidth === 0 && img.src).map((i) => i.src);
    check(group, `${label} no broken images`, broken.length === 0, broken.join(', '));
    check(group, `${label} demo notice`, /Demonstration data/.test(text) === Boolean(settings.demoMode), 'demo notice mismatch');
    const forbidden = [...doc.querySelectorAll('button, a, input[type=submit]')]
      .map((el) => el.textContent || el.value)
      .filter((t) => FORBIDDEN_UI.test(t));
    check(group, `${label} no online-sale controls`, forbidden.length === 0, forbidden.join(', '));
    for (const a of doc.querySelectorAll('a[href^="/"]')) links.add(a.getAttribute('href'));
    return { doc, text };
  }

  try {
    // ---- 1. static pages
    for (const path of STATIC_ROUTES) await visit('pages', path);
    for (const slug of LEGAL) await visit('pages', `/legal/${slug}`);

    // ---- 2. catalogue counts vs API
    const productsPage = await visit('catalogue', '/products');
    const total = productsPage.text.match(/of\s+(\d+)\s+products?/i)?.[1];
    check('catalogue', '/products count = API', Number(total) === page.totalElements, `${total} vs ${page.totalElements}`);

    const firstCat = categories.find((c) => c.productCount > 0);
    const catApi = await api(`/public/products?categories=${firstCat.slug}&sort=price,asc&size=48`);
    const filtered = await visit('catalogue', `/products?category=${firstCat.slug}&sort=price,asc`, { path: '/products' });
    const filteredTotal = filtered.text.match(/of\s+(\d+)\s+products?/i)?.[1];
    check('catalogue', 'category filter count = API', Number(filteredTotal) === catApi.totalElements, `${filteredTotal} vs ${catApi.totalElements}`);
    const cardOrder = [...new Set([...filtered.doc.querySelectorAll('main a[href^="/products/"]')].map((a) => a.getAttribute('href')))];
    check('catalogue', 'price sort order = API', cardOrder[0] === `/products/${catApi.content[0].slug}`, `${cardOrder[0]} vs ${catApi.content[0].slug}`);

    const search = products[0].name.split(' ').slice(-1)[0];
    const searchApi = await api(`/public/products?search=${encodeURIComponent(search)}&size=48`);
    const searched = await visit('catalogue', `/products?search=${encodeURIComponent(search)}`, { path: '/products' });
    const searchTotal = searched.text.match(/of\s+(\d+)\s+products?/i)?.[1];
    check('catalogue', `search "${search}" count = API`, Number(searchTotal) === searchApi.totalElements, `${searchTotal} vs ${searchApi.totalElements}`);

    const pl = await visit('catalogue', '/price-list');
    const plCount = priceList.reduce((n, g) => n + g.products.length, 0);
    const missing = priceList.flatMap((g) => g.products).filter((p) => !pl.text.includes(p.name)).map((p) => p.name);
    check('catalogue', `price list shows all ${plCount} products`, missing.length === 0, missing.join(', '));

    // ---- 3. every category, brand and product page
    const cats = quick ? categories.slice(0, 2) : categories;
    for (const c of cats) {
      const { text } = await visit('categories', `/categories/${c.slug}`, { h1: c.name });
      const n = text.match(/of\s+(\d+)\s+products?/i)?.[1];
      if (c.productCount > 0) check('categories', `${c.slug} count = ${c.productCount}`, Number(n) === c.productCount, n);
    }
    const brs = quick ? brands.slice(0, 2) : brands;
    for (const b of brs) {
      const { text } = await visit('brands', `/brands/${b.slug}`);
      const n = text.match(/of\s+(\d+)\s+products?/i)?.[1];
      if (b.productCount > 0) check('brands', `${b.slug} count = ${b.productCount}`, Number(n) === b.productCount, n);
    }
    const prods = quick ? products.slice(0, 3) : products;
    for (const p of prods) {
      const { text } = await visit('products', `/products/${p.slug}`, { h1: p.name });
      check('products', `${p.slug} price ₹${inr(p.price)}`, text.includes(inr(p.price)), 'price not shown');
      check('products', `${p.slug} brand + category`, text.includes(p.brand.name) && text.includes(p.category.name), 'missing');
    }

    // ---- 4. not found and protected routes
    const nf = await visit('routing', '/products/this-does-not-exist', { path: '/products/this-does-not-exist' });
    check('routing', 'unknown product → not found', /not found|fizzled|no longer/i.test(nf.text), 'no not-found message');
    await visit('routing', '/no-such-page', { h1: 'This page fizzled out' });
    await visit('routing', '/account', { path: '/login', h1: 'Login' });

    // ---- 5. internal links found on all visited pages
    const bad = [...links].filter((href) => !knownLink(href, data));
    check('links', `${links.size} internal links resolve`, bad.length === 0, bad.join(', '));
  } finally {
    frame.remove();
  }

  const failed = results.filter((r) => !r.ok);
  return { width, total: results.length, passed: results.length - failed.length, failed, results };
}
