-- ============================================================
-- Migracion V5 del esquema heypudu_posts
-- Guarda el nombre visible del autor junto al username, igual
-- que ya se hace con el username y el avatar. Las publicaciones
-- anteriores quedan con NULL y el cliente usa el username.
-- ============================================================

ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS author_name VARCHAR(80);
