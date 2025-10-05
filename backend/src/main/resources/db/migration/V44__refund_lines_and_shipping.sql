-- Adds shipping markers to refund_events and creates refund_lines.
-- Safe defaults ensure existing rows remain valid.

ALTER TABLE refund_events
  ADD COLUMN IF NOT EXISTS includes_shipping boolean NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS shipping_amount   numeric(10,2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS refund_lines (
  id               bigserial PRIMARY KEY,
  refund_event_id  bigint NOT NULL REFERENCES refund_events(id) ON DELETE CASCADE,
  order_item_id    bigint NOT NULL REFERENCES order_items(id),
  quantity         integer NOT NULL CHECK (quantity > 0)
);
CREATE INDEX IF NOT EXISTS idx_refund_lines_refund     ON refund_lines(refund_event_id);
CREATE INDEX IF NOT EXISTS idx_refund_lines_order_item ON refund_lines(order_item_id);

