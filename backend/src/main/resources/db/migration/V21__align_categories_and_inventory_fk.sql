/*  1. Promote categories.id  and products.category_id  to BIGINT
 *  2. Promote inventory.product_id                  to BIGINT
 *  All other INT4 columns discovered are quantities or weights and
 *  *intentionally* stay as INTEGER. */
BEGIN;

/* Helper function: returns TRUE if a column is still plain INTEGER    */
CREATE OR REPLACE FUNCTION column_is_int4(p_table TEXT, p_col TEXT)
RETURNS BOOLEAN AS $$
SELECT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name   = p_table
      AND column_name  = p_col
      AND data_type    = 'integer'
); $$ LANGUAGE SQL IMMUTABLE;

/* 1. categories.id  (PK) */
DO $$
BEGIN
    IF column_is_int4('categories','id') THEN
        -- drop dependent FKs first
        ALTER TABLE products
            DROP CONSTRAINT IF EXISTS products_category_id_fkey;

        ALTER TABLE categories
            ALTER COLUMN id TYPE BIGINT USING id::bigint;

        -- re-apply FK after type change
        ALTER TABLE products
            ADD CONSTRAINT fk_products_category
                FOREIGN KEY (category_id) REFERENCES categories(id);
    END IF;
END $$;

/* 2. products.category_id  (FK) */
DO $$
BEGIN
    IF column_is_int4('products','category_id') THEN
        ALTER TABLE products
            ALTER COLUMN category_id TYPE BIGINT USING category_id::bigint;
    END IF;
END $$;

/* 3. inventory.product_id  (FK -> products.id) */
DO $$
BEGIN
    IF column_is_int4('inventory','product_id') THEN
        ALTER TABLE inventory
            DROP CONSTRAINT IF EXISTS inventory_product_id_fkey;

        ALTER TABLE inventory
            ALTER COLUMN product_id TYPE BIGINT USING product_id::bigint;

        ALTER TABLE inventory
            ADD CONSTRAINT fk_inventory_product
                FOREIGN KEY (product_id) REFERENCES products(id);
    END IF;
END $$;

/* 4. Clean-up helper */
DROP FUNCTION IF EXISTS column_is_int4(TEXT, TEXT);

COMMIT;

