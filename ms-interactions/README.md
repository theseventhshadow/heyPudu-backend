# ms-interactions

Microservicio de interacciones sociales para **heyPudú!**.

## Descripción

`ms-interactions` gestiona las interacciones que los usuarios tienen con las publicaciones:

- **Likes**: dar, quitar, listar, contar.
- **Comentarios**: crear, listar, eliminar (solo texto en EP1).

Forma parte de la arquitectura de microservicios de heyPudú!, junto a `ms-auth`, `ms-users` y `ms-posts`.

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
[ms-interactions :8084]  --->  [ms-posts :8083]
        |                              |
        v                              v
[heypudu_interactions]          [heypudu_posts]
```

- **Puerto:** `8084`
- **Esquema de BD:** `heypudu_interactions`
- **Depende de:** `ms-posts` (para validar existencia de posts y actualizar contadores).
- **IdP:** AWS Cognito (User Pool).

## Estructura del proyecto

```
ms-interactions/
├── build.gradle
├── settings.gradle
├── Dockerfile
└── src/main/
    ├── java/com/heypudu/ms_interactions/
    │   ├── MsInteractionsApplication.java
    │   ├── config/
    │   │   ├── SecurityConfig.java
    │   │   ├── JwtConfig.java
    │   │   └── RestClientConfig.java
    │   ├── controller/
    │   │   ├── LikeController.java
    │   │   └── CommentController.java
    │   ├── client/
    │   │   └── PostClient.java
    │   ├── dto/
    │   │   ├── request/
    │   │   │   └── CreateCommentRequest.java
    │   │   └── response/
    │   │       ├── LikeResponse.java
    │   │       ├── CommentResponse.java
    │   │       └── LikeCountResponse.java
    │   ├── exception/
    │   │   ├── GlobalExceptionHandler.java
    │   │   ├── ResourceNotFoundException.java
    │   │   └── BusinessException.java
    │   ├── mapper/
    │   │   └── InteractionMapper.java
    │   ├── model/
    │   │   ├── Like.java
    │   │   └── Comment.java
    │   ├── repository/
    │   │   ├── LikeRepository.java
    │   │   └── CommentRepository.java
    │   ├── security/
    │   │   ├── JwtAuthConverter.java
    │   │   └── CognitoUserExtractor.java
    │   └── service/
    │       ├── LikeService.java
    │       ├── CommentService.java
    │       └── impl/
    │           ├── LikeServiceImpl.java
    │           └── CommentServiceImpl.java
    └── resources/
        ├── application.yml
        ├── application-dev.yml
        ├── application-prod.yml
        └── db/migration/
            ├── V1__init_likes_table.sql
            └── V2__init_comments_table.sql
```

## Endpoints

Todos los endpoints están versionados bajo `/api/v1`.

### Likes

| Método | Endpoint | Descripción | Rol |
|---|---|---|---|
| `POST` | `/api/v1/posts/{postId}/likes` | Dar like a un post | `user`, `admin` |
| `DELETE` | `/api/v1/posts/{postId}/likes` | Quitar like | `user`, `admin` |
| `GET` | `/api/v1/posts/{postId}/likes` | Listar likes del post (paginado) | `user`, `admin` |
| `GET` | `/api/v1/posts/{postId}/likes/count` | Contar likes del post | `user`, `admin` |
| `GET` | `/api/v1/users/me/likes` | Listar mis likes (paginado) | `user`, `admin` |

### Comentarios

| Método | Endpoint | Descripción | Rol |
|---|---|---|---|
| `POST` | `/api/v1/posts/{postId}/comments` | Crear comentario (texto) | `user`, `admin` |
| `GET` | `/api/v1/posts/{postId}/comments` | Listar comentarios (paginado) | `user`, `admin` |
| `DELETE` | `/api/v1/comments/{commentId}` | Eliminar comentario propio | `user`, `admin` |
| `DELETE` | `/api/v1/admin/comments/{commentId}` | Eliminar cualquier comentario | `admin` |

### Endpoints de salud

| Método | Endpoint | Acceso |
|---|---|---|
| `GET` | `/actuator/health` | Público |
| `GET` | `/actuator/info` | Público (solo en `dev`) |

### Detalle de los endpoints principales

#### `POST /api/v1/posts/{postId}/likes`

Dar like a un post. **Idempotente**: si el usuario ya dio like, devuelve el like existente sin crear uno nuevo.

- **Body:** no recibe.
- **Respuesta `200 OK`:**

```json
{
  "id": "8a4b2c1d-...",
  "postId": "3f1c9e2b-...",
  "userId": "a1b2c3d4-...",
  "createdAt": "2026-10-06T12:00:00Z"
}
```

#### `DELETE /api/v1/posts/{postId}/likes`

Quitar el like de un post. **Idempotente**: si no existe, no hace nada.

- **Respuesta `204 No Content`**.

#### `GET /api/v1/posts/{postId}/likes`

Listar likes de un post, paginados.

- **Query params:** `?page=0&size=20&sort=createdAt,desc`
- **Respuesta `200 OK`:**

```json
{
  "content": [
    {
      "id": "8a4b2c1d-...",
      "postId": "3f1c9e2b-...",
      "userId": "a1b2c3d4-...",
      "createdAt": "2026-10-06T12:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 15,
  "totalPages": 1,
  "last": true
}
```

#### `POST /api/v1/posts/{postId}/comments`

Crear un comentario en un post.

- **Body:**

```json
{
  "content": "Buenísimo el briefcast, me encantó."
}
```

- **Validaciones:** `content` no vacío, máximo 500 caracteres.
- **Respuesta `200 OK`:**

```json
{
  "id": "9b5c3d2e-...",
  "postId": "3f1c9e2b-...",
  "userId": "a1b2c3d4-...",
  "content": "Buenísimo el briefcast, me encantó.",
  "createdAt": "2026-10-06T12:00:00Z"
}
```

#### `DELETE /api/v1/comments/{commentId}`

Eliminar un comentario propio. Solo el autor puede eliminar su comentario.

- **Respuesta `204 No Content`**.
- **Respuesta `400 Bad Request`** si el comentario no es del usuario autenticado.

#### `DELETE /api/v1/admin/comments/{commentId}`

Eliminar cualquier comentario. Solo admins.

- **Requiere:** rol `ROLE_ADMIN` (grupo `admin` en Cognito).
- **Respuesta `204 No Content`**.

## Modelo de datos

### Tabla `likes` (esquema `heypudu_interactions`)

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | `UUID` | `PRIMARY KEY` | Clave primaria. |
| `post_id` | `UUID` | `NOT NULL` | Post al que se da like. |
| `user_id` | `UUID` | `NOT NULL` | Usuario que da like (`cognito_sub`). |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` | Fecha de creación. |

- **`UNIQUE (post_id, user_id)`**: un usuario no puede dar like dos veces al mismo post.

### Tabla `comments` (esquema `heypudu_interactions`)

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | `UUID` | `PRIMARY KEY` | Clave primaria. |
| `post_id` | `UUID` | `NOT NULL` | Post comentado. |
| `user_id` | `UUID` | `NOT NULL` | Autor del comentario (`cognito_sub`). |
| `content` | `VARCHAR(500)` | `NOT NULL` | Texto del comentario. |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` | Fecha de creación. |

## Roles y seguridad

| Grupo Cognito | Rol Spring | Uso en `ms-interactions` |
|---|---|---|
| `user` | `ROLE_USER` | Interacciones normales (likes, comentarios). |
| `admin` | `ROLE_ADMIN` | Eliminar cualquier comentario, ver cualquier interacción. |
| `internal` | `ROLE_INTERNAL` | (Trabajo futuro) Cliente de servicio para endpoints internos. |

Los roles viajan en el claim `cognito:groups` del JWT y son convertidos a `GrantedAuthority` por `JwtAuthConverter`.

## Comunicación con `ms-posts`

`ms-interactions` se comunica con `ms-posts` para dos operaciones:

| Operación | Método y endpoint en `ms-posts` | Cuándo se llama |
|---|---|---|
| Validar que un post existe | `GET /api/v1/posts/{id}` | Antes de crear un like o un comentario. |
| Actualizar contadores | `PATCH /api/v1/internal/posts/{id}/counters` | Al crear o eliminar un like o comentario. |

### Contrato del endpoint de contadores

- **Request:**

```json
{
  "likeDelta": 1,
  "commentDelta": 0
}
```

- **Response:** `204 No Content` si tuvo éxito.

- **Autenticación:** este endpoint interno **no requiere JWT** de usuario. Se documenta como trabajo futuro migrar a un token de servicio con `client_credentials`.

### Cliente HTTP

`PostClient` encapsula ambas operaciones. Se apoya en un `RestClient` configurado en `RestClientConfig`.

- Si falla la validación del post → lanza `BusinessException`.
- Si falla la actualización de contadores → lanza `BusinessException`.

**Importante:** si `ms-posts` está caído, las operaciones de creación/eliminación de likes y comentarios fallan y se revierten por `@Transactional`. Esto evita inconsistencias entre los contadores de `ms-posts` y los registros reales de `ms-interactions`.

## Configuración

### Variables de entorno requeridas en producción

| Variable | Descripción |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Debe ser `prod` en producción. |
| `DB_URL` | URL JDBC de PostgreSQL (incluye `currentSchema=heypudu_interactions`). |
| `DB_USERNAME` | Usuario de BD. |
| `DB_PASSWORD` | Contraseña de BD. |
| `COGNITO_ISSUER_URI` | Issuer del User Pool de Cognito. |
| `COGNITO_JWK_SET_URI` | Endpoint JWKS del User Pool. |
| `CLIENTS_POSTS_BASE_URL` | URL base de `ms-posts`. |

### Perfiles

| Perfil | Uso | Credenciales | `ms-posts` |
|---|---|---|---|
| `dev` | Desarrollo local | Hardcodeadas | `http://localhost:8083` |
| `prod` | Producción | Variables de entorno | Variable obligatoria |

## Ejecución local

### Requisitos

- JDK 25
- Docker (opcional, para PostgreSQL)
- Gradle Wrapper (incluido en el repo)
- **`ms-posts` corriendo** en `http://localhost:8083`

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
CREATE SCHEMA IF NOT EXISTS heypudu_interactions;

CREATE USER heypudu_interactions_user WITH PASSWORD 'devpassword';
GRANT ALL PRIVILEGES ON SCHEMA heypudu_interactions TO heypudu_interactions_user;
```

### Ejecutar el microservicio

```bash
./gradlew bootRun
```

El servicio queda disponible en `http://localhost:8084`.

### Verificar que funciona

```bash
curl http://localhost:8084/actuator/health
```

## Build y despliegue

### Construir el JAR

```bash
./gradlew build
```

El JAR queda en `build/libs/ms-interactions-0.0.1-SNAPSHOT.jar`.

### Construir la imagen Docker

```bash
docker build -t heypudu/ms-interactions:latest .
```

### Ejecutar el contenedor

```bash
docker run -d --name ms-interactions \
  -p 8084:8084 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL="jdbc:postgresql://host:5432/heypudu?currentSchema=heypudu_interactions" \
  -e DB_USERNAME="heypudu_interactions_user" \
  -e DB_PASSWORD="********" \
  -e COGNITO_ISSUER_URI="https://cognito-idp.<region>.amazonaws.com/<poolId>" \
  -e COGNITO_JWK_SET_URI="https://cognito-idp.<region>.amazonaws.com/<poolId>/.well-known/jwks.json" \
  -e CLIENTS_POSTS_BASE_URL="http://ms-posts.internal:8083" \
  heypudu/ms-interactions:latest
```

## Flujos principales

### Dar like a un post

```
[React: POST /api/v1/posts/{postId}/likes]
        |
        v
[ms-interactions valida JWT]
        |
        v
[Extrae userId del JWT]
        |
        v
[Busca like existente (postId, userId)]
        |
        +--- Existe → devuelve el existente
        |
        +--- No existe
                |
                v
        [GET ms-posts /api/v1/posts/{postId}]  ← validar que existe
                |
                v
        [Guardar like en BD]
                |
                v
        [PATCH ms-posts /api/v1/internal/posts/{postId}/counters {likeDelta: 1}]
                |
                v
        [Devolver LikeResponse]
```

### Crear comentario

```
[React: POST /api/v1/posts/{postId}/comments con {content}]
        |
        v
[ms-interactions valida JWT y DTO]
        |
        v
[Extrae userId del JWT]
        |
        v
[GET ms-posts /api/v1/posts/{postId}]  ← validar que existe
        |
        v
[Guardar comentario en BD]
        |
        v
[PATCH ms-posts /api/v1/internal/posts/{postId}/counters {commentDelta: 1}]
        |
        v
[Devolver CommentResponse]
```

### Eliminar comentario propio

```
[React: DELETE /api/v1/comments/{commentId}]
        |
        v
[ms-interactions valida JWT]
        |
        v
[Busca comentario por id]
        |
        v
[Verifica que userId del comentario == userId del JWT]
        |
        +--- No coincide → BusinessException (400)
        |
        +--- Coincide
                |
                v
        [Eliminar comentario]
                |
                v
        [PATCH ms-posts /counters {commentDelta: -1}]
                |
                v
        [Devolver 204]
```

## Decisiones de diseño

| Decisión | Razón |
|---|---|
| Sin FK física hacia `posts` ni `users` | Esos datos viven en otras BD. La integridad se maneja a nivel de aplicación. |
| Validar que el post existe antes de crear un like/comentario | Evita interacciones huérfanas. |
| `POST /likes` idempotente | Evita duplicados por reintentos del frontend. |
| `DELETE /likes` idempotente | Igual, sin error si el like no existe. |
| Eliminar comentario: solo autor o admin | Regla de negocio clara, con endpoints separados. |
| `like_count` y `comment_count` en `ms-posts` | Desnormalización para lectura rápida. `ms-interactions` los mantiene. |
| Si `ms-posts` falla, la operación se revierte | Evita inconsistencias entre contadores y registros reales. |
| `RestClient` en lugar de OpenFeign | Más simple para dos llamadas puntuales. |
| Endpoints internos sin JWT (EP1) | Se documenta como trabajo futuro migrar a un token de servicio. |
| Comentarios solo texto en EP1 | Audio queda como trabajo futuro. |

## Trabajo futuro

- **Token de servicio para endpoints internos**: usar `client_credentials` en Cognito para que `ms-interactions` se autentique como cliente de servicio en lugar de reenviar el JWT del usuario.
- **Comentarios en audio**: soporte para comentarios grabados (máx. 1 minuto).
- **Reacciones**: más allá del like (me encanta, me ríe, etc.).
- **Sincronización asíncrona**: usar SQS/SNS para actualizar contadores, en lugar de REST síncrono.
- **Retry con Resilience4j**: si `ms-posts` está caído, reintentar con backoff exponencial antes de fallar.
- **Edición de comentarios**: agregar `updated_at` y endpoint `PUT /api/v1/comments/{id}`.
- **Notificaciones**: cuando alguien da like o comenta, notificar al autor del post.

## Autor

Proyecto desarrollado para la asignatura **DSY1107 - Desarrollo Cloud Native**.