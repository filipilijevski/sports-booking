-- 1) membership_groups (new)
CREATE TABLE membership_groups (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  owner_id BIGINT NOT NULL REFERENCES users(id),
  plan_id BIGINT NOT NULL REFERENCES membership_plans(id),
  start_ts TIMESTAMP NOT NULL,
  end_ts   TIMESTAMP NOT NULL,
  active   BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2) membership_plans.holder_kind (default INDIVIDUAL)
ALTER TABLE membership_plans
  ADD COLUMN holder_kind VARCHAR(16) NOT NULL DEFAULT 'INDIVIDUAL';

-- 3) user_memberships.group_id (nullable)
ALTER TABLE user_memberships
  ADD COLUMN group_id BIGINT NULL,
  ADD CONSTRAINT fk_um_group FOREIGN KEY (group_id) REFERENCES membership_groups(id);

-- 4) membership_group_counters (new)
CREATE TABLE membership_group_counters (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  group_id BIGINT NOT NULL REFERENCES membership_groups(id),
  kind VARCHAR(32) NOT NULL,
  amount_consumed NUMERIC(10,2) NOT NULL DEFAULT 0,
  CONSTRAINT uq_mgc_by_kind UNIQUE (group_id, kind)
);

-- 5) table_rental_credits.group_id (nullable)
ALTER TABLE table_rental_credits
  ADD COLUMN group_id BIGINT NULL REFERENCES membership_groups(id);

-- 6) membership_payments (new)
CREATE TABLE membership_payments (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  plan_id BIGINT NOT NULL REFERENCES membership_plans(id),
  start_ts TIMESTAMP NOT NULL,
  end_ts   TIMESTAMP NOT NULL,
  status   VARCHAR(16) NOT NULL,
  price_cad NUMERIC(10,2) NOT NULL,
  tax_cad   NUMERIC(10,2) NOT NULL,
  total_cad NUMERIC(10,2) NOT NULL,
  currency  VARCHAR(10) NOT NULL,
  stripe_payment_intent_id VARCHAR(100),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_mp_pi ON membership_payments(stripe_payment_intent_id);

