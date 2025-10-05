ALTER TABLE users
  ADD COLUMN provider       varchar(16) NOT NULL DEFAULT 'LOCAL',
  ADD COLUMN provider_sub   varchar(128);
CREATE UNIQUE INDEX uq_user_provider_sub
  ON users(provider, provider_sub)
  WHERE provider <> 'LOCAL';

