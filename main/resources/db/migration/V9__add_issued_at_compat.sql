-- V10__add_issued_at_compat.sql
-- 1) Добавляем колонку
ALTER TABLE licenses
    ADD COLUMN IF NOT EXISTS issued_at timestamp with time zone;

-- 2) Бэкфилл: приравниваем к starts_at для существующих строк
UPDATE licenses
SET issued_at = starts_at
WHERE issued_at IS NULL;

-- 3) Гарантируем наличие значения дальше
ALTER TABLE licenses
    ALTER COLUMN issued_at SET NOT NULL;

-- 4) Триггер: если при INSERT/UPDATE issued_at не передали — проставим = starts_at
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_trigger WHERE tgname = 'licenses_set_issued_at_trg'
  ) THEN
    CREATE OR REPLACE FUNCTION licenses_set_issued_at_fn()
    RETURNS trigger AS $f$
BEGIN
      IF NEW.issued_at IS NULL THEN
        NEW.issued_at := NEW.starts_at;
END IF;
RETURN NEW;
END;
    $f$ LANGUAGE plpgsql;

CREATE TRIGGER licenses_set_issued_at_trg
    BEFORE INSERT OR UPDATE ON licenses
                         FOR EACH ROW
                         EXECUTE FUNCTION licenses_set_issued_at_fn();
END IF;
END$$;
