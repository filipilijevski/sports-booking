
-- 1. Coupons  (flat-$ / %-off, unique code, hard expiry)
create table coupons
(
    id            bigserial primary key,
    code          varchar(32)  not null,
    percent_off   numeric(5,4),      -- 0.1500 becomes 15 %
    amount_off    numeric(10,2),     -- flat $ off (CAD)
    min_spend     numeric(10,2),     -- nullable then no minimum
    expires_at    timestamptz not null,

    constraint uq_coupons_code unique (code),

    /* either %-off or $-off but never both / neither */
    constraint ck_coupon_exactly_one_type
        check ( (percent_off is not null)::int +
                (amount_off  is not null)::int = 1 ),

    /* sane ranges */
    constraint ck_coupon_pct_range
        check ( percent_off is null
             or (percent_off >= 0 and percent_off <= 1) ),

    constraint ck_coupon_amount_range
        check ( amount_off is null
             or amount_off >= 0 )
);

comment on column coupons.percent_off is '0.15 means 15 %';
comment on column coupons.amount_off  is 'Flat CAD dollars off';

-- 2. Orders - new monetary & linkage columns + shipping method
alter table orders
    add column discount_amount  numeric(10,2) not null default 0,
    add column coupon_id        bigint,
    add column shipping_method  varchar(16)   not null default 'REGULAR';

-- FK to coupons  (on delete set null - keeps historic data even
-- if the admin deletes an expired coupon later)
alter table orders
    add constraint fk_orders_coupon
        foreign key (coupon_id)
        references coupons(id)
        on delete set null;

-- Soft enum for shipping options (keeps flexibility / avoids
-- hard-coding a second PG enum).  Adds a CHECK so typos are impossible.
alter table orders
    add constraint ck_orders_shipping_method
        check (shipping_method in ('REGULAR', 'EXPRESS'));

-- 3. Helpful secondary indexes (optional but recommended)
create index if not exists idx_coupons_expires_at  on coupons (expires_at);
create index if not exists idx_orders_created_at   on orders  (created_at);

