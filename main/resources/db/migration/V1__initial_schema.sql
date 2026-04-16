-- Расширения
create extension if not exists "uuid-ossp";

-- ===== tenants =====
create table if not exists tenants (
                                       id          uuid primary key,
                                       slug        varchar(100) not null unique,
    code        varchar(100) unique,
    name        varchar(255) not null,
    status      varchar(32)  not null default 'ACTIVE',
    created_at  timestamptz  not null default now()
    );
create index if not exists idx_tenants_slug on tenants(slug);

-- ===== user_accounts (глобальные пользователи) =====
create table if not exists user_accounts (
                                             id            uuid primary key,
                                             username      varchar(255) not null unique,
    email         varchar(255) not null unique,
    display_name  varchar(255),
    password_hash varchar(255) not null,
    role          varchar(64),
    is_active     boolean not null default true,
    created_at    timestamptz not null default now()
    );

-- ===== членство пользователя в арендаторе =====
create table if not exists user_tenant_membership (
                                                      id               uuid primary key,
                                                      user_id          uuid not null references user_accounts(id) on delete cascade,
    tenant_id        uuid not null references tenants(id)      on delete cascade,
    role             varchar(64) not null,  -- ROLE_ADMIN / ROLE_HR / ROLE_VIEWER
    department_scope jsonb not null default '{}'::jsonb,
    unique (user_id, tenant_id)
    );
create index if not exists idx_membership_user   on user_tenant_membership(user_id);
create index if not exists idx_membership_tenant on user_tenant_membership(tenant_id);
create index if not exists idx_membership_role   on user_tenant_membership(role);

-- ===== refresh_tokens (под JPA: @Id Long + IDENTITY) =====
create table if not exists refresh_tokens (
                                              id              bigserial primary key,
                                              token_hash      varchar(255) not null,
    user_account_id uuid not null,
    revoked         boolean not null default false,
    created_at      timestamptz not null default now(),
    expires_at      timestamptz not null
    );
-- FK с защитой от двойного создания
do $$
begin
  if not exists (select 1 from pg_constraint where conname = 'fk_refresh_user') then
alter table refresh_tokens
    add constraint fk_refresh_user
        foreign key (user_account_id) references user_accounts(id) on delete cascade;
end if;
end $$;
create index if not exists idx_refresh_user       on refresh_tokens(user_account_id);
create index if not exists idx_refresh_revoked    on refresh_tokens(revoked);
create index if not exists idx_refresh_expires_at on refresh_tokens(expires_at);

-- ===== доменные таблицы =====
create table if not exists employees (
                                         id            bigserial primary key,
                                         tenant_id     uuid not null references tenants(id) on delete cascade,
    tab_number    varchar(255) not null,
    full_name     varchar(255) not null,
    department    varchar(255),
    position      varchar(255),
    hire_date     date,
    birth_date    date,
    comment       text,
    required_points1y text,
    required_points2y text,
    is_active     boolean not null default true,
    created_at    timestamptz not null default now(),
    unique (tenant_id, tab_number)
    );
create index if not exists idx_employees_tenant     on employees(tenant_id);
create index if not exists idx_employees_full_name_ci on employees((lower(full_name)));

create table if not exists medical_exams (
                                             id            bigserial primary key,
                                             tenant_id     uuid   not null references tenants(id)     on delete cascade,
    employee_id   bigint not null references employees(id)   on delete cascade,
    exam_type     varchar(255) not null,
    frequency_years integer,
    exam_date     date,
    next_due_date date,                  -- ранняя версия
    next_exam_date date,                 -- обновлённое поле (оставляем оба для совместимости, можем позже мигрировать)
    referral_date date,
    points        text,
    created_at    timestamptz not null default now()
    );
create index if not exists idx_medical_exams_tenant on medical_exams(tenant_id);
create index if not exists idx_medical_exams_due    on medical_exams(tenant_id, next_due_date);
create index if not exists idx_medical_exams_next_exam_date on medical_exams(next_exam_date);
create index if not exists idx_exam_type            on medical_exams(exam_type);
create index if not exists idx_initial_employee_id  on medical_exams(employee_id);

-- ===== аудит медосмотров =====
create table if not exists medical_exam_audit (
                                                  id           bigserial primary key,
                                                  employee_id  bigint not null,
                                                  exam_type    varchar(64),
    changed_at   timestamptz not null default now(),
    changed_by   varchar(128),
    change_type  varchar(64),
    before_json  text,
    after_json   text,
    comment      text
    );
