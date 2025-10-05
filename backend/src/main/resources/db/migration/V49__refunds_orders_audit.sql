-- 1) Product audit log table
create table if not exists product_audit (
  id              bigserial primary key,
  action          varchar(16) not null,        -- CREATE / UPDATE / DELETE / STOCK / IMAGE
  product_id      bigint,
  actor_user_id   bigint,
  sku             varchar(64),
  name            varchar(255),
  price           numeric(10,2),
  inventory_qty   integer,
  brand           varchar(64),
  grams           integer,
  category_id     bigint,
  category_name   varchar(100),
  image_modified  boolean not null default false,
  details_json    text,
  created_at      timestamptz not null default now()
);

-- optional: keep audit rows even if users/products are removed
do $$
begin
  if not exists (
      select 1 from pg_constraint
       where conname = 'fk_product_audit_actor_user'
  ) then
    alter table product_audit
      add constraint fk_product_audit_actor_user
      foreign key (actor_user_id) references users(id) on delete set null;
  end if;
end$$;

create index if not exists idx_product_audit_created_at on product_audit (created_at desc);
create index if not exists idx_product_audit_action     on product_audit (action);

-- 2) Allow guest orders: user_id nullable (if not already)
do $$
begin
  if exists (
    select 1 from information_schema.columns
     where table_name = 'orders'
       and column_name = 'user_id'
       and is_nullable = 'NO'
  ) then
    alter table orders alter column user_id drop not null;
  end if;
end$$;

-- 3) Keep order items alive if product deleted: product_id nullable (if not already)
do $$
begin
  if exists (
    select 1 from information_schema.columns
     where table_name = 'order_items'
       and column_name = 'product_id'
       and is_nullable = 'NO'
  ) then
    alter table order_items alter column product_id drop not null;
  end if;
end$$;


-- 5) Helpful search indexes for admin order search on guest orders
create index if not exists idx_orders_shipping_email_lower on orders (lower(shipping_email));
create index if not exists idx_orders_shipping_name_lower  on orders (lower(shipping_full_name));

