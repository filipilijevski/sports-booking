-- status column 
ALTER TABLE time_slots
  ADD COLUMN IF NOT EXISTS status VARCHAR(20)
  NOT NULL DEFAULT 'FREE';

-- (optional) enum-like guard
ALTER TABLE time_slots
  ADD CONSTRAINT chk_time_slots_status
  CHECK (status IN ('FREE','HELD','BOOKED','CLOSED'));

ALTER TABLE time_slots
  RENAME COLUMN start_ts TO start_time;
  
ALTER TABLE time_slots
  RENAME COLUMN end_ts TO end_time;

