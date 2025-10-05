-- Safe, additive indexes to speed up admin reads and attendance paths.
-- No schema changes; guarded with IF NOT EXISTS.

-- 1) Calendar feeds: range scans by time and by (program_id, time)
CREATE INDEX IF NOT EXISTS ix_po_start_active
  ON program_occurrences (start_ts)
  WHERE cancelled = false;

CREATE INDEX IF NOT EXISTS ix_po_program_start_active
  ON program_occurrences (program_id, start_ts)
  WHERE cancelled = false;

-- 2) Eligibility list for attendance: (program_id, status, sessions_remaining)
CREATE INDEX IF NOT EXISTS ix_upe_prog_status_remaining
  ON user_program_enrollments (program_id, status, sessions_remaining);

-- 3) Common FK lookups used by admin lists
CREATE INDEX IF NOT EXISTS ix_pp_program ON program_packages (program_id);
CREATE INDEX IF NOT EXISTS ix_ps_program ON program_slots (program_id);

-- 4) Active membership lookups (user_id + active window)
CREATE INDEX IF NOT EXISTS ix_um_user_active_window
  ON user_memberships (user_id, active, start_ts, end_ts);

