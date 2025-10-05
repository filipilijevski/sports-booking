/** Introduces cart persistence, extended product catalog attributes,
 *  enriched order & payment data, search indexes, and optimistic locking.
 *  Compatible with PostgreSQL 15.13 and Flyway 10.22.0. */

-- 0. Safeguard: run entire file in a single transaction
BEGIN;

/** Extensions */
-- Enable fuzzy-search capabilities for product search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

/** Product catalogue enhancements */
ALTER TABLE products
    ADD COLUMN sku            VARCHAR(32)  NOT NULL UNIQUE,
    ADD COLUMN inventory_qty  INT          NOT NULL DEFAULT 0,
    ADD COLUMN brand          VARCHAR(64),
    ADD COLUMN grams          INT,
    ADD COLUMN version        BIGINT       NOT NULL DEFAULT 0;

-- Keep descriptions searchable with full-text + trigram
DROP INDEX IF EXISTS idx_products_search;
CREATE INDEX idx_products_search
    ON products
    USING gin (to_tsvector('simple', name || ' ' || coalesce(description, '')));

-- Control gallery order
ALTER TABLE product_images
    ADD COLUMN sort_order SMALLINT NOT NULL DEFAULT 0;

/** Cart persistence */
CREATE TABLE IF NOT EXISTS carts (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_cart_user UNIQUE (user_id)          -- one open cart per user
);

CREATE TABLE IF NOT EXISTS cart_items (
    id          BIGSERIAL PRIMARY KEY,
    cart_id     BIGINT  NOT NULL REFERENCES carts(id)   ON DELETE CASCADE,
    product_id  BIGINT  NOT NULL REFERENCES products(id),
    quantity    INT     NOT NULL CHECK (quantity > 0),
    unit_price  NUMERIC(10,2) NOT NULL,                 -- price snapshot
    UNIQUE (cart_id, product_id)
);

/** Order & payment overhaul */
-- Domain enum for order.status
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'order_status') THEN
        CREATE TYPE order_status AS ENUM ('PENDING_PAYMENT', 'PAID', 'FULFILLED', 'CANCELLED');
    END IF;
END$$;

-- Orders table changes
ALTER TABLE orders
    -- Stripe & auditing
    ADD COLUMN stripe_payment_intent_id VARCHAR(64),
    ADD COLUMN updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN version                  BIGINT       NOT NULL DEFAULT 0,

    -- Financial breakdown
    ADD COLUMN subtotal_amount NUMERIC(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN tax_amount      NUMERIC(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN shipping_fee    NUMERIC(10,2) NOT NULL DEFAULT 0,

    -- Shipping address (denormalised snapshot)
    ADD COLUMN shipping_full_name   VARCHAR(128),
    ADD COLUMN shipping_phone       VARCHAR(32),
    ADD COLUMN shipping_email       VARCHAR(128),
    ADD COLUMN shipping_line1       VARCHAR(128),
    ADD COLUMN shipping_line2       VARCHAR(128),
    ADD COLUMN shipping_city        VARCHAR(64),
    ADD COLUMN shipping_province    VARCHAR(64),
    ADD COLUMN shipping_postal_code VARCHAR(16),
    ADD COLUMN shipping_country     VARCHAR(64);

-- Convert existing status column to ENUM
-- Safe because current data set should be empty; adjust USING clause if needed.
ALTER TABLE orders
    ALTER COLUMN status TYPE order_status
        USING status::order_status,
    ALTER COLUMN status SET DEFAULT 'PENDING_PAYMENT';

-- Rename & extend payments table for audit history
ALTER TABLE payments RENAME TO payment_events;

ALTER TABLE payment_events
    ADD COLUMN event_type    VARCHAR(64),  -- 'payment_intent.succeeded'
    ADD COLUMN payload_json  JSONB;        -- raw Stripe event for compliance

/* Triggers - keep updated_at current **/
-- Function to auto-update timestamp columns
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to carts, products, orders (avoid duplicates if rerun)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trg_carts_set_updated_at'
    ) THEN
        CREATE TRIGGER trg_carts_set_updated_at
            BEFORE UPDATE ON carts
            FOR EACH ROW
            EXECUTE FUNCTION set_updated_at();
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trg_products_set_updated_at'
    ) THEN
        CREATE TRIGGER trg_products_set_updated_at
            BEFORE UPDATE ON products
            FOR EACH ROW
            EXECUTE FUNCTION set_updated_at();
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trg_orders_set_updated_at'
    ) THEN
        CREATE TRIGGER trg_orders_set_updated_at
            BEFORE UPDATE ON orders
            FOR EACH ROW
            EXECUTE FUNCTION set_updated_at();
    END IF;
END$$;

/* Comments for self-documentation */
COMMENT ON COLUMN products.sku           IS 'Merchant-defined stock-keeping unit (unique, external-facing)';
COMMENT ON COLUMN products.inventory_qty IS 'Units on hand, decremented at checkout';
COMMENT ON COLUMN carts.user_id          IS 'Owner of the open cart';
COMMENT ON COLUMN orders.stripe_payment_intent_id IS 'Primary key linking to Stripe PaymentIntent';
COMMENT ON COLUMN orders.version         IS 'Optimistic-locking column (incremented on state change)';
COMMENT ON COLUMN payment_events.payload_json     IS 'Raw Stripe event payload for forensic/audit purposes';

COMMIT;

