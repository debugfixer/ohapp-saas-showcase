do $$
declare
t_id uuid := uuid_generate_v4();
  u_id uuid := uuid_generate_v4();
begin
  if not exists (select 1 from tenants where slug = 'demo') then
    insert into tenants(id, slug, name, status) values (t_id, 'demo', 'Demo Tenant', 'ACTIVE');
else
select id into t_id from tenants where slug = 'demo';
end if;

  if not exists (select 1 from user_accounts where email = 'admin@demo.local') then
    insert into user_accounts(id, username, email, display_name, password_hash, role, is_active, created_at)
    values (
      u_id, 'admin', 'admin@demo.local', 'Demo Admin',
      '{bcrypt-will-be-overwritten}', 'ROLE_ADMIN_PLATFORM', true, now()
    );
else
select id into u_id from user_accounts where email = 'admin@demo.local';
end if;

  if not exists (select 1 from user_tenant_membership where user_id = u_id and tenant_id = t_id) then
    insert into user_tenant_membership(id, user_id, tenant_id, role)
      values (uuid_generate_v4(), u_id, t_id, 'ROLE_ADMIN');
end if;
end $$;
