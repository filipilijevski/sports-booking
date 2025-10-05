-- Creates blog posts and images tables
-- Compatible with PostgreSQL; aligns with existing naming conventions.

create table if not exists blog_posts (
  id               bigserial primary key,
  title            varchar(200) not null,
  subtitle         varchar(300),
  body_markdown    text not null,
  main_image_url   varchar(512),
  main_image_alt   varchar(255),
  sort_order       smallint not null default 0,
  visible          boolean not null default true,
  created_at       timestamptz not null default now(),
  updated_at       timestamptz not null default now(),
  deleted_at       timestamptz
);

create table if not exists blog_images (
  id          bigserial primary key,
  post_id     bigint not null references blog_posts(id) on delete cascade,
  url         varchar(512) not null,
  alt_text    varchar(255),
  sort_order  smallint not null default 0
);

-- Query performance: public lists by visibility + deletion + order
create index if not exists idx_blog_posts_public
  on blog_posts (visible, deleted_at, sort_order, created_at desc);

create index if not exists idx_blog_images_post
  on blog_images (post_id, sort_order);

-- Trigger to keep updated_at in sync (optional but handy)
do $$
begin
  if not exists (select 1 from pg_proc where proname = 'blog_touch_updated_at') then
    create or replace function blog_touch_updated_at()
    returns trigger
    language plpgsql as $fn$
    begin
      new.updated_at = now();
      return new;
    end;
    $fn$;
  end if;
end $$;

do $$
begin
  if not exists (
    select 1 from pg_trigger
    where tgname = 'trg_blog_posts_touch_updated_at'
  ) then
    create trigger trg_blog_posts_touch_updated_at
      before update on blog_posts
      for each row execute function blog_touch_updated_at();
  end if;
end $$;

