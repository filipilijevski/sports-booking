-- 1) Allow guest orders (no user_id)
ALTER TABLE orders ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE order_items ALTER COLUMN product_id DROP NOT NULL;

-- 2) Category delete should not prevent product deletion and must not delete products
DO $$
BEGIN
  IF EXISTS (
      SELECT 1 FROM information_schema.table_constraints
       WHERE constraint_name = 'products_category_id_fkey'
         AND table_name = 'products'
  ) THEN
    ALTER TABLE products DROP CONSTRAINT products_category_id_fkey;
  END IF;
END$$;

ALTER TABLE products
  ADD CONSTRAINT products_category_id_fkey
  FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL;

-- 3) Helpful indexes to keep admin search snappy for guest filters
CREATE INDEX IF NOT EXISTS idx_orders_shipping_email_lower
  ON orders (LOWER(shipping_email));

CREATE INDEX IF NOT EXISTS idx_orders_shipping_name_lower
  ON orders (LOWER(shipping_full_name));

