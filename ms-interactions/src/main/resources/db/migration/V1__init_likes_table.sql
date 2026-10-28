-- ============================================================
-- Migracion inicial del esquema heypudu_interactions
-- Crea la tabla likes que registra los "me gusta" de los
-- usuarios sobre las publicaciones.
-- ============================================================

CREATE TABLE likes (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     UUID         NOT NULL,
    user_id     UUID         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_likes_post_user
        UNIQUE (post_id, user_id)
);

-- ============================================================
-- Indices
-- ============================================================

-- Busqueda por post: se usa al listar likes de un post y al contar.
CREATE INDEX idx_likes_post_id
    ON likes (post_id);

-- Busqueda por usuario: se usa al listar los likes de un usuario.
CREATE INDEX idx_likes_user_id
    ON likes (user_id);

-- Busqueda combinada: se usa al verificar si un usuario ya dio like.
-- El UNIQUE ya crea un indice, asi que este es redundante pero explicito.
-- Si prefieres evitar redundancia, puedes omitir este indice.
CREATE INDEX idx_likes_post_user
    ON likes (post_id, user_id);