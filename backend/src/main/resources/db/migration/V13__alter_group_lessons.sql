-- Adjust existing table instead of creating a new one
BEGIN;

/* ── rename current title column so code can use getTitle() */
ALTER TABLE group_lessons
    RENAME COLUMN name TO title;

/* ── add missing business columns */
ALTER TABLE group_lessons
    ADD COLUMN coach_id      BIGINT NOT NULL REFERENCES users(id),
    ADD COLUMN price         NUMERIC(10,2) NOT NULL DEFAULT 0  CHECK (price >= 0),
    ADD COLUMN status        VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN version       INTEGER       NOT NULL DEFAULT 0,
    ADD COLUMN created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    ADD COLUMN updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now();

/* optional helper index when you’ll query by coach + start date later  */
CREATE INDEX IF NOT EXISTS idx_group_lessons_coach ON group_lessons(coach_id);

COMMIT;

