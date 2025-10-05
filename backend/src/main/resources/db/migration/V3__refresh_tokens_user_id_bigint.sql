-- 1) drop the old FK constraint
ALTER TABLE refresh_tokens
  DROP CONSTRAINT IF EXISTS refresh_tokens_user_id_fkey;

-- 2) change the column’s type to BIGINT
ALTER TABLE refresh_tokens
  ALTER COLUMN user_id TYPE BIGINT
    USING user_id::BIGINT,
  ALTER COLUMN user_id SET NOT NULL;

-- 3) re-create the foreign-key against users(id)
ALTER TABLE refresh_tokens
  ADD CONSTRAINT refresh_tokens_user_id_fkey
  FOREIGN KEY (user_id) REFERENCES users(id);

