# ms-interactions

Microservicio de interacciones sociales para **heyPudú!**.

## Responsabilidad

`ms-interactions` gestiona:

- Likes sobre publicaciones.
- Comentarios de texto.
- Reportes de comentarios y sus estados de moderación.
- Eliminación de todas las interacciones asociadas a una publicación cuando
  recibe el comando `post.delete`.
- Publicación de eventos de likes y comentarios para notificaciones y
  auditoría.

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
| Comunicación HTTP | RestClient hacia `ms-posts` |
| Mensajería | RabbitMQ mediante Spring AMQP |
| Monitoreo | Spring Boot Actuator |

## Arquitectura

```text
[React + AWS Amplify]
        |
        | JWT
        v
[AWS API Gateway]
        |
        v
[ms-interactions :8084] ---> [ms-posts :8083]
        |                         |
        v                         `--> contadores de likes/comentarios
[Neon PostgreSQL: heypudu_interactions]
        |
        `--> [RabbitMQ: comandos y eventos]
```

- **Puerto:** `8084`
- **Esquema de BD:** `heypudu_interactions`
- **Dependencia HTTP:** `ms-posts` para validar publicaciones y actualizar sus
  contadores.
- **Dependencia de mensajería:** RabbitMQ para eliminar interacciones de forma
  asíncrona y publicar eventos.
- **IdP:** AWS Cognito mediante JWT.

## Mensajería RabbitMQ

### Consumir `post.delete`

`PostCommandListener` consume `q.heypudu.command`, enlazada a
`ex.heypudu.cmd` con la routing key `post.delete`.

Cuando recibe el comando:

1. Elimina los comentarios del `postId`.
2. Elimina los likes del `postId`.
3. Envía ACK manual solo si ambas operaciones terminan correctamente.
4. Envía NACK sin reencolado si ocurre un error; RabbitMQ dirige el mensaje a
   `q.heypudu.command.dlq`.

Este flujo es asíncrono: `ms-posts` puede responder al `DELETE` de la
publicación sin esperar a que finalice esta limpieza.

### Publicar eventos

`InteractionEventPublisher` publica en `ex.heypudu.events`:

| Routing key | Cuándo se publica | Consumidores |
|---|---|---|
| `interaction.liked` | Se crea un like nuevo | `ms-users` y auditoría |
| `interaction.commented` | Se crea un comentario | `ms-users` y auditoría |

El evento incluye `eventId`, fecha, actor, destinatario, publicación y, para
comentarios, el `commentId`. El destinatario es el autor de la publicación y
permite que `ms-users` persista la notificación correspondiente.

### DLQ

`DeadLetterListener` registra mensajes que no pudieron procesarse en:

- `q.heypudu.command.dlq`
- `q.heypudu.notification.dlq`
- `q.heypudu.audit.dlq`

La configuración compartida usa ACK manual, `default-requeue-rejected=false`,
TTL, límite de mensajes y límite de entregas. La topología completa se define
en `RabbitTopologyConfig`.

| Elemento | Valor |
|---|---|
| Exchange de comandos | `ex.heypudu.cmd` |
| Exchange de eventos | `ex.heypudu.events` |
| Exchange de errores | `ex.heypudu.dlx` |
| Cola de comandos | `q.heypudu.command` |
| DLQ de comandos | `q.heypudu.command.dlq` |
| Comando consumido | `post.delete` |

## Estructura del proyecto

```text
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
    │   │   ├── RestClientConfig.java
    │   │   └── amqp/
    │   ├── controller/
    │   │   ├── LikeController.java
    │   │   ├── CommentController.java
    │   │   ├── CommentReportController.java
    │   │   └── InternalInteractionController.java
    │   ├── client/
    │   │   └── PostClient.java
    │   ├── messaging/
    │   │   ├── PostCommandListener.java
    │   │   ├── InteractionEventPublisher.java
    │   │   └── DeadLetterListener.java
    │   ├── dto/
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
            ├── V1__init_likes_table.sql
            ├── V2__init_comments_table.sql
            ├── V3__init_comment_reports.sql
            └── V4__allow_repeat_reports.sql
```

## Endpoints

Todos los endpoints están versionados bajo `/api/v1`.

### Likes

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/v1/posts/{postId}/likes` | Dar like a una publicación |
| `DELETE` | `/api/v1/posts/{postId}/likes` | Quitar el like propio |
| `GET` | `/api/v1/posts/{postId}/likes` | Listar likes paginados |
| `GET` | `/api/v1/posts/{postId}/likes/count` | Contar likes |
| `GET` | `/api/v1/users/me/likes` | Listar likes del usuario |

### Comentarios

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/v1/posts/{postId}/comments` | Crear comentario de texto |
| `GET` | `/api/v1/posts/{postId}/comments` | Listar comentarios paginados |
| `DELETE` | `/api/v1/comments/{commentId}` | Eliminar comentario propio |
| `DELETE` | `/api/v1/admin/comments/{commentId}` | Eliminar cualquier comentario |

### Reportes

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/v1/comments/{commentId}/reports` | Reportar un comentario |
| `GET` | `/api/v1/admin/comment-reports` | Listar reportes abiertos |
| `PUT` | `/api/v1/admin/comment-reports/{reportId}/{status}` | Cambiar estado |
| `GET` | `/api/v1/internal/comment-reports/counts` | Contadores por autor |

Los endpoints administrativos requieren `ROLE_ADMIN`. Los endpoints internos
son consumidos por otros servicios y no forman parte del flujo público del
frontend.

### Internos

| Método | Endpoint | Descripción |
|---|---|---|
| `DELETE` | `/api/v1/internal/posts/{postId}/interactions` | Eliminar likes y comentarios de un post |

Este endpoint REST se conserva para compatibilidad interna, pero el flujo
principal de eliminación de publicaciones utiliza el comando RabbitMQ
`post.delete`.

### Salud

| Método | Endpoint | Acceso |
|---|---|---|
| `GET` | `/actuator/health` | Público |
| `GET` | `/actuator/info` | Público según el perfil |

## Modelo de datos

### Tabla `likes` (esquema `heypudu_interactions`)

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | `UUID` | `PRIMARY KEY` |
| `post_id` | `UUID` | `NOT NULL` |
| `user_id` | `UUID` | `NOT NULL` |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` |

Existe una restricción `UNIQUE (post_id, user_id)` para evitar likes
duplicados.

### Tabla `comments`

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | `UUID` | `PRIMARY KEY` |
| `post_id` | `UUID` | `NOT NULL` |
| `user_id` | `UUID` | `NOT NULL` |
| `content` | `VARCHAR(500)` | `NOT NULL` |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` |

Las tablas de reportes de comentarios se crean mediante
`V3__init_comment_reports.sql` y `V4__allow_repeat_reports.sql`.

## Roles y seguridad

| Grupo Cognito | Rol Spring | Uso |
|---|---|---|
| `user` | `ROLE_USER` | Likes, comentarios y reportes |
| `admin` | `ROLE_ADMIN` | Moderación y eliminación administrativa |

Los roles viajan en el claim `cognito:groups` del JWT y son convertidos a
autoridades por `JwtAuthConverter`.

## Comunicación con `ms-posts`

`PostClient` utiliza `RestClient` para:

| Operación | Endpoint | Cuándo |
|---|---|---|
| Validar publicación | `GET /api/v1/posts/{id}` | Antes de crear un like o comentario |
| Actualizar contadores | `PATCH /api/v1/internal/posts/{id}/counters` | Al crear o eliminar likes o comentarios |

Request de contadores:

```json
{
  "likeDelta": 1,
  "commentDelta": 0
}
```

La operación de creación o eliminación se revierte si la llamada requerida a
`ms-posts` falla. La eliminación provocada por `post.delete` no depende de este
cliente HTTP: usa los servicios locales de likes y comentarios.

## Configuración

| Variable | Descripción |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Perfil activo, normalmente `prod` |
| `DB_URL` | JDBC con `currentSchema=heypudu_interactions` |
| `DB_USERNAME` | Usuario de PostgreSQL |
| `DB_PASSWORD` | Contraseña de PostgreSQL |
| `COGNITO_ISSUER_URI` | Issuer del User Pool de Cognito |
| `COGNITO_JWK_SET_URI` | URL JWKS de Cognito |
| `CLIENTS_POSTS_BASE_URL` | URL base de `ms-posts` |
| `RABBITMQ_HOST` | Host de RabbitMQ |
| `RABBITMQ_PORT` | Puerto AMQP, normalmente `5672` |
| `RABBITMQ_USERNAME` | Usuario AMQP |
| `RABBITMQ_PASSWORD` | Contraseña AMQP |
| `RABBITMQ_VHOST` | Virtual host AMQP |
| `RABBIT_MSG_TTL_MS` | TTL de mensajes |
| `RABBIT_MAX_LENGTH` | Máximo de mensajes por cola |
| `RABBIT_DELIVERY_LIMIT` | Límite de entregas |

No se deben versionar contraseñas, tokens ni archivos `.env` reales.

## Ejecución local

### Requisitos

- JDK 25.
- Docker y Docker Compose.
- PostgreSQL local o la base definida en `DB_URL`.
- RabbitMQ local o accesible mediante las variables `RABBITMQ_*`.
- `ms-posts` ejecutándose en `:8083` para validar publicaciones y ajustar
  contadores.

Desde `heyPudu-backend/` se puede levantar RabbitMQ con:

```bash
docker compose -f docker-compose.rabbit.yml up -d
```

Luego, desde este directorio:

```bash
./gradlew bootRun
```

Verificar:

```bash
curl http://localhost:8084/actuator/health
```

## Flujos principales

### Dar like

```text
[POST /api/v1/posts/{postId}/likes]
        |
        v
[Validar JWT y extraer userId]
        |
        v
[Comprobar like existente]
        |
        +--> Existe: devolver el existente
        |
        `--> Validar post en ms-posts
              |
              +--> Guardar like
              +--> Actualizar like_count en ms-posts
              `--> Publicar interaction.liked
```

### Crear comentario

```text
[POST /api/v1/posts/{postId}/comments]
        |
        v
[Validar JWT, contenido y existencia del post]
        |
        v
[Guardar comentario]
        |
        +--> Actualizar comment_count en ms-posts
        `--> Publicar interaction.commented
```

### Eliminar publicación

```text
[ms-posts publica post.delete]
        |
        v
[PostCommandListener recibe el comando]
        |
        +--> Eliminar comentarios del post
        +--> Eliminar likes del post
        `--> ACK o NACK hacia RabbitMQ
```

## Decisiones de diseño

| Decisión | Razón |
|---|---|
| Sin FK física hacia `posts` o `users` | Los datos viven en otras bases |
| Validar el post antes de crear interacción | Evita interacciones huérfanas |
| `POST /likes` idempotente | Evita duplicados ante reintentos |
| `DELETE /likes` idempotente | Repetir la operación no genera error |
| Contadores en `ms-posts` | Lectura rápida del feed |
| `post.delete` por RabbitMQ | Desacopla la limpieza de interacciones |
| ACK manual y DLQ | Hace visible el fallo y evita pérdida silenciosa |
| `RestClient` para operaciones síncronas | Adecuado para validar y actualizar contadores |

## Trabajo futuro

- Token de servicio para proteger los endpoints internos.
- Retry y circuit breaker para llamadas a `ms-posts`.
- Comentarios de audio y edición de comentarios.
- Reacciones adicionales al like.
- Observabilidad centralizada de eventos y DLQ.

## Autor

Proyecto desarrollado para la asignatura **DSY1107 - Desarrollo Cloud Native**.
