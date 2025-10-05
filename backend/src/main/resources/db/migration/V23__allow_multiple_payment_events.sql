ALTER TABLE payment_events
    DROP CONSTRAINT IF EXISTS payments_order_id_key;

-- keep it fast for look-ups
CREATE INDEX IF NOT EXISTS idx_payment_events_order
    ON payment_events(order_id);
