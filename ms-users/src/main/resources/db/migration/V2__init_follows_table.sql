-- ============================================================
-- Migracion V2 del esquema heypudu_users
-- Crea la tabla follows para registrar las relaciones de
-- seguimiento entre usuarios.
-- ============================================================

CREATE TABLE follows (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    follower_id  UUID         NOT NULL,
    followed_id  UUID         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_follows_follower_followed
        UNIQUE (follower_id, followed_id),

    CONSTRAINT chk_follows_no_self
        CHECK (follower_id <> followed_id)
);

CREATE INDEX idx_follows_follower_id
    ON follows (follower_id);

CREATE INDEX idx_follows_followed_id
    ON follows (followed_id);

CREATE INDEX idx_follows_followed_created
    ON follows (followed_id, created_at DESC);

CREATE INDEX idx_follows_follower_created
    ON follows (follower_id, created_at DESC);