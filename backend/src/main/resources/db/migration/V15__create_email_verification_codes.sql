CREATE TABLE IF NOT EXISTS email_verification_codes (
    id         BIGSERIAL PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    code       CHAR(6)      NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

COMMENT ON TABLE  email_verification_codes IS 'Temporary 6-digit codes for registration e-mail verification';
COMMENT ON COLUMN email_verification_codes.code       IS '6-digit numeric code stored as CHAR(6)';
COMMENT ON COLUMN email_verification_codes.expires_at IS 'Auto-expire after ~10 minutes';

