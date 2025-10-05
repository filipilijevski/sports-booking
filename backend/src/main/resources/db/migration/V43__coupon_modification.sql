-- Coupons: activation window and manual toggle
ALTER TABLE coupons
  ADD COLUMN IF NOT EXISTS starts_at  timestamptz NOT NULL DEFAULT now(),
  ADD COLUMN IF NOT EXISTS active     boolean     NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS idx_coupons_active     ON coupons(active);
CREATE INDEX IF NOT EXISTS idx_coupons_starts_at  ON coupons(starts_at);

-- Orders: offline payment method (for in-person/manual orders)
ALTER TABLE orders
  ADD COLUMN IF NOT EXISTS offline_payment_method varchar(16);
