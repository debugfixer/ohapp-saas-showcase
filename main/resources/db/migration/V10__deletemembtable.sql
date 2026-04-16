-- V10__deletemembtable.sql
-- Нормализуем membership-таблицу и переносим данные со старого имени (без created_at)

-- на всякий случай, для gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1) Новая целевая таблица (если уже есть — не трогаем)
CREATE TABLE IF NOT EXISTS public.user_tenant_memberships (
                                                              id        uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id   uuid NOT NULL REFERENCES public.user_accounts(id) ON DELETE CASCADE,
    tenant_id uuid NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    role      text NOT NULL DEFAULT 'USER',
    CONSTRAINT uq_user_tenant UNIQUE (user_id, tenant_id)
    );

-- Доп.индексы (идемпотентно)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
    WHERE c.relname='idx_utm_user' AND n.nspname='public'
  ) THEN
CREATE INDEX idx_utm_user ON public.user_tenant_memberships(user_id);
END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
    WHERE c.relname='idx_utm_tenant' AND n.nspname='public'
  ) THEN
CREATE INDEX idx_utm_tenant ON public.user_tenant_memberships(tenant_id);
END IF;
END$$;

-- 2) Перенос данных со старой таблицы, если она существует
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema='public' AND table_name='user_tenant_membership'
  ) THEN
    INSERT INTO public.user_tenant_memberships (id, user_id, tenant_id, role)
SELECT
    COALESCE(id, gen_random_uuid()),
    user_id,
    tenant_id,
    COALESCE(role, 'USER')
FROM public.user_tenant_membership
    ON CONFLICT (user_id, tenant_id) DO UPDATE
                                            SET role = EXCLUDED.role;

-- Переименуем старую таблицу в бэкап (однократно)
IF NOT EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema='public' AND table_name='user_tenant_membership_bak'
    ) THEN
ALTER TABLE public.user_tenant_membership RENAME TO user_tenant_membership_bak;
END IF;
END IF;
END$$;
