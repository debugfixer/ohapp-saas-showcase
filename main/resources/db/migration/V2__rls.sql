-- RLS включаем на «мульти-tenant» таблицах
alter table employees     enable row level security;
alter table medical_exams enable row level security;

-- Текущий tenant берём из параметра сеанса 'app.tenant_id' (uuid строкой)
create policy p_employees_tenant on employees
  using (tenant_id = current_setting('app.tenant_id', true)::uuid)
  with check (tenant_id = current_setting('app.tenant_id', true)::uuid);

create policy p_medical_exams_tenant on medical_exams
  using (tenant_id = current_setting('app.tenant_id', true)::uuid)
  with check (tenant_id = current_setting('app.tenant_id', true)::uuid);
