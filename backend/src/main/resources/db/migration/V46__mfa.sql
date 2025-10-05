-- 1) users: MFA flags and encrypted secrets (current and pending)
alter table users
  add column if not exists mfa_enabled boolean not null default false,
  add column if not exists mfa_secret_enc text,
  add column if not exists mfa_secret_tmp_enc text;

-- 2) recovery codes (hashed, one per row)
create table if not exists user_mfa_recovery_codes (
  id bigserial primary key,
  user_id bigint not null references users(id) on delete cascade,
  code_hash varchar(64) not null,
  used_at timestamp with time zone,
  created_at timestamp with time zone not null default now(),
  unique (user_id, code_hash)
);

-- 3) pre-auth sessions (hashed token; used for MFA second step)
create table if not exists pre_auth_sessions (
  id bigserial primary key,
  token_hash varchar(64) not null unique,
  user_id bigint not null references users(id) on delete cascade,
  purpose varchar(20) not null,
  attempts int not null default 0,
  expires_at timestamp with time zone not null,
  created_at timestamp with time zone not null default now()
);

create index if not exists idx_pre_auth_expires on pre_auth_sessions(expires_at);

