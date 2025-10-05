-- Aligns schedule_templates.id with the Long id in ScheduleTemplate.java

DO $$
BEGIN
  -- Only run if the column is still INT4
  IF EXISTS (
        SELECT 1
        FROM   pg_attribute a
        JOIN   pg_class     c ON c.oid = a.attrelid
        WHERE  c.relname = 'schedule_templates'
          AND  a.attname = 'id'
          AND  a.atttypid = 'int4'::regtype) THEN

      -- 1  widen the column to BIGINT
      ALTER TABLE schedule_templates
             ALTER COLUMN id TYPE BIGINT;

      -- 2  make sure its sequence can hold BIGINT
      ALTER SEQUENCE schedule_templates_id_seq AS BIGINT;
  END IF;
END$$;

