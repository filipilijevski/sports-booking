-- Convert every primary‑key column that is still INT/SERIAL to BIGINT.
-- (Only does it once - safe to re‑run due to the IF checks)

DO $$
DECLARE
    rec RECORD;
BEGIN
    -- Find candidate columns in *public* schema
    FOR rec IN
        SELECT table_name, column_name,
               pg_get_serial_sequence(format('%I.%I', table_schema, table_name), column_name) AS seq_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND column_default LIKE 'nextval(%'            -- serial / bigserial
          AND data_type = 'integer'                      -- still 32‑bit
    LOOP
        RAISE NOTICE 'Converting %.% to BIGINT', rec.table_name, rec.column_name;

        -- 1. widen the column
        EXECUTE format(
            'ALTER TABLE public.%I ALTER COLUMN %I TYPE BIGINT',
            rec.table_name, rec.column_name);

        -- 2. widen the underlying sequence (needed for > 2^31 ids)
        IF rec.seq_name IS NOT NULL THEN
            EXECUTE format('ALTER SEQUENCE %s AS BIGINT', rec.seq_name);
        END IF;
    END LOOP;
END$$;

