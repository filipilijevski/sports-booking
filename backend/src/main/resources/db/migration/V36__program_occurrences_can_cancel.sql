-- Add soft-cancel (hide from calendar) flag to occurrences
ALTER TABLE program_occurrences
    ADD COLUMN IF NOT EXISTS cancelled boolean NOT NULL DEFAULT false;

-- Helpful partial index for queries that exclude cancelled
CREATE INDEX IF NOT EXISTS ix_program_occurrences_active_window
    ON program_occurrences (start_ts)
    WHERE cancelled = false;

