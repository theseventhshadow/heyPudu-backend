-- ============================================================
-- Migracion V1 del esquema heypudu_users
-- Crea la tabla users con el perfil de cada usuario.
-- ============================================================

CREATE TABLE users (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    cognito_sub           UUID         NOT NULL UNIQUE,
    name                  VARCHAR(100) NOT NULL,
    username              VARCHAR(50)  NOT NULL UNIQUE,
    bio                   VARCHAR(300),
    avatar_key            VARCHAR(500),
    is_verified           BOOLEAN      NOT NULL DEFAULT FALSE,
    status                VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    follower_count        INTEGER      NOT NULL DEFAULT 0,
    following_count       INTEGER      NOT NULL DEFAULT 0,
    post_count            INTEGER      NOT NULL DEFAULT 0,
    username_updated_at   TIMESTAMPTZ,
    anonymized_at         TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_users_status
        CHECK (status IN ('ACTIVE', 'BANNED', 'DELETED')),

    CONSTRAINT chk_users_counters_non_negative
        CHECK (follower_count >= 0 AND following_count >= 0 AND post_count >= 0),

    CONSTRAINT chk_users_username_format
        CHECK (username ~ '^[a-zA-Z0-9._]+$')
);

CREATE INDEX idx_users_status
    ON users (status);

CREATE INDEX idx_users_is_verified
    ON users (is_verified);

CREATE INDEX idx_users_name_lower
    ON users (LOWER(name));

CREATE INDEX idx_users_username_lower
    ON users (LOWER(username));