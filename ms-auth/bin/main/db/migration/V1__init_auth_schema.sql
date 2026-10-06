-- ============================================================
-- Migracion inicial del esquema heypudu_auth
-- Crea la tabla auth_users que registra los usuarios
-- sincronizados desde Cognito.
-- ============================================================

CREATE TABLE auth_users (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    cognito_sub  UUID         NOT NULL UNIQUE,
    email        VARCHAR(255) NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_auth_users_status
        CHECK (status IN ('ACTIVE', 'BANNED', 'DELETED'))
);

-- ============================================================
-- Indices
-- ============================================================

-- Busqueda por cognito_sub: se usa en cada login y en cada sync.
CREATE UNIQUE INDEX idx_auth_users_cognito_sub
    ON auth_users (cognito_sub);

-- Busqueda por email: util para reportes y validaciones.
CREATE INDEX idx_auth_users_email
    ON auth_users (email);

-- Filtrado por estado: util para reportes administrativos.
CREATE INDEX idx_auth_users_status
    ON auth_users (status);