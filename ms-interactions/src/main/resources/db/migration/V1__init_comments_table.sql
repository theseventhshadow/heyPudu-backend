-- ============================================================
-- Migracion V2 del esquema heypudu_interactions
-- Crea la tabla comments que registra los comentarios
-- de texto que los usuarios hacen en las publicaciones.
-- ============================================================

CREATE TABLE comments (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     UUID          NOT NULL,
    user_id     UUID          NOT NULL,
    content     VARCHAR(500)  NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- ============================================================
-- Indices
-- ============================================================

-- Busqueda por post: se usa al listar comentarios y al contar.
CREATE INDEX idx_comments_post_id
    ON comments (post_id);

-- Busqueda por usuario: se usa al eliminar comentarios de un usuario
-- cuando se anonimiza.
CREATE INDEX idx_comments_user_id
    ON comments (user_id);

-- Orden por fecha dentro de un post: optimiza la paginacion
-- cuando se listan comentarios ordenados por createdAt.
CREATE INDEX idx_comments_post_created
    ON comments (post_id, created_at);