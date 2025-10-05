-- Drop the old numeric constraint if it exists
ALTER TABLE schedule_templates
  DROP CONSTRAINT IF EXISTS schedule_templates_weekday_check;

-- Change INT -> VARCHAR(9) while mapping existing values
ALTER TABLE schedule_templates
  ALTER COLUMN weekday
    TYPE VARCHAR(9)
    USING (
      CASE weekday
        WHEN 0 THEN 'MONDAY'
        WHEN 1 THEN 'TUESDAY'
        WHEN 2 THEN 'WEDNESDAY'
        WHEN 3 THEN 'THURSDAY'
        WHEN 4 THEN 'FRIDAY'
        WHEN 5 THEN 'SATURDAY'
        WHEN 6 THEN 'SUNDAY'
      END
    );

-- New constraint to allow only valid names (case‑sensitive)
ALTER TABLE schedule_templates
  ADD CONSTRAINT schedule_templates_weekday_check
    CHECK (weekday IN (
      'MONDAY','TUESDAY','WEDNESDAY',
      'THURSDAY','FRIDAY','SATURDAY','SUNDAY'
    ));

