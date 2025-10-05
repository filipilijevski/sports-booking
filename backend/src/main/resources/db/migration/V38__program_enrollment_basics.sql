-- 1) programs.enrollment_mode
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'program_enrollment_mode') THEN
    CREATE TYPE program_enrollment_mode AS ENUM ('OPEN', 'ADMIN_ONLY');
  END IF;
END$$;

ALTER TABLE programs
  ADD COLUMN IF NOT EXISTS enrollment_mode program_enrollment_mode NOT NULL DEFAULT 'OPEN';

-- 2) program_enrollment_payments
CREATE TABLE IF NOT EXISTS program_enrollment_payments (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  program_id BIGINT NOT NULL REFERENCES programs(id),
  program_package_id BIGINT NOT NULL REFERENCES program_packages(id),
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  price_cad NUMERIC(10,2) NOT NULL,
  tax_cad   NUMERIC(10,2) NOT NULL,
  total_cad NUMERIC(10,2) NOT NULL,
  currency  VARCHAR(10)   NOT NULL DEFAULT 'cad',
  stripe_payment_intent_id VARCHAR(100),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_pep_user      ON program_enrollment_payments(user_id);
CREATE INDEX IF NOT EXISTS ix_pep_program   ON program_enrollment_payments(program_id);
CREATE INDEX IF NOT EXISTS ix_pep_pkg       ON program_enrollment_payments(program_package_id);
CREATE INDEX IF NOT EXISTS ix_pep_pi        ON program_enrollment_payments(stripe_payment_intent_id);

-- 3) user_program_enrollments
CREATE TABLE IF NOT EXISTS user_program_enrollments (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  program_id BIGINT NOT NULL REFERENCES programs(id),
  program_package_id BIGINT NOT NULL REFERENCES program_packages(id),
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  sessions_purchased INTEGER NOT NULL,
  sessions_remaining INTEGER NOT NULL,
  start_ts TIMESTAMPTZ NOT NULL DEFAULT now(),
  end_ts   TIMESTAMPTZ NULL,
  last_attended_at TIMESTAMPTZ NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_upe_user    ON user_program_enrollments(user_id);
CREATE INDEX IF NOT EXISTS ix_upe_program ON user_program_enrollments(program_id);
CREATE INDEX IF NOT EXISTS ix_upe_pkg     ON user_program_enrollments(program_package_id);

-- Enforce "only one ACTIVE enrollment per (user, program)"
-- (PostgreSQL partial unique index)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_indexes
    WHERE schemaname = current_schema()
      AND indexname = 'uq_upe_active_per_user_program'
  ) THEN
    CREATE UNIQUE INDEX uq_upe_active_per_user_program
      ON user_program_enrollments(user_id, program_id)
      WHERE status = 'ACTIVE';
  END IF;
END$$;

