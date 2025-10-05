/*  Converts product_images.product_id from INT4 to BIGINT so it
 *  matches products.id (BIGINT) and the JPA model (Long). */
BEGIN;

/* 1. drop existing FK if any (safe if it never existed) */
ALTER TABLE product_images
    DROP CONSTRAINT IF EXISTS product_images_product_id_fkey;

/* 2. cast the column to BIGINT */
ALTER TABLE product_images
    ALTER COLUMN product_id TYPE BIGINT USING product_id::bigint;

/* 3. recreate the FK for referential integrity */
ALTER TABLE product_images
    ADD  CONSTRAINT fk_product_images_product
         FOREIGN KEY (product_id) REFERENCES products(id);

COMMIT;

