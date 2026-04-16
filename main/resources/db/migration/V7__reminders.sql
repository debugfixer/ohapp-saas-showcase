-- Напоминания/уведомления

-- ENUM-типы (в PostgreSQL нет IF NOT EXISTS для CREATE TYPE)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'notification_channel') THEN
CREATE TYPE notification_channel AS ENUM ('EMAIL','SMS','WHATSAPP');
END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'notification_status') THEN
CREATE TYPE notification_status AS ENUM ('PENDING','SENT','FAILED','CANCELLED');
END IF;
END$$;

-- Таблица reminders
CREATE TABLE IF NOT EXISTS reminders (
                                         id           uuid PRIMARY KEY,
                                         tenant_id    uuid,
                                         user_id      uuid,
                                         channel      notification_channel NOT NULL,
                                         recipient    varchar(255)         NOT NULL,   -- email или телефон (E.164)
    subject      varchar(255),                    -- для email (необязательно)
    message      text                  NOT NULL,  -- тело сообщения
    send_at      timestamptz,                     -- одиночный запуск
    cron_expr    varchar(120),                    -- CRON для повторений (вместо send_at)
    next_run_at  timestamptz,                     -- вычисленное след. время запуска
    status       notification_status   NOT NULL DEFAULT 'PENDING',
    attempts     int                   NOT NULL DEFAULT 0,
    last_error   text,
    created_at   timestamptz           NOT NULL DEFAULT now(),
    updated_at   timestamptz           NOT NULL DEFAULT now()
    );

-- Индексы (идемпотентно)
CREATE INDEX IF NOT EXISTS idx_rem_next_run ON reminders(next_run_at);
CREATE INDEX IF NOT EXISTS idx_rem_status   ON reminders(status);
CREATE INDEX IF NOT EXISTS idx_rem_channel  ON reminders(channel);

-- Функция и триггер для авто-обновления updated_at (идемпотентно)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_proc WHERE proname = 'reminders_set_updated_at'
    ) THEN
CREATE FUNCTION reminders_set_updated_at() RETURNS trigger AS $BODY$
BEGIN
            NEW.updated_at := now();
RETURN NEW;
END;
        $BODY$ LANGUAGE plpgsql;
END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trg_reminders_set_updated_at'
    ) THEN
CREATE TRIGGER trg_reminders_set_updated_at
    BEFORE UPDATE ON reminders
    FOR EACH ROW
    EXECUTE FUNCTION reminders_set_updated_at();
END IF;
END$$;
