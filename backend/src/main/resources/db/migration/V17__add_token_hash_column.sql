
-- for existing rows (dev only) copy uuid() so they validate;
-- in prod would drop existing rows anyway
UPDATE refresh_tokens
    SET token_hash = md5(random()::text); -- any 32-char placeholder

CREATE UNIQUE INDEX refresh_tokens_token_hash_idx
    ON refresh_tokens(token_hash);

