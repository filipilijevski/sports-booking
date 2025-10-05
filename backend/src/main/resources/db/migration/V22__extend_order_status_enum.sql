ALTER TABLE orders
    DROP CONSTRAINT IF EXISTS orders_status_check;

/* extend the PostgreSQL enum so that it matches our Java enum */
ALTER TYPE order_status
    ADD VALUE IF NOT EXISTS 'PENDING_PAYMENT' BEFORE 'PAID';

ALTER TYPE order_status
    ADD VALUE IF NOT EXISTS 'FULFILLED'       AFTER  'PAID';
