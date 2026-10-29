-- ============================================================
-- Recalcula los contadores de perfil a partir de los datos reales.
-- Ejecutar una sola vez tras desplegar el incremento de postCount.
-- ============================================================

UPDATE heypudu_users.users u
SET post_count = (
    SELECT COUNT(*)
    FROM heypudu_posts.posts p
    WHERE p.author_id = u.cognito_sub
);

UPDATE heypudu_users.users u
SET follower_count = (
    SELECT COUNT(*)
    FROM heypudu_users.follows f
    WHERE f.followed_id = u.id
);

UPDATE heypudu_users.users u
SET following_count = (
    SELECT COUNT(*)
    FROM heypudu_users.follows f
    WHERE f.follower_id = u.id
);
