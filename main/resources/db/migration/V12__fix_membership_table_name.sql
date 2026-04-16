-- V12__fix_membership_table_name.sql
-- Приводим имя таблицы к ожидаемому JPA: user_tenant_membership
-- И оставляем совместимость для возможных прямых SQL на множественном имени.

DO $$
BEGIN
  -- Если есть plural-таблица и нет singular — переименуем.
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema='public' AND table_name='user_tenant_memberships'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema='public' AND table_name='user_tenant_membership'
  ) THEN
ALTER TABLE public.user_tenant_memberships RENAME TO user_tenant_membership;
END IF;
END$$;

-- Индексы создаём идемпотентно
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
    WHERE c.relname='idx_utm_user' AND n.nspname='public'
  ) THEN
CREATE INDEX idx_utm_user ON public.user_tenant_membership(user_id);
END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
    WHERE c.relname='idx_utm_tenant' AND n.nspname='public'
  ) THEN
CREATE INDEX idx_utm_tenant ON public.user_tenant_membership(tenant_id);
END IF;
END$$;

-- Оставляем совместимость: создаём VIEW со старым названием, если его ещё нет.
-- (На случай, если где-то в коде/SQL осталась ссылка на plural-имя.)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.views
    WHERE table_schema='public' AND table_name='user_tenant_memberships'
  ) THEN
    CREATE OR REPLACE VIEW public.user_tenant_memberships AS
SELECT id, user_id, tenant_id, role
FROM public.user_tenant_membership;
END IF;
END$$;
