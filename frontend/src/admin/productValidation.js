// Product form checks shared by the admin form and its tests.

const PRICE = /^\d{1,8}(\.\d{1,2})?$/;
const WHOLE = /^\d{1,7}$/;

/** Client checks mirroring the server rules (the server checks again). */
export function validateProduct(f) {
  const e = {};
  if (!f.name.trim()) e.name = 'Please enter the product name.';
  if (f.slug && !/^[a-z0-9]+(-[a-z0-9]+)*$/.test(f.slug)) e.slug = 'Use only lower-case letters, numbers and single hyphens.';
  if (!f.sku.trim()) e.sku = 'Please enter a product code.';
  else if (!/^[A-Za-z0-9][A-Za-z0-9._/-]*$/.test(f.sku.trim())) e.sku = 'Use only letters, numbers and . _ / -';
  if (!f.categoryId) e.categoryId = 'Please choose a category.';
  if (!f.brandId) e.brandId = 'Please choose a brand.';
  if (!f.packSize.trim()) e.packSize = 'Please enter the pack size, e.g. “Box of 10”.';
  if (!PRICE.test(f.mrp) || Number(f.mrp) <= 0) e.mrp = 'Enter a price above 0 (at most 2 decimal places).';
  if (!PRICE.test(f.price) || Number(f.price) <= 0) e.price = 'Enter a price above 0 (at most 2 decimal places).';
  else if (!e.mrp && Number(f.price) > Number(f.mrp)) e.price = 'The price cannot be more than the MRP.';
  if (!WHOLE.test(f.stockQuantity)) e.stockQuantity = 'Enter a whole number (0 or more).';
  if (!WHOLE.test(f.reorderLevel)) e.reorderLevel = 'Enter a whole number (0 or more).';
  return e;
}
