-- Add optimistic locking to user_program_enrollments (safe default)
ALTER TABLE user_program_enrollments
  ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Speed up attendance lookups
CREATE INDEX IF NOT EXISTS ix_attendance_occurrence ON attendance(occurrence_id);

-- Ensure fast eligible user scans per program/status/remaining
CREATE INDEX IF NOT EXISTS ix_upe_program_status_remaining
  ON user_program_enrollments(program_id, status, sessions_remaining);

-- Defensive uniqueness for (occurrence_id, user_id) even if constraint already exists
CREATE UNIQUE INDEX IF NOT EXISTS uq_attendance_once_idx
  ON attendance(occurrence_id, user_id);

