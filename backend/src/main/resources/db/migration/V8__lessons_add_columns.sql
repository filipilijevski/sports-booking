/* V8__lessons_add_columns.sql
   Adds scheduling columns + coach FK for Lesson.java */

-- 1  start_time
ALTER TABLE lessons
    ADD COLUMN IF NOT EXISTS start_time TIMESTAMP NOT NULL DEFAULT NOW();

-- 2  end_time
ALTER TABLE lessons
    ADD COLUMN IF NOT EXISTS end_time TIMESTAMP NOT NULL DEFAULT NOW();

-- 3  status
ALTER TABLE lessons
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'BOOKED';

-- 4  coach_id to users(id)
ALTER TABLE lessons
    ADD COLUMN IF NOT EXISTS coach_id BIGINT NOT NULL;

--  !no IF NOT EXISTS here (not supported before PG‑16)
DO $$
BEGIN
  IF NOT EXISTS (
        SELECT 1
        FROM   pg_constraint
        WHERE  conname = 'fk_lessons_coach') THEN
    ALTER TABLE lessons
      ADD CONSTRAINT fk_lessons_coach
        FOREIGN KEY (coach_id) REFERENCES users(id);
  END IF;
END$$;

-- 5  drop the temporary defaults
ALTER TABLE lessons
    ALTER COLUMN start_time DROP DEFAULT;

ALTER TABLE lessons
    ALTER COLUMN end_time   DROP DEFAULT;

