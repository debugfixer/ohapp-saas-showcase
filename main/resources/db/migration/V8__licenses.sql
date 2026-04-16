create table if not exists licenses (
                                        id uuid primary key,
                                        tenant_id uuid not null references tenants(id) on delete cascade,
    plan varchar(32) not null,
    status varchar(32) not null,
    seats int not null check (seats > 0),
    starts_at timestamp with time zone not null,
    expires_at timestamp with time zone not null,
    suspended_until timestamp with time zone,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now()
    );

create index if not exists idx_lic_tenant on licenses(tenant_id);
create index if not exists idx_lic_status on licenses(status);
