-- V6__fix_refresh_tokens_id_to_uuid.sql
-- Приводим refresh_tokens.id к UUID (совместимо с текущими @Entity)

-- UUID-генератор (pgcrypto) — включаем, если ещё не включён
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Если колонка id уже UUID — ничего не делаем
DO $$
DECLARE
col_type text;
BEGIN
SELECT data_type
INTO col_type
FROM information_schema.columns
WHERE table_name = 'refresh_tokens' AND column_name = 'id';

IF col_type IS NULL THEN
        RAISE EXCEPTION 'Table refresh_tokens or column id not found';
END IF;

    -- information_schema для uuid вернёт 'uuid', для bigserial/bigint — 'bigint'
    IF col_type <> 'uuid' THEN
        -- Снимаем PK (если есть)
ALTER TABLE refresh_tokens DROP CONSTRAINT IF EXISTS refresh_tokens_pkey;

-- Старую колонку переименуем, чтобы не потерять данные до апдейта
ALTER TABLE refresh_tokens RENAME COLUMN id TO id_old;

-- Добавляем UUID-колонку
ALTER TABLE refresh_tokens ADD COLUMN id UUID;

-- Проставляем UUID каждому ряду
UPDATE refresh_tokens SET id = gen_random_uuid() WHERE id IS NULL;

-- Делаем not null и PK
ALTER TABLE refresh_tokens ALTER COLUMN id SET NOT NULL;
ALTER TABLE refresh_tokens ADD PRIMARY KEY (id);

-- Удаляем старую колонку
ALTER TABLE refresh_tokens DROP COLUMN id_old;

-- Индекс по user_id оставляем как есть,
-- на всякий случай создадим идемпотентно
CREATE INDEX IF NOT EXISTS idx_rt_user ON refresh_tokens(user_id);
END IF;
END
$$;

-- Чистим возможную «сиротскую» sequence от BIGSERIAL
DROP SEQUENCE IF EXISTS refresh_tokens_id_seq;
