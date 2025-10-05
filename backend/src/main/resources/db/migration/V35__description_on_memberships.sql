-- Adds an optional description to membership plans.
-- Safe, idempotent-ish migration (only adds a nullable column).

ALTER TABLE membership_plans
    ADD COLUMN IF NOT EXISTS description text;

