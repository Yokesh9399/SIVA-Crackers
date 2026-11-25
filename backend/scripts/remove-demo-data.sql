-- SIVA Crackers – remove the DEMONSTRATION catalogue
--
-- Deletes only rows marked is_demo = 1. Real products, brands, categories,
-- business settings, banners and user accounts are never touched.
--
-- Before running:
--   1. Set APP_SEED_DEMO=false in backend/.env (otherwise the demo data is loaded again on the next start).
--   2. Take a backup of the database (MySQL Workbench: Server > Data Export).
-- Run in MySQL Workbench against the siva_crackers database.

START TRANSACTION;

-- 1. Demo products (their images are removed automatically).
DELETE FROM products WHERE is_demo = 1;

-- 2. Demo brands that no remaining (real) product uses.
DELETE b FROM brands b
WHERE b.is_demo = 1
  AND NOT EXISTS (SELECT 1 FROM products p WHERE p.brand_id = b.id);

-- 3. Demo categories that no remaining (real) product uses (their safety notes go with them).
DELETE c FROM categories c
WHERE c.is_demo = 1
  AND NOT EXISTS (SELECT 1 FROM products p WHERE p.category_id = c.id);

-- Check what is left marked as demo (should be 0 rows unless a real product still uses a demo brand/category).
SELECT 'products' AS t, COUNT(*) FROM products WHERE is_demo = 1
UNION ALL SELECT 'brands', COUNT(*) FROM brands WHERE is_demo = 1
UNION ALL SELECT 'categories', COUNT(*) FROM categories WHERE is_demo = 1;

COMMIT;
