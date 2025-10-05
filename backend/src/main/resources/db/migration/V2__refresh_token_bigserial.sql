-- Upgrade refresh_tokens.id from 32‑bit serial to 64‑bit bigserial
ALTER TABLE refresh_tokens
    ALTER COLUMN id TYPE BIGINT,
    ALTER COLUMN id SET NOT NULL;


