-- Make sure pg_trgm is available for gin_trgm_ops (allowed in a transaction)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Email search: backend uses lower(users.email) LIKE '%term%' (substring match)
--   1) GIN + trigram index accelerates ILIKE/substring queries
--   2) Additional BTREE on lower(email) helps exact matches & some planners
-- If a previous CREATE INDEX CONCURRENTLY failed, an invalid stub might exist.
-- Drop by name so we can recreate it cleanly below.
DROP INDEX IF EXISTS public.idx_users_email_trgm;

CREATE INDEX IF NOT EXISTS idx_users_email_trgm
  ON public.users
  USING gin (lower(email) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_users_email_lower_btree
  ON public.users (lower(email));

-- Orders filtering/sorting used by Admin page:
--   WHERE status = ?
--   AND created_at BETWEEN ? AND ?
--   AND total_amount BETWEEN ? AND ?
--   ORDER BY created_at DESC
-- Also join to users via orders.user_id

CREATE INDEX IF NOT EXISTS idx_orders_status
  ON public.orders (status);

CREATE INDEX IF NOT EXISTS idx_orders_created_at
  ON public.orders (created_at);

CREATE INDEX IF NOT EXISTS idx_orders_total_amount
  ON public.orders (total_amount);

-- Composite supports: WHERE status = ? ORDER BY created_at DESC
CREATE INDEX IF NOT EXISTS idx_orders_status_created_at
  ON public.orders (status, created_at DESC);

-- Ensure the join to users is cheap (FKs aren’t auto-indexed by Postgres)
CREATE INDEX IF NOT EXISTS idx_orders_user_id
  ON public.orders (user_id);
