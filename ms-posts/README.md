# ms-posts

Microservicio de publicaciones y feed para **heyPudú!**.

## Responsabilidad

`ms-posts` gestiona:

- Publicaciones de texto y audio (briefcasts).
- Feed general, feed personalizado y búsqueda.
- URLs prefirmadas de S3 para audio y portadas.
- Likes y comentarios mediante contadores desnormalizados.
- Reportes de publicaciones y operaciones administrativas.
- Publicación del comando `post.delete` para eliminar interacciones de forma
  asíncrona.
- Consumo de eventos de interacción y seguimiento para auditoría en logs.

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
| Comunicación HTTP | RestClient hacia `ms-users` |
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
[ms-posts :8083] ---> [ms-users :8082]
        |
        +--> [Neon PostgreSQL: heypudu_posts]
        +--> [S3: audio y portadas]
        `--> [RabbitMQ: comando post.delete y auditoría]
```

- **Puerto:** `8083`
- **Esquema de BD:** `heypudu_posts`
- **Buckets S3:** `heypudu-audio` y `heypudu-covers`
- **Dependencia HTTP:** `ms-users` para datos del autor, seguidos y contadores.
- **Dependencia de mensajería:** RabbitMQ para el comando de eliminación y
  eventos de auditoría.

## Mensajería RabbitMQ

### Publicación del comando `post.delete`

Al eliminar una publicación, `ms-posts`:

1. Verifica la autoría o los permisos administrativos.
2. Publica `post.delete` en `ex.heypudu.cmd`.
3. Elimina los archivos asociados de S3.
4. Elimina la publicación de `heypudu_posts`.
5. Ajusta el contador de publicaciones del usuario mediante `ms-users`.

El consumidor de `ms-interactions` recibe el comando desde
`q.heypudu.command` y elimina los likes y comentarios asociados. La respuesta
HTTP no espera ese procesamiento; la limpieza de interacciones es asíncrona.

Si RabbitMQ no está disponible al publicar el comando, la operación de
eliminación falla y la transacción no continúa.

### Consumo de auditoría

`AuditEventListener` consume `q.heypudu.audit`, recibe eventos publicados en
`ex.heypudu.events` y registra en los logs los eventos de likes, comentarios y
seguimientos. Utiliza ACK manual; ante un error envía el mensaje a la DLQ
correspondiente.

| Elemento | Valor |
|---|---|
| Exchange de comandos | `ex.heypudu.cmd` |
| Exchange de eventos | `ex.heypudu.events` |
| Exchange de errores | `ex.heypudu.dlx` |
| Cola de comandos | `q.heypudu.command` |
| Cola de auditoría | `q.heypudu.audit` |
| DLQ de auditoría | `q.heypudu.audit.dlq` |
| Comando publicado | `post.delete` |
| Eventos auditados | `interaction.*` y `user.#` |
| ACK/NACK | ACK manual; rechazo sin reencolado |

La topología completa, TTL, límite de mensajes y límite de entregas se
configuran en la infraestructura RabbitMQ compartida del backend.

## Estructura del proyecto

```text
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
    │   │   ├── S3Config.java
    │   │   └── amqp/
    │   ├── controller/
    │   │   ├── PostController.java
    │   │   ├── FeedController.java
    │   │   ├── PostReportController.java
    │   │   └── AdminPostController.java
    │   ├── client/
    │   │   └── UserClient.java
    │   ├── messaging/
    │   │   ├── PostEventPublisher.java
    │   │   └── AuditEventListener.java
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
            ├── V1__init_posts_schema.sql
            ├── V2__allow_audio_description.sql
            ├── V3__init_post_reports.sql
            ├── V4__allow_repeat_reports.sql
            └── V5__add_author_name.sql
```

## Endpoints

### Publicaciones y feeds

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/v1/posts` | Feed general paginado |
| `GET` | `/api/v1/posts/{id}` | Ver una publicación |
| `GET` | `/api/v1/posts/me` | Publicaciones propias |
| `GET` | `/api/v1/posts/author/{authorId}` | Publicaciones de un autor |
| `POST` | `/api/v1/posts` | Crear publicación |
| `PUT` | `/api/v1/posts/{id}` | Editar publicación propia |
| `DELETE` | `/api/v1/posts/{id}` | Eliminar publicación propia |
| `GET` | `/api/v1/posts/feed` | Feed personalizado |
| `GET` | `/api/v1/posts/search?q=` | Buscar publicaciones |

Todos requieren un JWT válido de Cognito.

### S3 y reportes

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/v1/posts/audio/presigned-url` | URL prefirmada para audio |
| `POST` | `/api/v1/posts/covers/presigned-url` | URL prefirmada para portada |
| `POST` | `/api/v1/posts/{postId}/reports` | Reportar una publicación |
| `GET` | `/api/v1/admin/posts/reports` | Listar reportes abiertos |
| `PUT` | `/api/v1/admin/posts/reports/{reportId}/{status}` | Cambiar estado de reporte |

Los endpoints administrativos requieren `ROLE_ADMIN`.

### Administración e internos

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/v1/admin/posts/audio` | Listar publicaciones de audio |
| `DELETE` | `/api/v1/admin/posts/{postId}` | Eliminar una publicación como admin |
| `GET` | `/api/v1/internal/posts/reports/counts` | Contadores de reportes por autor |
| `PATCH` | `/api/v1/internal/posts/{id}/counters` | Actualizar contadores |
| `DELETE` | `/api/v1/internal/posts/users/{userId}` | Anonimizar publicaciones de un usuario |

Los endpoints internos son consumidos por otros servicios y no forman parte
del flujo público del frontend.

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
| `type` | `VARCHAR(20)` | `TEXT` o `AUDIO` |
| `title` | `VARCHAR(50)` | `NOT NULL` |
| `content` | `TEXT` | |
| `audio_key` | `VARCHAR(500)` | |
| `audio_duration_seconds` | `INTEGER` | Entre 1 y 300 segundos |
| `cover_image_key` | `VARCHAR(500)` | |
| `like_count` | `INTEGER` | `NOT NULL DEFAULT 0` |
| `comment_count` | `INTEGER` | `NOT NULL DEFAULT 0` |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL` |

Las tablas de reportes se crean mediante `V3__init_post_reports.sql` y sus
ajustes posteriores.

## Flujo de subida de audio

```text
[React solicita URL prefirmada]
        |
        v
[ms-posts genera URL de S3 válida por 15 minutos]
        |
        v
[React sube el audio directamente a S3 mediante PUT]
        |
        v
[React crea el post con audioKey y duración]
        |
        v
[ms-posts valida, obtiene el autor desde ms-users y guarda en BD]
```

El backend nunca recibe el binario. Solo almacena la key de S3 y genera URLs
temporales para su lectura.

## Flujo de eliminación de publicación

```text
[DELETE /api/v1/posts/{id}]
        |
        v
[ms-posts verifica autoría o rol ADMIN]
        |
        +--> Publica post.delete en RabbitMQ
        |        |
        |        `--> ms-interactions elimina likes y comentarios
        |
        +--> Elimina audio y portada de S3
        |
        +--> Elimina la publicación de PostgreSQL
        |
        `--> Ajusta el contador en ms-users
```

La eliminación de interacciones no es una llamada REST síncrona. Si el
consumidor falla, el mensaje se enruta según la política de reintentos y DLQ
de RabbitMQ.

## Comunicación con otros servicios

### `ms-users`

`UserClient` utiliza HTTP para:

- Obtener los datos del autor.
- Obtener los IDs de usuarios seguidos para el feed personalizado.
- Ajustar el contador de publicaciones después de crear o eliminar.

Base URL: `CLIENTS_USERS_BASE_URL`.

### `ms-interactions`

No existe una llamada REST para borrar interacciones desde `ms-posts`. La
coordinación se realiza mediante el comando RabbitMQ `post.delete`, consumido
por `ms-interactions`.

## Configuración

| Variable | Descripción |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Perfil activo, normalmente `prod` |
| `DB_URL` | JDBC con `currentSchema=heypudu_posts` |
| `DB_USERNAME` | Usuario de PostgreSQL |
| `DB_PASSWORD` | Contraseña de PostgreSQL |
| `COGNITO_ISSUER_URI` | Issuer del User Pool de Cognito |
| `COGNITO_JWK_SET_URI` | URL JWKS de Cognito |
| `CLIENTS_USERS_BASE_URL` | URL base de `ms-users` |
| `RABBITMQ_HOST` | Host de RabbitMQ |
| `RABBITMQ_PORT` | Puerto AMQP, normalmente `5672` |
| `RABBITMQ_USERNAME` | Usuario AMQP |
| `RABBITMQ_PASSWORD` | Contraseña AMQP |
| `RABBITMQ_VHOST` | Virtual host AMQP |
| `RABBIT_MSG_TTL_MS` | TTL de mensajes |
| `RABBIT_MAX_LENGTH` | Máximo de mensajes por cola |
| `RABBIT_DELIVERY_LIMIT` | Límite de entregas |
| `AWS_REGION` | Región de AWS |
| `S3_AUDIO_BUCKET` | Bucket de audio |
| `S3_COVERS_BUCKET` | Bucket de portadas |

Los valores por defecto están pensados para desarrollo local. Nunca se deben
versionar contraseñas, tokens ni archivos `.env` reales.

## Ejecución local

### Requisitos

- JDK 25.
- Docker y Docker Compose.
- PostgreSQL local o la base de datos definida en `DB_URL`.
- RabbitMQ local o accesible desde las variables `RABBITMQ_*`.
- Credenciales AWS configuradas para generar URLs de S3.
- `ms-users` ejecutándose en `:8082` para los flujos que requieren datos de
  usuarios.

Desde `heyPudu-backend/` se puede levantar la infraestructura compartida con:

```bash
docker compose -f docker-compose.rabbit.yml up -d
```

Luego, desde este directorio:

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
| El audio nunca pasa por el backend | Se sube directamente a S3 con URL prefirmada |
| La BD guarda keys, no binarios | Evita almacenar archivos grandes en PostgreSQL |
| Duplicación mínima del autor | Evita una llamada HTTP en cada lectura |
| Contadores desnormalizados | Acelera la lectura del feed |
| `post.delete` por RabbitMQ | Desacopla la limpieza de interacciones |
| ACK manual y DLQ | Evita pérdida silenciosa de mensajes |
| Hard delete de publicaciones | La publicación deja de estar disponible inmediatamente |
| Anonimización de usuarios | Conserva referencias cuando corresponde |

## Trabajo futuro

- Token de servicio o autenticación específica para endpoints internos.
- Búsqueda avanzada con `pg_trgm` o `tsvector`.
- Retry y circuit breaker para llamadas HTTP a `ms-users`.
- Observabilidad centralizada de comandos y eventos RabbitMQ.
- Funcionalidades futuras de publicación, como temporadas o reacciones.

## Autor

Proyecto desarrollado para la asignatura **DSY1107 - Desarrollo Cloud Native**.
