-- ============================================================
-- Migracion inicial del esquema heypudu_posts
-- Crea la tabla posts que almacena publicaciones de texto
-- y audio (briefcasts) con portada opcional.
-- ============================================================

CREATE TABLE posts (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id                UUID         NOT NULL,
    author_username          VARCHAR(50)  NOT NULL,
    author_avatar_url        VARCHAR(500),
    type                     VARCHAR(20)  NOT NULL,
    title                    VARCHAR(50)  NOT NULL,
    content                  TEXT,
    audio_key                VARCHAR(500),
    audio_duration_seconds   INTEGER,
    cover_image_key          VARCHAR(500),
    like_count               INTEGER      NOT NULL DEFAULT 0,
    comment_count            INTEGER      NOT NULL DEFAULT 0,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_posts_type
        CHECK (type IN ('TEXT', 'AUDIO')),

    CONSTRAINT chk_posts_audio_duration
        CHECK (audio_duration_seconds IS NULL OR audio_duration_seconds BETWEEN 1 AND 300),

    CONSTRAINT chk_posts_content_by_type
        CHECK (
            (type = 'TEXT'  AND content IS NOT NULL AND audio_key IS NULL) OR
            (type = 'AUDIO' AND audio_key IS NOT NULL AND content IS NULL)
        ),

    CONSTRAINT chk_posts_counters_non_negative
        CHECK (like_count >= 0 AND comment_count >= 0)
);

-- ============================================================
-- Indices
-- ============================================================

CREATE INDEX idx_posts_author_id
    ON posts (author_id);

CREATE INDEX idx_posts_created_at
    ON posts (created_at DESC);

CREATE INDEX idx_posts_author_created
    ON posts (author_id, created_at DESC);