-- Create a unique expression index on lower(btrim(email))
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_normalized
    ON users (LOWER(BTRIM(email)));

