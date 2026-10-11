# ms-posts

Microservicio de publicaciones y feed para **heyPudú!**.

## Descripción

`ms-posts` gestiona las publicaciones (briefcasts) de la red social:

- Crear, leer, editar y eliminar publicaciones de texto o audio.
- Generar URLs prefirmadas de S3 para subir audio y portadas.
- Servir el feed general, el feed personalizado y la búsqueda.
- Mantener los contadores de likes y comentarios desnormalizados.

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
[ms-posts :8083]  --->  [ms-users :8082]         (datos del autor, seguidos)
        |          --->  [ms-interactions :8084] (notificar eliminación)
        |
        v
[heypudu_posts] + [S3: heypudu-audio, heypudu-covers]
```

- **Puerto:** `8083`
- **Esquema de BD:** `heypudu_posts`
- **Buckets S3:** `heypudu-audio`, `heypudu-covers`
- **Depende de:** `ms-users`, `ms-interactions`

## Estructura del proyecto

```
ms-posts/
├── build.gradle
├── settings.gradle
├── Dockerfile
└── src/main/
    ├── java/com/heypudu/ms_posts/
    │   ├── MsPostsApplication.java
    │   ├── config/
    │   │   ├── SecurityConfig.java
    │   │   ├── JwtConfig.java
    │   │   ├── RestClientConfig.java
    │   │   └── S3Config.java
    │   ├── controller/
    │   │   ├── PostController.java
    │   │   ├── FeedController.java
    │   │   └── InternalPostController.java
    │   ├── client/
    │   │   ├── UserClient.java
    │   │   └── InteractionClient.java
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
            └── V1__init_posts_schema.sql
```

## Endpoints

### Publicaciones

| Método | Endpoint | Descripción | Rol |
|---|---|---|---|
| `GET` | `/api/v1/posts` | Feed general (paginado) | `user`, `admin` |
| `GET` | `/api/v1/posts/{id}` | Ver publicación | `user`, `admin` |
| `GET` | `/api/v1/posts/me` | Mis publicaciones | `user`, `admin` |
| `GET` | `/api/v1/posts/author/{authorId}` | Publicaciones de un autor | `user`, `admin` |
| `POST` | `/api/v1/posts` | Crear publicación | `user`, `admin` |
| `PUT` | `/api/v1/posts/{id}` | Editar publicación propia | `user`, `admin` |
| `DELETE` | `/api/v1/posts/{id}` | Eliminar publicación propia | `user`, `admin` |
| `GET` | `/api/v1/posts/feed` | Feed personalizado | `user`, `admin` |
| `GET` | `/api/v1/posts/search?q=` | Búsqueda | `user`, `admin` |

### Almacenamiento (URLs prefirmadas)

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/v1/posts/audio/presigned-url` | URL prefirmada para audio |
| `POST` | `/api/v1/posts/covers/presigned-url` | URL prefirmada para portada |

### Endpoints internos

| Método | Endpoint | Descripción |
|---|---|---|
| `PATCH` | `/api/v1/internal/posts/{id}/counters` | Actualizar contadores |
| `DELETE` | `/api/v1/internal/posts/users/{userId}` | Anonimizar posts de un usuario |

### Salud

| Método | Endpoint |
|---|---|
| `GET` | `/actuator/health` |
| `GET` | `/actuator/info` |

## Modelo de datos

### Tabla `posts` (esquema `heypudu_posts`)

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | `UUID` | `PRIMARY KEY` |
| `author_id` | `UUID` | `NOT NULL` |
| `author_username` | `VARCHAR(50)` | `NOT NULL` |
| `author_avatar_url` | `VARCHAR(500)` | |
| `type` | `VARCHAR(20)` | `NOT NULL`, `CHECK` (`TEXT`, `AUDIO`) |
| `title` | `VARCHAR(50)` | `NOT NULL` |
| `content` | `TEXT` | |
| `audio_key` | `VARCHAR(500)` | |
| `audio_duration_seconds` | `INTEGER` | `CHECK` (1–300) |
| `cover_image_key` | `VARCHAR(500)` | |
| `like_count` | `INTEGER` | `NOT NULL DEFAULT 0` |
| `comment_count` | `INTEGER` | `NOT NULL DEFAULT 0` |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL` |

## Flujo de subida de audio

```
[React pide POST /api/v1/posts/audio/presigned-url]
        |
        v
[ms-posts genera URL prefirmada de S3 (válida 15 min)]
        |
        v
[React sube el audio DIRECTO a S3 con PUT]
        |
        v
[React llama a POST /api/v1/posts con {type: "AUDIO", audioKey, audioDurationSeconds, title}]
        |
        v
[ms-posts valida, obtiene datos del autor desde ms-users, guarda en BD]
        |
        v
[Devuelve el post con URL prefirmada de descarga]
```

## Flujo de eliminación de post

```
[React: DELETE /api/v1/posts/{id}]
        |
        v
[ms-posts verifica autoría]
        |
        v
[ms-posts notifica a ms-interactions: DELETE /api/v1/internal/posts/{id}/interactions]
        |
        v
[ms-posts elimina audio y portada de S3]
        |
        v
[ms-posts elimina el post de la BD]
```

## Comunicación con otros microservicios

### `ms-users`

| Operación | Método y endpoint en `ms-users` |
|---|---|
| Obtener datos del autor | `GET /api/v1/users/{id}` |
| Lista de seguidos (solo IDs) | `GET /api/v1/users/{id}/following/ids` |

### `ms-interactions`

| Operación | Método y endpoint en `ms-interactions` |
|---|---|
| Eliminar interacciones de un post | `DELETE /api/v1/internal/posts/{postId}/interactions` |

> **Endpoints requeridos en `ms-interactions`** (ya existen):
> - `DELETE /api/v1/internal/posts/{postId}/interactions`
> - `DELETE /api/v1/internal/users/{userId}/interactions`

## Configuración

### Variables de entorno en producción

| Variable | Descripción |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | JDBC con `currentSchema=heypudu_posts` |
| `DB_USERNAME` | Usuario BD |
| `DB_PASSWORD` | Contraseña BD |
| `COGNITO_ISSUER_URI` | Issuer Cognito |
| `COGNITO_JWK_SET_URI` | JWKS Cognito |
| `CLIENTS_USERS_BASE_URL` | URL de `ms-users` |
| `CLIENTS_INTERACTIONS_BASE_URL` | URL de `ms-interactions` |
| `AWS_REGION` | Región de S3 |
| `S3_AUDIO_BUCKET` | Bucket de audio |
| `S3_COVERS_BUCKET` | Bucket de portadas |

## Ejecución local

### Requisitos

- JDK 25
- PostgreSQL (Docker)
- Credenciales AWS configuradas (`aws configure` o variables de entorno)
- `ms-users` corriendo en `:8082`
- `ms-interactions` corriendo en `:8084`

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
CREATE SCHEMA IF NOT EXISTS heypudu_posts;
CREATE USER heypudu_posts_user WITH PASSWORD 'devpassword';
GRANT ALL PRIVILEGES ON SCHEMA heypudu_posts TO heypudu_posts_user;
```

### Ejecutar

```bash
./gradlew bootRun
```

Verificar:

```bash
curl http://localhost:8083/actuator/health
```

## Decisiones de diseño

| Decisión | Razón |
|---|---|
| El audio nunca pasa por el backend | Se sube directo a S3 con URL prefirmada |
| La BD guarda `audio_key`, no el audio | Los binarios no van en la BD |
| `audio_key` inmutable | Para cambiar el audio, se elimina y se crea otro post |
| Duración máxima 300s | Requisito de briefcasts |
| Duplicación mínima del autor | Evita llamada REST en cada lectura |
| Contadores desnormalizados | Lectura rápida del feed |
| `type` inmutable | No se puede cambiar de texto a audio |
| Post eliminado: hard delete | No hay razón para conservar posts eliminados |
| Anonimización conserva posts | Evita romper referencias en otras BD |
| `S3Service` como interfaz | Facilita cambiar de proveedor de almacenamiento |

## Trabajo futuro

- Sincronización asíncrona de datos del autor (SQS/SNS)
- Token de servicio para endpoints internos
- Búsqueda con `pg_trgm` o `tsvector`
- Retry con Resilience4j hacia `ms-users` y `ms-interactions`
- Grupos de publicaciones (temporadas)
- Comentarios en audio
- Editor de audio con reacciones

## Autor

Proyecto desarrollado para la asignatura **DSY1107 - Desarrollo Cloud Native**.