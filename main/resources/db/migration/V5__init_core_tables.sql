-- PostgreSQL / Flyway. Идемпотентные создания там, где это возможно.

-- =========================
-- tenants
-- =========================
CREATE TABLE IF NOT EXISTS tenants (
                                       id         UUID PRIMARY KEY,
                                       slug       VARCHAR(255) NOT NULL UNIQUE,
    name       VARCHAR(255) NOT NULL,
    status     VARCHAR(50)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL
    );

-- =========================
-- user_accounts
-- =========================
CREATE TABLE IF NOT EXISTS user_accounts (
                                             id            UUID PRIMARY KEY,
                                             username      VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    display_name  VARCHAR(255),
    role          VARCHAR(100),
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL
    );

-- =========================
-- user_tenant_memberships
-- =========================
CREATE TABLE IF NOT EXISTS user_tenant_memberships (
                                                       id               UUID PRIMARY KEY,
                                                       user_id          UUID NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
    tenant_id        UUID NOT NULL REFERENCES tenants(id)       ON DELETE CASCADE,
    role             VARCHAR(100) NOT NULL,
    department_scope TEXT NOT NULL DEFAULT '{}'
    );

-- уникальность членства пользователя в одном tenant
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_user_tenant_unique'
    ) THEN
ALTER TABLE user_tenant_memberships
    ADD CONSTRAINT uk_user_tenant_unique UNIQUE (user_id, tenant_id);
END IF;
END
$$;

-- =========================
-- password_reset_tokens
-- =========================
CREATE TABLE IF NOT EXISTS password_reset_tokens (
                                                     id               BIGSERIAL    PRIMARY KEY,
                                                     user_account_id  UUID         NOT NULL,
                                                     token_hash       VARCHAR(255) NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    expires_at       TIMESTAMPTZ,
    used_at          TIMESTAMPTZ
    );

-- FK на пользователя (идемпотентно)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_prt_user_account'
          AND table_name = 'password_reset_tokens'
    ) THEN
ALTER TABLE password_reset_tokens
    ADD CONSTRAINT fk_prt_user_account
        FOREIGN KEY (user_account_id)
            REFERENCES user_accounts(id)
            ON DELETE CASCADE;
END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_prt_user ON password_reset_tokens(user_account_id);

-- =========================
-- refresh_tokens
-- =========================
CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              id         UUID PRIMARY KEY,
    -- ожидаемая колонка user_id (FK на user_accounts)
                                              user_id    UUID NOT NULL,
                                              token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
    );

-- Приводим refresh_tokens к ожидаемой схеме (user_id, FK, индекс)
DO $$
BEGIN
    -- Если есть старая колонка user_account_id, а новой еще нет — переименуем
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'refresh_tokens' AND column_name = 'user_account_id'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'refresh_tokens' AND column_name = 'user_id'
    ) THEN
        EXECUTE 'ALTER TABLE refresh_tokens RENAME COLUMN user_account_id TO user_id';
END IF;

    -- Если колонки user_id нет вовсе — добавим (nullable, чтобы миграция прошла без данных)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'refresh_tokens' AND column_name = 'user_id'
    ) THEN
        EXECUTE 'ALTER TABLE refresh_tokens ADD COLUMN user_id UUID';
END IF;

    -- Убедимся, что существует внешний ключ на user_accounts(id)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'refresh_tokens' AND column_name = 'user_id'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON tc.constraint_name = kcu.constraint_name
        WHERE tc.table_name = 'refresh_tokens'
          AND tc.constraint_type = 'FOREIGN KEY'
          AND kcu.column_name = 'user_id'
    ) THEN
        EXECUTE 'ALTER TABLE refresh_tokens
                 ADD CONSTRAINT fk_refresh_tokens_user
                 FOREIGN KEY (user_id) REFERENCES user_accounts(id) ON DELETE CASCADE';
END IF;

    -- Индекс по user_id создаем только если колонка есть
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'refresh_tokens' AND column_name = 'user_id'
    ) THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_rt_user ON refresh_tokens(user_id)';
END IF;
END
$$;
