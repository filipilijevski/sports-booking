--  Harmonise "lessons.id" with other tables (bigserial -> BIGINT)

-- 1) drop PK so we can change the column type
ALTER TABLE lessons DROP CONSTRAINT lessons_pkey;

-- 2) make the existing sequence BIGINT‑aware (otherwise nextval() will overflow)
ALTER SEQUENCE lessons_id_seq AS BIGINT;

-- 3) convert the column to BIGINT and re‑attach the default
ALTER TABLE lessons
    ALTER COLUMN id TYPE BIGINT,
    ALTER COLUMN id SET DEFAULT nextval('lessons_id_seq');

-- 4) recreate the primary key
ALTER TABLE lessons ADD PRIMARY KEY (id);

