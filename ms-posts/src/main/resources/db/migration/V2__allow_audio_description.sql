-- ============================================================
-- Migracion V2 del esquema heypudu_posts
-- Permite texto descriptivo opcional en publicaciones AUDIO.
-- ============================================================

ALTER TABLE posts
    DROP CONSTRAINT IF EXISTS chk_posts_content_by_type;

ALTER TABLE posts
    ADD CONSTRAINT chk_posts_content_by_type
        CHECK (
            (type = 'TEXT' AND content IS NOT NULL AND audio_key IS NULL) OR
            (type = 'AUDIO' AND audio_key IS NOT NULL)
        );
