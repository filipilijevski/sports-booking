-- 1) programs  
create table if not exists programs (
    id          bigserial primary key,
    title       varchar(255) not null,
    description text,
    active      boolean not null default true,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

-- 2) program_slots  
-- Assign weekly occurrences (weekday + time) and coach responsible
create table if not exists program_slots (
    id          bigserial primary key,
    program_id  bigint not null references programs(id) on delete cascade,
    weekday     varchar(9) not null,
    start_time  time not null,
    end_time    time not null,
    coach_id    bigint not null references users(id),
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    constraint chk_slot_weekday
        check (weekday in ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
    constraint chk_slot_time
        check (end_time > start_time)
);
create index if not exists ix_program_slots_program on program_slots(program_id);
create index if not exists ix_program_slots_coach on program_slots(coach_id);

-- 3) program_packages  
-- Up to 5 per program (enforced at service layer)
create table if not exists program_packages (
    id              bigserial primary key,
    program_id      bigint not null references programs(id) on delete cascade,
    name            varchar(80) not null,
    sessions_count  integer not null,
    price_cad       numeric(10,2) not null,
    active          boolean not null default true,
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now(),
    constraint chk_pkg_sessions check (sessions_count > 0),
    constraint chk_pkg_price    check (price_cad >= 0)
);
create unique index if not exists uq_program_packages_name
    on program_packages(program_id, name);

-- 4) program_occurrences  
-- Materialized dates from program_slots; can exist even if slot is later edited
create table if not exists program_occurrences (
    id          bigserial primary key,
    program_id  bigint not null references programs(id) on delete cascade,
    slot_id     bigint references program_slots(id) on delete set null,
    start_ts    timestamptz not null,
    end_ts      timestamptz not null,
    coach_id    bigint not null references users(id),
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    constraint chk_occ_times check (end_ts > start_ts)
);
create unique index if not exists uq_program_occurrence
    on program_occurrences(program_id, start_ts);
create index if not exists ix_program_occurrences_start
    on program_occurrences(start_ts);
create index if not exists ix_program_occurrences_coach
    on program_occurrences(coach_id);

-- 5) enrollments  
-- One ACTIVE enrollment per (user, program) enforced by partial unique index
create table if not exists enrollments (
    id                  bigserial primary key,
    user_id             bigint not null references users(id),
    program_id          bigint not null references programs(id) on delete cascade,
    package_id          bigint not null references program_packages(id),
    sessions_remaining  integer not null,
    status              varchar(20) not null,
    source              varchar(20) not null,
    version             integer not null default 0,
    created_at          timestamptz not null default now(),
    constraint chk_enroll_sessions check (sessions_remaining >= 0),
    constraint chk_enroll_status check (status in ('ACTIVE','COMPLETED','EXPIRED')),
    constraint chk_enroll_source check (source in ('ONLINE','IN_PERSON'))
);
create index if not exists ix_enroll_user on enrollments(user_id);
create index if not exists ix_enroll_program on enrollments(program_id);
create unique index if not exists uq_enroll_active_per_program
    on enrollments(user_id, program_id)
    where status = 'ACTIVE';

-- 6) membership_plans    
create table if not exists membership_plans (
    id             bigserial primary key,
    type           varchar(20) not null,
    name           varchar(120) not null,
    price_cad      numeric(10,2) not null,
    duration_days  integer not null,
    active         boolean not null default true,
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now(),
    constraint chk_mp_type    check (type in ('INITIAL','SPECIAL')),
    constraint chk_mp_price   check (price_cad >= 0),
    constraint chk_mp_days    check (duration_days > 0)
);
create unique index if not exists uq_membership_plan_type_name
    on membership_plans(type, name);

-- 7) membership_entitlements  
create table if not exists membership_entitlements (
    id        bigserial primary key,
    plan_id   bigint not null references membership_plans(id) on delete cascade,
    kind      varchar(32) not null,
    amount    numeric(10,2) not null,
    created_at timestamptz not null default now(),
    constraint chk_me_kind check (kind in ('TABLE_HOURS','PROGRAM_CREDITS','TOURNAMENT_ENTRIES')),
    constraint chk_me_amount check (amount >= 0)
);
create index if not exists ix_me_plan on membership_entitlements(plan_id);

-- 8) user_memberships  
create table if not exists user_memberships (
    id         bigserial primary key,
    user_id    bigint not null references users(id),
    plan_id    bigint not null references membership_plans(id),
    start_ts   timestamptz not null,
    end_ts     timestamptz not null,
    active     boolean not null,
    created_at timestamptz not null default now()
);
create index if not exists ix_um_user on user_memberships(user_id);
create index if not exists ix_um_plan on user_memberships(plan_id);

-- 8b) user_membership_counters (consumed per entitlement kind)  
create table if not exists user_membership_counters (
    id                 bigserial primary key,
    user_membership_id bigint not null references user_memberships(id) on delete cascade,
    kind               varchar(32) not null,
    amount_consumed    numeric(10,2) not null default 0,
    constraint chk_umc_kind check (kind in ('TABLE_HOURS','PROGRAM_CREDITS','TOURNAMENT_ENTRIES')),
    constraint chk_umc_amount check (amount_consumed >= 0)
);
create unique index if not exists uq_umc_by_kind
    on user_membership_counters(user_membership_id, kind);

-- 9) attendance  
create table if not exists attendance (
    id             bigserial primary key,
    occurrence_id  bigint not null references program_occurrences(id) on delete cascade,
    user_id        bigint not null references users(id),
    enrollment_id  bigint references enrollments(id),
    marked_by      bigint not null references users(id),
    marked_at      timestamptz not null default now()
);
create unique index if not exists uq_attendance_once
    on attendance(occurrence_id, user_id);
create index if not exists ix_att_occ on attendance(occurrence_id);
create index if not exists ix_att_user on attendance(user_id);

-- 10) table_rental_credits  
create table if not exists table_rental_credits (
    id              bigserial primary key,
    user_id         bigint not null references users(id),
    hours_remaining numeric(6,2) not null,
    source_plan_id  bigint references membership_plans(id),
    created_at      timestamptz not null default now(),
    constraint chk_trc_hours check (hours_remaining >= 0)
);
create index if not exists ix_trc_user on table_rental_credits(user_id);

-- Housekeeping comments (not enforced):
-- Service layer will cap program_packages to <=5 per program.
-- Enforcing exactly-one ACTIVE INITIAL membership per user can be added later if needed.

