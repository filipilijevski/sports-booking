-- Add temporary password fields and soft-delete to users
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS temp_password_hash        VARCHAR(255),
    ADD COLUMN IF NOT EXISTS temp_password_expires_at  TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deleted_at                TIMESTAMPTZ;

-- Propagate pwd_change_required across MFA pre-auth
ALTER TABLE pre_auth_sessions
    ADD COLUMN IF NOT EXISTS pwd_change_required BOOLEAN NOT NULL DEFAULT FALSE;

