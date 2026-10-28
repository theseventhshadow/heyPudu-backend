# ms-users

Microservicio de perfiles y relaciones sociales para **heyPudú!**.

## Descripción

`ms-users` gestiona los perfiles de usuario y las relaciones de seguimiento:

- Crear el perfil base tras el sync con `ms-auth`.
- Ver y editar el perfil propio.
- Ver el perfil público de otros.
- Seguir y dejar de seguir.
- Buscar usuarios.
- Administrar usuarios (banear, desbanear, verificar, desverificar).
- Generar URLs prefirmadas para subir el avatar a S3.

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 25 |
| Framework | Spring Boot 3.5.x |
| Build | Gradle 8.10.2+ |
| Seguridad | Spring Security + OAuth2 Resource Server |
| Autenticación | AWS Cognito (JWT) |
| Persistencia | Spring Data JPA + PostgreSQL |
| Migraciones | Flyway |
| Cliente HTTP | RestClient (Spring 6.1+) |
| Almacenamiento | AWS S3 (presigned URLs) |
| Monitoreo | Spring Boot Actuator |

## Arquitectura

```
[React + AWS Amplify]
        |
        | JWT
        v
[AWS API Gateway]
        |
        v
[ms-users :8082]  --->  [ms-auth :8081]        (disable/enable/delete en Cognito)
        |
        v
[heypudu_users] + [S3: heypudu-avatars]
```

- **Puerto:** `8082`
- **Esquema de BD:** `heypudu_users`
- **Bucket S3:** `heypudu-avatars`
- **Depende de:** `ms-auth`

## Estructura del proyecto

```
ms-users/
├── build.gradle
├── settings.gradle
├── Dockerfile
└── src/main/
    ├── java/com/heypudu/ms_users/
    │   ├── MsUsersApplication.java
    │   ├── config/
    │   │   ├── SecurityConfig.java
    │   │   ├── JwtConfig.java
    │   │   ├── RestClientConfig.java
    │   │   └── S3Config.java
    │   ├── controller/
    │   │   ├── UserController.java
    │   │   ├── FollowController.java
    │   │   ├── AdminUserController.java
    │   │   └── InternalUserController.java
    │   ├── client/
    │   │   ├── AuthClient.java
    │   │   └── PostClient.java
    │   ├── dto/
    │   │   ├── request/
    │   │   └── response/
    │   ├── exception/
    │   ├── mapper/
    │   ├── model/
    │   ├── repository/
    │   ├── security/
    │   └── service/
    └── resources/
        ├── application.yml
        ├── application-dev.yml
        ├── application-prod.yml
        └── db/migration/
            ├── V1__init_users_schema.sql
            └── V2__init_follows_table.sql
```

## Endpoints

### Perfil propio

| Método | Endpoint | Descripción | Rol |
|---|---|---|---|
| `GET` | `/api/v1/users/me` | Ver perfil propio | `user`, `admin` |
| `PUT` | `/api/v1/users/me` | Editar perfil propio | `user`, `admin` |
| `DELETE` | `/api/v1/users/me` | Eliminar cuenta (anonimiza) | `user`, `admin` |

### Perfil ajeno y relaciones

| Método | Endpoint | Descripción | Rol |
|---|---|---|---|
| `GET` | `/api/v1/users/{id}` | Ver perfil público | `user`, `admin` |
| `GET` | `/api/v1/users/{id}/followers` | Listar seguidores | `user`, `admin` |
| `GET` | `/api/v1/users/{id}/following` | Listar seguidos | `user`, `admin` |
| `POST` | `/api/v1/users/{id}/follow` | Seguir | `user`, `admin` |
| `DELETE` | `/api/v1/users/{id}/follow` | Dejar de seguir | `user`, `admin` |
| `GET` | `/api/v1/users/search?q=` | Buscar usuarios | `user`, `admin` |

### Almacenamiento

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/v1/users/me/avatar/presigned-url` | URL prefirmada para avatar |

### Administración

| Método | Endpoint | Descripción | Rol |
|---|---|---|---|
| `GET` | `/api/v1/admin/users` | Listar todos | `admin` |
| `PUT` | `/api/v1/admin/users/{id}/ban` | Banear | `admin` |
| `PUT` | `/api/v1/admin/users/{id}/unban` | Desbanear | `admin` |
| `PUT` | `/api/v1/admin/users/{id}/verify` | Verificar | `admin` |
| `PUT` | `/api/v1/admin/users/{id}/unverify` | Desverificar | `admin` |

### Endpoints internos

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/v1/internal/users` | Crear perfil base |
| `GET` | `/api/v1/internal/users/{id}/summary` | Resumen de un usuario |
| `GET` | `/api/v1/internal/users/{id}/following/ids` | IDs de seguidos |
| `GET` | `/api/v1/internal/users/{id}/exists` | Verificar existencia |

### Salud

| Método | Endpoint |
|---|---|
| `GET` | `/actuator/health` |
| `GET` | `/actuator/info` |

## Modelo de datos

### Tabla `users`

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | `UUID` | PK |
| `cognito_sub` | `UUID` | `NOT NULL UNIQUE` |
| `name` | `VARCHAR(100)` | `NOT NULL` |
| `username` | `VARCHAR(50)` | `NOT NULL UNIQUE` |
| `bio` | `VARCHAR(300)` | |
| `avatar_key` | `VARCHAR(500)` | |
| `is_verified` | `BOOLEAN` | `NOT NULL DEFAULT FALSE` |
| `status` | `VARCHAR(20)` | `NOT NULL DEFAULT 'ACTIVE'` |
| `follower_count` | `INTEGER` | `NOT NULL DEFAULT 0` |
| `following_count` | `INTEGER` | `NOT NULL DEFAULT 0` |
| `post_count` | `INTEGER` | `NOT NULL DEFAULT 0` |
| `username_updated_at` | `TIMESTAMPTZ` | |
| `anonymized_at` | `TIMESTAMPTZ` | |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL` |

### Tabla `follows`

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | `UUID` | PK |
| `follower_id` | `UUID` | `NOT NULL` |
| `followed_id` | `UUID` | `NOT NULL` |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` |

Restricciones: `UNIQUE (follower_id, followed_id)`, `CHECK (follower_id <> followed_id)`.

## Reglas de negocio

| Regla | Detalle |
|---|---|
| `username` único | Validación en creación y edición |
| Cambio de `username` | Una vez cada 30 días (`username_updated_at`) |
| Un usuario no puede seguirse a sí mismo | `BusinessException` + CHECK en BD |
| Seguir/dejar de seguir idempotente | Sin error si ya existe/no existe |
| Banear | `status = BANNED` + deshabilitar en Cognito |
| Desbanear | `status = ACTIVE` + habilitar en Cognito |
| Banear no elimina nada | Los datos se mantienen para poder desbanear |
| Eliminar cuenta | Anonimización real + eliminar de Cognito |
| Búsqueda | Por `name` o `username`, case-insensitive |
| Perfil público | No expone `cognitoSub` ni `status` |

## Flujos principales

### Creación de perfil tras el registro

```
[Cognito crea la cuenta]
        |
        v
[React llama a POST /api/v1/auth/sync]
        |
        v
[ms-auth crea el registro en auth_users]
        |
        v
[ms-auth llama a POST /api/v1/internal/users en ms-users]
        |
        v
[ms-users crea el perfil con name="Usuario" y username="user_<sub8>"]
        |
        v
[React redirige a "completar perfil"]
        |
        v
[Usuario llama a PUT /api/v1/users/me con datos reales]
```

### Baneo

```
[Admin llama a PUT /api/v1/admin/users/{id}/ban]
        |
        v
[ms-users cambia status a BANNED]
        |
        v
[ms-users llama a ms-auth: PUT /api/v1/internal/users/{sub}/disable]
        |
        v
[ms-auth deshabilita al usuario en Cognito]
```

### Eliminación de cuenta

```
[Usuario llama a DELETE /api/v1/users/me]
        |
        v
[ms-users anonimiza: name, username, bio, avatar_key]
        |
        v
[ms-users cambia status a DELETED]
        |
        v
[ms-users llama a ms-auth: DELETE /api/v1/internal/users/{sub}]
        |
        v
[ms-auth elimina al usuario de Cognito]
```

## Configuración

### Variables de entorno en producción

| Variable | Descripción |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | JDBC con `currentSchema=heypudu_users` |
| `DB_USERNAME` | Usuario BD |
| `DB_PASSWORD` | Contraseña BD |
| `COGNITO_ISSUER_URI` | Issuer Cognito |
| `COGNITO_JWK_SET_URI` | JWKS Cognito |
| `CLIENTS_AUTH_BASE_URL` | URL de `ms-auth` |
| `CLIENTS_POSTS_BASE_URL` | URL de `ms-posts` |
| `AWS_REGION` | Región de S3 |
| `S3_AVATARS_BUCKET` | Bucket de avatares |

## Ejecución local

### Requisitos

- JDK 25
- PostgreSQL (Docker)
- Credenciales AWS configuradas
- `ms-auth` corriendo en `:8081`

### Levantar PostgreSQL

```bash
docker run -d --name heypudu-postgres \
  -e POSTGRES_DB=heypudu \
  -e POSTGRES_USER=heypudu_admin \
  -e POSTGRES_PASSWORD=devpassword \
  -p 5432:5432 \
  postgres:16
```

Crear el esquema:

```sql
CREATE SCHEMA IF NOT EXISTS heypudu_users;
CREATE USER heypudu_users_user WITH PASSWORD 'devpassword';
GRANT ALL PRIVILEGES ON SCHEMA heypudu_users TO heypudu_users_user;
```

### Ejecutar

```bash
./gradlew bootRun
```

Verificar:

```bash
curl http://localhost:8082/actuator/health
```

## Decisiones de diseño

| Decisión | Razón |
|---|---|
| `cognito_sub` como clave externa | Identidad del usuario en Cognito |
| `avatar_key` en lugar de URL | Consistencia con otros microservicios; permite cambiar de proveedor de storage |
| `is_verified` como atributo | No es un rol, es una cualidad del perfil |
| Banear ≠ eliminar | Permite desbanear sin pérdida de datos |
| Anonimización al eliminar | Evita referencias rotas en otras BD |
| Contadores desnormalizados | Lectura rápida del perfil |
| Cambio de username cada 30 días | Evita abuso y suplantación |
| Endpoints internos abiertos (EP1) | Consistencia; trabajo futuro migrar a token de servicio |
| `PostClient` sin uso en EP1 | Documentado para trabajo futuro |

## Trabajo futuro

- Token de servicio para endpoints internos
- Sincronización asíncrona de `username`/`avatar` con `ms-posts`
- `post_count` actualizado vía eventos
- Eliminación diferida 30 días (con cancelación)
- Búsqueda con `pg_trgm`
- Notificaciones al recibir un follow

## Autor

Proyecto desarrollado para la asignatura **DSY1107 - Desarrollo Cloud Native**.