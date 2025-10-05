-- Table rental packages + purchases + admin consumptions

create table if not exists table_rental_packages (
    id              bigserial primary key,
    name            varchar(120) not null,
    hours           numeric(6,2) not null check (hours > 0),
    price_cad       numeric(10,2) not null check (price_cad >= 0),
    active          boolean not null default true,
    sort_order      integer,
    created_at      timestamptz not null default now()
);

create index if not exists ix_trpkg_active on table_rental_packages (active);

create table if not exists table_rental_purchases (
    id              bigserial primary key,
    user_id         bigint not null references users(id),
    package_id      bigint not null references table_rental_packages(id),
    status          varchar(16) not null,
    price_cad       numeric(10,2) not null,
    tax_cad         numeric(10,2) not null,
    total_cad       numeric(10,2) not null,
    currency        varchar(10) not null,
    stripe_payment_intent_id varchar(100),
    created_at      timestamptz not null default now()
);

create index if not exists ix_trp_user on table_rental_purchases(user_id);
create index if not exists ix_trp_pkg  on table_rental_purchases(package_id);
create index if not exists ix_trp_pi   on table_rental_purchases(stripe_payment_intent_id);

create table if not exists table_rental_consumptions (
    id              bigserial primary key,
    user_id         bigint not null references users(id),
    consumed_by     bigint not null references users(id),
    trc_id          bigint references table_rental_credits(id),
    group_id        bigint references membership_groups(id),
    amount_hours    numeric(6,2) not null check (amount_hours > 0),
    created_at      timestamptz not null default now()
);

create index if not exists ix_trcons_user  on table_rental_consumptions(user_id);
create index if not exists ix_trcons_admin on table_rental_consumptions(consumed_by);
create index if not exists ix_trcons_trc   on table_rental_consumptions(trc_id);

