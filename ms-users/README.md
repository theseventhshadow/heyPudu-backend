# ms-users

Microservicio de perfiles, relaciones sociales y notificaciones para
**heyPudú!**.

## Responsabilidad

`ms-users` gestiona:

- Creación y edición de perfiles.
- Perfiles públicos, búsqueda y relaciones de seguimiento.
- Contadores de seguidores, seguidos y publicaciones.
- Baneo, desbaneo, verificación y desverificación administrativa.
- URLs prefirmadas de S3 para avatares.
- Persistencia y consulta de notificaciones recibidas por RabbitMQ.
- Publicación del evento `user.followed`.

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
| Comunicación HTTP | RestClient hacia `ms-auth` y clientes internos |
| Mensajería | RabbitMQ mediante Spring AMQP |
| Almacenamiento | AWS S3 con URLs prefirmadas |
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
[ms-users :8082] ---> [ms-auth :8081]
        |
        +--> [Neon PostgreSQL: heypudu_users]
        +--> [S3: heypudu-avatars]
        `--> [RabbitMQ: eventos y notificaciones]
```

- **Puerto:** `8082`
- **Esquema de BD:** `heypudu_users`
- **Bucket S3:** `heypudu-avatars`
- **Dependencia HTTP:** `ms-auth` para operaciones administrativas de Cognito.
- **Dependencia de mensajería:** RabbitMQ para notificaciones y eventos de
  seguimiento.

## Mensajería RabbitMQ

### Consumir notificaciones

`NotificationListener` consume `q.heypudu.notification`, enlazada a
`ex.heypudu.events` para recibir:

- `interaction.liked`
- `interaction.commented`
- `user.followed`

Para cada evento, `NotificationService`:

1. Valida el payload.
2. Traduce la routing key a un tipo de notificación.
3. Persiste la notificación en la tabla `notifications`.
4. Envía ACK manual.

Si ocurre un error, envía NACK sin reencolado y el mensaje queda en
`q.heypudu.notification.dlq`. `DeadLetterListener` registra el payload y los
metadatos de muerte.

### Publicar eventos de seguimiento

Cuando un usuario sigue a otro, `UserEventPublisher` publica
`user.followed` en `ex.heypudu.events`. El evento contiene:

- `eventId`.
- Fecha de creación.
- `actorId`: quien comenzó a seguir.
- `recipientId`: usuario seguido.
- `postId` y `commentId` nulos para este tipo de evento.

Los eventos de like y comentario son publicados por `ms-interactions`; este
servicio los consume para crear las notificaciones del destinatario.

| Elemento | Valor |
|---|---|
| Exchange de eventos | `ex.heypudu.events` |
| Exchange de errores | `ex.heypudu.dlx` |
| Cola de notificaciones | `q.heypudu.notification` |
| DLQ de notificaciones | `q.heypudu.notification.dlq` |
| Eventos consumidos | `interaction.liked`, `interaction.commented`, `user.followed` |
| Evento publicado | `user.followed` |
| ACK/NACK | ACK manual; rechazo sin reencolado |

## Estructura del proyecto

```text
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
    │   │   ├── S3Config.java
    │   │   └── amqp/
    │   ├── controller/
    │   │   ├── UserController.java
    │   │   ├── FollowController.java
    │   │   ├── NotificationController.java
    │   │   ├── AdminUserController.java
    │   │   └── InternalUserController.java
    │   ├── client/
    │   │   ├── AuthClient.java
    │   │   └── PostClient.java
    │   ├── messaging/
    │   │   ├── NotificationListener.java
    │   │   ├── UserEventPublisher.java
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
            ├── V1__init_users-schema.sql
            ├── V2__init_follows_table.sql
            └── V3__init_notifications_table.sql
```

## Endpoints

### Perfil y relaciones

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/v1/users/me` | Ver perfil propio |
| `PUT` | `/api/v1/users/me` | Editar perfil propio |
| `DELETE` | `/api/v1/users/me` | Anonimizar y eliminar cuenta |
| `GET` | `/api/v1/users/{id}` | Ver perfil público |
| `GET` | `/api/v1/users/{id}/followers` | Listar seguidores |
| `GET` | `/api/v1/users/{id}/following` | Listar seguidos |
| `POST` | `/api/v1/users/{id}/follow` | Seguir |
| `DELETE` | `/api/v1/users/{id}/follow` | Dejar de seguir |
| `GET` | `/api/v1/users/search?q=` | Buscar usuarios |

### Notificaciones

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/v1/notifications` | Listar notificaciones paginadas |
| `GET` | `/api/v1/notifications?unreadOnly=true` | Listar solo no leídas |
| `PATCH` | `/api/v1/notifications/{id}/read` | Marcar una notificación como leída |
| `POST` | `/api/v1/notifications/read-all` | Marcar todas como leídas |

### Almacenamiento

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/v1/users/me/avatar/presigned-url` | URL prefirmada para avatar |
| `GET` | `/api/v1/users/by-cognito/{cognitoSub}/avatar` | URL de avatar por Cognito |

### Administración

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/v1/admin/users` | Listar usuarios |
| `GET` | `/api/v1/admin/users/ban-candidates` | Listar candidatos a baneo |
| `PUT` | `/api/v1/admin/users/{id}/ban` | Banear usuario |
| `PUT` | `/api/v1/admin/users/{id}/unban` | Desbanear usuario |
| `PUT` | `/api/v1/admin/users/{id}/verify` | Verificar usuario |
| `PUT` | `/api/v1/admin/users/{id}/unverify` | Desverificar usuario |

Las operaciones administrativas requieren `ROLE_ADMIN`.

### Internos

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/v1/internal/users` | Crear perfil base |
| `GET` | `/api/v1/internal/users/{id}/summary` | Obtener resumen |
| `GET` | `/api/v1/internal/users/{id}/following/ids` | Obtener IDs seguidos |
| `GET` | `/api/v1/internal/users/{id}/exists` | Verificar existencia |
| `PUT` | `/api/v1/internal/users/by-cognito/{cognitoSub}/post-count` | Ajustar contador de posts |

### Salud

| Método | Endpoint |
|---|---|
| `GET` | `/actuator/health` |
| `GET` | `/actuator/info` |

## Modelo de datos

### Tabla `users`

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | `UUID` | `PRIMARY KEY` |
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
| `id` | `UUID` | `PRIMARY KEY` |
| `follower_id` | `UUID` | `NOT NULL` |
| `followed_id` | `UUID` | `NOT NULL` |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` |

Restricciones: `UNIQUE (follower_id, followed_id)` y
`CHECK (follower_id <> followed_id)`.

### Tabla `notifications`

La migración `V3__init_notifications_table.sql` crea la tabla para eventos de
likes, comentarios y follows.

| Campo | Descripción |
|---|---|
| `id` | Identificador interno |
| `event_id` | Identificador único del evento RabbitMQ |
| `recipient_id` | Usuario que recibe la notificación |
| `actor_id` | Usuario que originó la acción |
| `type` | Tipo derivado de la routing key |
| `post_id` | Publicación relacionada, si corresponde |
| `comment_id` | Comentario relacionado, si corresponde |
| `message` | Texto mostrado al usuario |
| `is_read` | Estado de lectura |
| `created_at` | Fecha del evento |

## Reglas de negocio

| Regla | Detalle |
|---|---|
| `username` único | Validación en creación y edición |
| Cambio de `username` | Una vez cada 30 días |
| No auto-seguimiento | Validación de servicio y restricción en BD |
| Follow idempotente | Repetir el seguimiento no duplica la relación |
| Unfollow idempotente | Quitar una relación inexistente no falla |
| Banear | `status = BANNED` y deshabilitar en Cognito |
| Desbanear | `status = ACTIVE` y habilitar en Cognito |
| Eliminar cuenta | Anonimización local y eliminación en Cognito |
| Notificaciones | Solo se consultan por el usuario destinatario |

## Flujos principales

### Creación de perfil tras el registro

```text
[Cognito crea la cuenta]
        |
        v
[React llama a POST /api/v1/auth/sync]
        |
        v
[ms-auth crea el registro y llama a ms-users]
        |
        v
[ms-users crea el perfil base]
        |
        v
[El usuario completa name y username]
```

### Seguir a otro usuario

```text
[POST /api/v1/users/{id}/follow]
        |
        v
[Guardar follow y actualizar contadores]
        |
        v
[Publicar user.followed en RabbitMQ]
        |
        v
[ms-users consume el evento en q.heypudu.notification]
        |
        v
[Persistir la notificación del usuario seguido]
```

### Recibir una notificación de interacción

```text
[ms-interactions publica interaction.liked o interaction.commented]
        |
        v
[q.heypudu.notification]
        |
        v
[NotificationListener valida y persiste]
        |
        v
[GET /api/v1/notifications]
```

### Baneo

```text
[Admin solicita PUT /api/v1/admin/users/{id}/ban]
        |
        v
[ms-users cambia status a BANNED]
        |
        v
[ms-users llama a ms-auth]
        |
        v
[ms-auth deshabilita al usuario en Cognito]
```

## Configuración

| Variable | Descripción |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Perfil activo, normalmente `prod` |
| `DB_URL` | JDBC con `currentSchema=heypudu_users` |
| `DB_USERNAME` | Usuario de PostgreSQL |
| `DB_PASSWORD` | Contraseña de PostgreSQL |
| `COGNITO_ISSUER_URI` | Issuer del User Pool de Cognito |
| `COGNITO_JWK_SET_URI` | URL JWKS de Cognito |
| `CLIENTS_AUTH_BASE_URL` | URL base de `ms-auth` |
| `CLIENTS_POSTS_ENABLED` | Habilita el cliente opcional de posts |
| `CLIENTS_POSTS_BASE_URL` | URL base de `ms-posts` |
| `CLIENTS_INTERACTIONS_BASE_URL` | URL base de `ms-interactions` |
| `RABBITMQ_HOST` | Host de RabbitMQ |
| `RABBITMQ_PORT` | Puerto AMQP, normalmente `5672` |
| `RABBITMQ_USERNAME` | Usuario AMQP |
| `RABBITMQ_PASSWORD` | Contraseña AMQP |
| `RABBITMQ_PREFETCH` | Prefetch del consumidor |
| `RABBITMQ_MESSAGE_TTL_MS` | TTL de mensajes |
| `RABBITMQ_MAX_LENGTH` | Máximo de mensajes por cola |
| `RABBITMQ_DELIVERY_LIMIT` | Límite de entregas |
| `AWS_REGION` | Región de AWS |
| `S3_AVATARS_BUCKET` | Bucket de avatares |

No se deben versionar contraseñas, tokens ni archivos `.env` reales.

## Ejecución local

### Requisitos

- JDK 25.
- Docker y Docker Compose.
- PostgreSQL local o la base definida en `DB_URL`.
- RabbitMQ local o accesible mediante las variables `RABBITMQ_*`.
- Credenciales AWS para generar URLs de S3.
- `ms-auth` ejecutándose en `:8081` para sincronización y administración.

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
curl http://localhost:8082/actuator/health
```

## Decisiones de diseño

| Decisión | Razón |
|---|---|
| `cognito_sub` como identidad externa | Mantiene la relación con Cognito |
| `avatar_key` en lugar de URL | Permite regenerar URLs temporales |
| Banear no elimina datos | Permite desbanear sin pérdida |
| Anonimización al eliminar | Evita referencias rotas en otras BD |
| Contadores desnormalizados | Lectura rápida del perfil |
| Eventos RabbitMQ | Desacopla notificaciones de las operaciones principales |
| ACK manual y DLQ | Hace visibles los fallos de consumo |

## Trabajo futuro

- Token de servicio para proteger endpoints internos.
- Activar el cliente de posts si se requiere sincronización adicional de
  perfiles.
- Retry y circuit breaker para clientes HTTP.
- Eliminación diferida de cuentas con posibilidad de cancelación.
- Búsqueda avanzada con `pg_trgm`.

## Autor

Proyecto desarrollado para la asignatura **DSY1107 - Desarrollo Cloud Native**.
