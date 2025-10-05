/* V19__align_fk_column_types.sql
 *  Converts legacy INT4 foreign-key columns to BIGINT so they match
 *  the Pro Shop JPA entities (Java Long).  No data loss because the
 *  affected tables are currently empty. */
BEGIN;

/* 1. order_items.order_id  ->  BIGINT */
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS order_items_order_id_fkey;
ALTER TABLE order_items
    ALTER COLUMN order_id TYPE BIGINT USING order_id::bigint;
ALTER TABLE order_items
    ADD  CONSTRAINT fk_order_items_order
         FOREIGN KEY (order_id) REFERENCES orders(id);

/* 2. order_items.product_id  ->  BIGINT */
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS order_items_product_id_fkey;
ALTER TABLE order_items
    ALTER COLUMN product_id TYPE BIGINT USING product_id::bigint;
ALTER TABLE order_items
    ADD  CONSTRAINT fk_order_items_product
         FOREIGN KEY (product_id) REFERENCES products(id);

/* 3. payment_events.order_id  ->  BIGINT */
/* (table was renamed from payments in V18) */
ALTER TABLE payment_events DROP CONSTRAINT IF EXISTS payment_events_order_id_fkey;
ALTER TABLE payment_events
    ALTER COLUMN order_id TYPE BIGINT USING order_id::bigint;
ALTER TABLE payment_events
    ADD  CONSTRAINT fk_payment_events_order
         FOREIGN KEY (order_id) REFERENCES orders(id);

COMMIT;

