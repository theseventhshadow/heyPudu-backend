-- ============================================================
-- Script de inicializacion de la base de datos heyPudu!
-- Se ejecuta automaticamente la primera vez que se levanta
-- el contenedor de PostgreSQL en docker-compose.
--
-- Crea un esquema y un usuario por microservicio, siguiendo
-- el principio de database-per-service.
-- ============================================================

-- ============================================================
-- Esquemas
-- ============================================================
CREATE SCHEMA IF NOT EXISTS heypudu_auth;
CREATE SCHEMA IF NOT EXISTS heypudu_users;
CREATE SCHEMA IF NOT EXISTS heypudu_posts;
CREATE SCHEMA IF NOT EXISTS heypudu_interactions;

-- ============================================================
-- Usuarios por microservicio
-- ============================================================
CREATE USER heypudu_auth_user WITH PASSWORD 'devpassword';
CREATE USER heypudu_users_user WITH PASSWORD 'devpassword';
CREATE USER heypudu_posts_user WITH PASSWORD 'devpassword';
CREATE USER heypudu_interactions_user WITH PASSWORD 'devpassword';

-- ============================================================
-- Permisos sobre cada esquema
-- ============================================================

-- ms-auth
GRANT ALL PRIVILEGES ON SCHEMA heypudu_auth TO heypudu_auth_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA heypudu_auth TO heypudu_auth_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA heypudu_auth TO heypudu_auth_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA heypudu_auth
    GRANT ALL PRIVILEGES ON TABLES TO heypudu_auth_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA heypudu_auth
    GRANT ALL PRIVILEGES ON SEQUENCES TO heypudu_auth_user;

-- ms-users
GRANT ALL PRIVILEGES ON SCHEMA heypudu_users TO heypudu_users_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA heypudu_users TO heypudu_users_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA heypudu_users TO heypudu_users_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA heypudu_users
    GRANT ALL PRIVILEGES ON TABLES TO heypudu_users_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA heypudu_users
    GRANT ALL PRIVILEGES ON SEQUENCES TO heypudu_users_user;

-- ms-posts
GRANT ALL PRIVILEGES ON SCHEMA heypudu_posts TO heypudu_posts_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA heypudu_posts TO heypudu_posts_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA heypudu_posts TO heypudu_posts_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA heypudu_posts
    GRANT ALL PRIVILEGES ON TABLES TO heypudu_posts_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA heypudu_posts
    GRANT ALL PRIVILEGES ON SEQUENCES TO heypudu_posts_user;

-- ms-interactions
GRANT ALL PRIVILEGES ON SCHEMA heypudu_interactions TO heypudu_interactions_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA heypudu_interactions TO heypudu_interactions_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA heypudu_interactions TO heypudu_interactions_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA heypudu_interactions
    GRANT ALL PRIVILEGES ON TABLES TO heypudu_interactions_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA heypudu_interactions
    GRANT ALL PRIVILEGES ON SEQUENCES TO heypudu_interactions_user;