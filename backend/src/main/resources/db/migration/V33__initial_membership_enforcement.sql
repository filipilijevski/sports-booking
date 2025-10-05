-- Guardrails for INITIAL annual club membership + helpful indexes

-- Ensure membership_plans have sane duration and status semantics
ALTER TABLE membership_plans
  ADD CONSTRAINT membership_plans_duration_check
  CHECK (duration_days IS NULL OR duration_days > 0);

-- For INITIAL plans, duration must exist
-- If you only want this for the "initial annual club membership" record, keep it app-level.
-- This generic constraint keeps all INITIAL plans time-bound.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'membership_plans_initial_requires_duration'
  ) THEN
    ALTER TABLE membership_plans
      ADD CONSTRAINT membership_plans_initial_requires_duration
      CHECK (type <> 'INITIAL' OR duration_days IS NOT NULL);
  END IF;
END$$;

-- Speed up lookups for active INITIAL membership verification
CREATE INDEX IF NOT EXISTS idx_user_memberships_active_period
  ON user_memberships (user_id, start_ts, end_ts)
  WHERE active = true;

-- Helpful index for attendance marking 
CREATE INDEX IF NOT EXISTS idx_attendance_occurrence
  ON attendance (occurrence_id);

-- Helpful index for occurrences by start time 
CREATE INDEX IF NOT EXISTS idx_program_occurrences_start
  ON program_occurrences (start_ts);

-- Make sure enrollments.version exists for optimistic locking 
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name='enrollments' AND column_name='version'
  ) THEN
    ALTER TABLE enrollments ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
  END IF;
END$$;

