-- Upgrade roles.id and users.id from INT4 (serial) to INT8 (bigserial)
-- without breaking foreign keys.

BEGIN;

-- ROLES  (only users.role_id references it)
-- widen the referencing column first
ALTER TABLE users
    ALTER COLUMN role_id TYPE BIGINT;

-- now widen the parent PK column
ALTER TABLE roles
    ALTER COLUMN id TYPE BIGINT;

-- widen the sequence behind roles.id (optional but tidy)
ALTER SEQUENCE roles_id_seq AS BIGINT;

-- USERS  (many tables reference it)
-- widen every FK column that points to users.id
ALTER TABLE orders                   ALTER COLUMN user_id TYPE BIGINT;
ALTER TABLE schedule_templates       ALTER COLUMN coach_id TYPE BIGINT;
ALTER TABLE time_slots               ALTER COLUMN coach_id TYPE BIGINT;
ALTER TABLE lessons                  ALTER COLUMN player_id TYPE BIGINT;
ALTER TABLE tournament_registrations ALTER COLUMN player_id TYPE BIGINT;
ALTER TABLE program_enrollments      ALTER COLUMN player_id TYPE BIGINT;
ALTER TABLE forum_threads            ALTER COLUMN user_id  TYPE BIGINT;
ALTER TABLE forum_posts              ALTER COLUMN user_id  TYPE BIGINT;
ALTER TABLE refresh_tokens           ALTER COLUMN user_id  TYPE BIGINT;

-- widen the parent PK
ALTER TABLE users
    ALTER COLUMN id TYPE BIGINT;

-- widen the sequence behind users.id
ALTER SEQUENCE users_id_seq AS BIGINT;

COMMIT;

