-- add an optional sort_order column to program_packages
ALTER TABLE program_packages
    ADD COLUMN IF NOT EXISTS sort_order integer NULL;

UPDATE program_packages SET sort_order = 0 WHERE sort_order IS NULL;

-- helpful index for program_id + sort_order queries
CREATE INDEX IF NOT EXISTS ix_program_packages_program_sort
    ON program_packages (program_id, sort_order NULLS LAST, id);

