# heyPudú! — Backend

Backend del proyecto **heyPudú!**, una red social de audio breve ("briefcasts") con texto, audio, likes, comentarios y perfiles. Construido con una arquitectura de microservicios en Spring Boot, desplegado en AWS y autenticado con Cognito.

Este README es la **fuente de verdad** del backend. Debe contener todo lo necesario para que cualquier desarrollador (o IA) pueda retomar el trabajo sin contexto adicional.

---

## Índice

1. [Descripción del proyecto](#descripción-del-proyecto)
2. [Arquitectura general](#arquitectura-general)
3. [Microservicios](#microservicios)
4. [RabbitMQ](#rabbitmq)
5. [Kafka y salas en vivo](#kafka-y-salas-en-vivo)
6. [Decisiones de diseño](#decisiones-de-diseño)
7. [Estructura del repositorio](#estructura-del-repositorio)
8. [Convenciones](#convenciones)
9. [Modelo de datos](#modelo-de-datos)
10. [Seguridad y autenticación](#seguridad-y-autenticación)
11. [Comunicación entre microservicios](#comunicación-entre-microservicios)
12. [Almacenamiento de archivos](#almacenamiento-de-archivos)
13. [Configuración y entornos](#configuración-y-entornos)
14. [Ejecución local](#ejecución-local)
15. [Despliegue](#despliegue)
16. [CI/CD](#cicd)
17. [Estado actual del proyecto](#estado-actual-del-proyecto)
18. [Trabajo futuro](#trabajo-futuro)

---

## Descripción del proyecto

**heyPudú!** es una red social de audio breve. Los usuarios pueden:

- Publicar **briefcasts**: audios de máximo 5 minutos, con título, portada y texto descriptivo opcionales.
- Dar "me gusta" y comentar publicaciones.
- Seguir a otros usuarios.
- Ver y editar su perfil.
- Buscar usuarios y publicaciones.

Los usuarios se autentican con **AWS Cognito**. Los administradores pueden banear o verificar cuentas.

---

## Arquitectura general

```
┌──────────────────────────────┐
│  Frontend React + Amplify    │
│  (repositorio separado)      │
└──────────────┬───────────────┘
               │ JWT
               ▼
┌──────────────────────────────┐
│     AWS API Gateway          │
│  (valida JWT contra Cognito) │
└──────────────┬───────────────┘
               │
   ┌───────────┼───────────┼───────────┬───────────────┐
   ▼           ▼           ▼           ▼               ▼
┌────────┐ ┌────────┐ ┌─────────┐ ┌──────────────┐ ┌──────────────┐
│ms-auth │ │ms-users│ │ms-posts │ │ms-interactions│ │rabbit-admin  │
│ :8081  │ │ :8082  │ │ :8083   │ │ :8084         │ │ :8085        │
└────┬───┘ └──┬─────┘ └────┬────┘ └──────┬────────┘ └──────┬───────┘
     │         │            │             │                 │
     └─────────┴────────────┴─────────────┴─────────────────┘
                           │
                           ▼
                 ┌────────────────────┐
                 │ EC2 RabbitMQ        │
                 │ rabbit1 + rabbit2   │
                 │ quorum queues + DLQ │
                 └────────────────────┘

  Cada microservicio utiliza su esquema PostgreSQL en Neon.
  ms-posts, ms-users y ms-interactions publican o consumen mensajes AMQP.
  ms-heypudu-rabbit-admin administra la topología mediante RabbitAdmin.

                     ┌────────────┐
                     │  AWS S3    │
                     │ audio,     │
                     │ portadas,  │
                     │ avatares   │
                     └────────────┘
```

**Tecnologías transversales:**

- **Java 25**
- **Spring Boot 3.5.x**
- **Gradle** (cada microservicio con su propio `build.gradle`)
- **PostgreSQL** (instancia administrada en Neon, esquemas separados)
- **AWS Cognito** (autenticación)
- **AWS S3** (almacenamiento de audio y portadas)
- **AWS API Gateway** (enrutamiento y validación de JWT)
- **AWS EC2** (despliegue de microservicios vía Docker)
- **RabbitMQ 4** (clúster de dos nodos, exchanges, colas quorum y DLQ)
- **GitHub Actions** (CI/CD)

---

## Microservicios

| Microservicio | Puerto | Esquema de BD | Responsabilidad |
|---|---|---|---|
| `ms-auth` | 8081 | `heypudu_auth` | Sincronización con Cognito, datos del usuario autenticado. |
| `ms-users` | 8082 | `heypudu_users` | Perfil, seguir/dejar de seguir, búsqueda de usuarios, administración. |
| `ms-posts` | 8083 | `heypudu_posts` | Publicaciones (texto/audio), feed, búsqueda de posts, URLs prefirmadas S3. |
| `ms-interactions` | 8084 | `heypudu_interactions` | Likes, comentarios, reportes y mensajería de comandos/eventos. |
| `ms-heypudu-rabbit-admin` | 8085 | — | Administración REST de exchanges, colas y bindings RabbitMQ. |
| `ms-live` | 8086 | `heypudu_live` | Salas de audio en vivo, acceso por seguimiento, reacciones y eventos Kafka. |
| `ms-analytics` | 8087 | — (métricas en memoria) | Métricas en tiempo real desde Kafka: oyentes activos, reacciones y publicaciones. |
| `ms-kafka-admin` | 8088 | — | Administración de tópicos, configuraciones y grupos de consumo de Kafka. |

Cada microservicio es un **proyecto Spring Boot independiente** con su propio `build.gradle`, su propia clase `@SpringBootApplication`, su propio `Dockerfile` y su propia configuración.

---

## RabbitMQ

RabbitMQ se ejecuta en una EC2 separada de la EC2 backend mediante
[`docker-compose.rabbit.yml`](./docker-compose.rabbit.yml). El clúster tiene
dos nodos:

- `rabbit@rabbit1`: AMQP `5672`, Management UI `15672`.
- `rabbit@rabbit2`: AMQP interno, Management UI publicada de forma
  independiente para comprobaciones.

La EC2 backend se conecta por la IP privada o DNS privado de RabbitMQ. El
puerto AMQP `5672` se permite únicamente desde el Security Group del backend.
La Management UI no se expone públicamente; para una demostración se utiliza
un túnel SSH.

### Topología

| Exchange | Tipo | Cola | Routing key o patrón |
|---|---|---|---|
| `ex.heypudu.cmd` | `direct` | `q.heypudu.command` | `post.delete` |
| `ex.heypudu.cmd` | `direct` | `q.heypudu.live.command` | `room.closed` |
| `ex.heypudu.events` | `topic` | `q.heypudu.notification` | `interaction.liked`, `interaction.commented`, `user.followed`, `room.created` |
| `ex.heypudu.events` | `topic` | `q.heypudu.audit` | `interaction.*`, `user.#` |
| `ex.heypudu.dlx` | `direct` | Cuatro DLQ | Routing key específica de cada DLQ |

Las cuatro colas funcionales y sus DLQ son colas quorum durables. Utilizan TTL
predeterminado de una hora, longitud máxima de `10000`, overflow
`rejectPublish`, límite de entregas `3` y dead-letter exchange
`ex.heypudu.dlx`.

### Flujos

- `ms-posts` publica `post.delete`; `ms-interactions` elimina likes y
  comentarios y confirma con ACK.
- `ms-interactions` publica eventos de likes y comentarios.
- `ms-users` publica eventos de seguimiento y consume notificaciones.
- `ms-live` publica `room.created` y `room.closed`; `ms-users` notifica a los
  seguidores y `ms-live` limpia las reacciones de la sala cerrada.
- `ms-posts` consume eventos de auditoría.
- Los consumidores usan ACK manual y NACK sin requeue ante errores no
  recuperables.
- Los listeners de DLQ registran los mensajes rechazados de las cuatro colas.

La topología está centralizada en clases `RabbitTopology` y
`RabbitTopologyConfig`. La configuración de escucha y el conversor de mensajes
se mantienen separados de la lógica de negocio.

---

## Kafka y salas en vivo

Las salas de audio en vivo usan tres piezas que se despliegan en una **EC2
propia** (`docker-compose.live.prod.yml`), separada de la EC2 del backend y de
la de RabbitMQ:

- **MediaMTX**: SFU WebRTC. Se publica con WHIP y se escucha con WHEP.
- **ms-live**: ciclo de vida de las salas, autorización de MediaMTX y emisión
  de eventos.
- **ms-analytics**: métricas en tiempo real y auditoría de eventos.
- **Kafka** en modo KRaft y **Caddy** (TLS para WHIP/WHEP) completan el stack.

```
Navegador ──WHIP/WHEP (HTTPS)──► Caddy ──► MediaMTX :8889
     │                                       │
     │                        hooks runOnRead / runOnUnread
     │                                       ▼
     └──REST──► API Gateway ──► ALB ──► ms-live :8086 ──► Kafka
                                                       │
                                            ms-analytics :8087
                                            (dos grupos de consumo)

  Los medios WebRTC (8189/UDP) van directo del navegador a MediaMTX.
```

### Tópico y eventos

| Elemento | Valor |
|---|---|
| Tópico | `heypudu.live.events` |
| Particiones | 3 |
| Factor de réplica | 1 |
| Retención | 7 días |
| Auto-create | Desactivado: `kafka-init` declara el tópico |

Tipos de evento (`LiveEventType`): `ROOM_CREATED`, `LISTENER_JOINED`,
`LISTENER_LEFT` y `REACTION`. Cada mensaje es un `LiveRoomEvent` con
`eventId`, `roomId`, `creatorCognitoSub`, `actorCognitoSub`, `reactionType`,
`occurredAt` y `schemaVersion`. La clave del mensaje es el `roomId`, de modo
que todos los eventos de una sala caen en la misma partición y se consumen en
orden.

### Grupos de consumidores

| Grupo | Servicio | Garantía |
|---|---|---|
| `live-analytics-group` | `ms-analytics` | Best-effort: ACK manual; si el procesamiento falla se registra y se continúa. |
| `live-audit-group` | `ms-analytics` | ACK solo cuando el evento queda guardado en `live_event_audit`; es idempotente por `eventId`. |
| `posts-analytics-group` | `ms-analytics` | Best-effort sobre `heypudu.posts.events`; deduplica por `eventId`. |

Los nombres se configuran con `KAFKA_CONSUMER_GROUP`, `KAFKA_AUDIT_GROUP` y
`KAFKA_POSTS_GROUP`.

El productor del segundo tópico vive en `ms-posts`, que corre en la EC2 del
backend y llega al clúster por el listener externo (`9192`/`9193`); `ms-live`,
`ms-analytics` y `ms-kafka-admin` lo hacen por el listener interno.

### Autorización de MediaMTX

MediaMTX usa `authMethod: http` y consulta a `ms-live` en cada publicación y
cada lectura:

- `publish`: solo el creador de la sala.
- `read`: el creador o un seguidor del creador, comprobado con `ms-users`
  (`/api/v1/internal/users/by-cognito/{sub}/followers/check`).
- Identidad: `user = cognitoSub` y `token`/`password` = JWT; el `sub` del token
  debe coincidir con `user`.

Los hooks `runOnRead` y `runOnUnread` envían `JOINED`/`LEFT` a
`/api/v1/internal/mediamtx/hooks` usando el `?user=<cognitoSub>` que el
frontend añade a la URL WHEP. **MediaMTX entrega `MTX_QUERY` con
`url.QueryEscape`**, así que `ms-live` decodifica el query antes de leer el
usuario; sin esa decodificación los hooks se descartan y el contador de
oyentes se queda en 0.

### Endpoints

| Método y ruta | Descripción |
|---|---|
| `POST /api/v1/live/rooms` | Crear la sala; el creador queda como dueño. |
| `GET /api/v1/live/rooms` | Salas activas con el creador resuelto. |
| `GET /api/v1/live/rooms/{id}` | Detalle con `whipUrl` y `whepUrl`. |
| `GET /api/v1/live/rooms/{id}/access` | `canListen` según el seguimiento. |
| `POST /api/v1/live/rooms/{id}/reactions` | Reacción; solo el creador o un seguidor. |
| `DELETE /api/v1/live/rooms/{id}` | Cerrar la sala y publicar `room.closed`. |
| `GET /api/v1/analytics/rooms/{id}/stats` | Oyentes activos y reacciones. |
| `GET /api/v1/analytics/posts/stats` | Publicaciones creadas y eliminadas. |
| `POST /api/v1/internal/mediamtx/auth` | Autorización de WHIP/WHEP (interno). |
| `POST /api/v1/internal/mediamtx/hooks` | Hooks de lectura (interno). |

Los endpoints `/api/v1/internal/**` están bloqueados en el ALB con una
respuesta fija `403`; solo son accesibles desde la red Docker o la IP privada.

### Administración del clúster

`ms-kafka-admin` expone el `AdminClient` de Kafka por REST, igual que
`ms-heypudu-rabbit-admin` hace con `RabbitAdmin`. Todas las rutas exigen
`ROLE_ADMIN` y están en `/api/v1/admin/kafka`:

| Método y ruta | Descripción |
|---|---|
| `GET /cluster` | Brokers vivos, controlador y `clusterId`. |
| `GET /topics` | Tópicos con particiones, factor de réplica e `internal`. |
| `GET /topics/{topic}` | Líder, réplicas e ISR por partición, más sus configuraciones. |
| `POST /topics` | Crea un tópico con particiones, RF, retención y cleanup policy. |
| `PATCH /topics/{topic}/config` | Cambia configuraciones de forma incremental. |
| `DELETE /topics/{topic}` | Elimina el tópico. |
| `GET /groups` | Lista los grupos de consumo. |
| `GET /groups/{group}` | Estado, miembros y LAG por partición. |

### Flujo de una sala

1. El creador crea la sala: `ROOM_CREATED` a Kafka y `room.created` a
   RabbitMQ, que notifica a sus seguidores.
2. El creador publica con WHIP; MediaMTX pide autorización a `ms-live`.
3. Un seguidor escucha con WHEP (`?user=<sub>`); el hook `runOnRead` publica
   `LISTENER_JOINED` y `ms-analytics` incrementa el contador.
4. Al salir, `runOnUnread` publica `LISTENER_LEFT`.
5. El creador cierra la sala: `ms-live` emite `room.closed`, elimina las
   reacciones y deja de aceptar publicaciones; el frontend refresca el estado
   y muestra el final a los oyentes sin necesidad de recargar.

### Variables de entorno

| Variable | Servicio | Uso |
|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | `ms-live`, `ms-analytics`, `ms-kafka-admin` | `broker1:9092,broker2:9092` dentro de Compose; `10.0.7.159:9192,10.0.7.159:9193` desde la EC2 del backend. |
| `KAFKA_TOPIC_LIVE_EVENTS` | `ms-live`, `ms-analytics` | Por defecto `heypudu.live.events`. |
| `KAFKA_TOPIC_POSTS_EVENTS` | `ms-posts`, `ms-analytics` | Por defecto `heypudu.posts.events`. |
| `KAFKA_CONSUMER_GROUP` / `KAFKA_AUDIT_GROUP` | `ms-analytics` | Grupos de las salas. |
| `KAFKA_POSTS_GROUP` | `ms-analytics` | Grupo de publicaciones (`posts-analytics-group`). |
| `KAFKA_EXTERNAL_HOST` | brokers | IP privada de la EC2 del clúster para el listener externo. |
| `MEDIAMTX_PUBLIC_URL` | `ms-live` | Base de `whipUrl` y `whepUrl`. |
| `MEDIAMTX_SERVICE_TOKEN` | `ms-live`, MediaMTX | Token compartido de los endpoints internos. |
| `CLIENTS_USERS_BASE_URL` | `ms-live` | IP privada del backend (`:8082`). |

### Despliegue

```bash
cp live-prod.env.example live-prod.env   # completar valores reales
docker compose --env-file live-prod.env -f docker-compose.live.prod.yml up -d
```

Las imágenes de `ms-live`, `ms-analytics` y MediaMTX se construyen en local y
se publican en Docker Hub: la EC2 no incluye `buildx`, así que `compose build`
falla allí. `live-prod.env` no se versiona.

El detalle de la infraestructura (Security Groups, ALB y sus reglas, Elastic
IP, TLS con Caddy y verificación) está en `../DESPLIEGUE-AWS.md` (§16 y §17).

---

## Decisiones de diseño

Estas decisiones son **vinculantes** para el proyecto. Cualquier IA o desarrollador que continúe debe respetarlas.

### Arquitectura

| Decisión | Razón |
|---|---|
| Microservicios separados | Requisito de la EP1. Cada servicio puede escalar y desplegarse de forma independiente. |
| Monorepo (todos los `ms-*` en un mismo repo) | Simplifica la gestión para un equipo pequeño. Cada carpeta es un proyecto Spring Boot autónomo. |
| Sin pom padre de Maven | Se usa Gradle. Cada microservicio tiene su propio build. |
| Base de datos por microservicio | Principio de database-per-service. PostgreSQL administrado en Neon con esquemas separados. |
| Sin BFF en la EP1 | El API Gateway cumple parcialmente ese rol. Se documenta como trabajo futuro. |
| RabbitMQ para mensajería asíncrona | Los comandos y eventos internos se desacoplan mediante exchanges, colas quorum, ACK/NACK y DLQ. |

### Modelo de datos

| Decisión | Razón |
|---|---|
| Roles solo `user` y `admin` en Cognito | La verificación no es un rol, es un atributo del perfil. |
| `is_verified` como atributo en `ms-users` | Es una cualidad del perfil, no un permiso. |
| Baneo = soft delete + anonimización | Evita referencias rotas en la BD. El usuario mantiene su `id`, pero sus datos personales se borran. |
| Duplicación mínima del autor en `ms-posts` | Se copia `author_id`, `author_username`, `author_avatar_url` al crear un post. No se sincroniza en tiempo real (trabajo futuro). |
| UUID como clave primaria | Se genera sin coordinación central. No expone IDs secuenciales. |
| `TIMESTAMPTZ` en PostgreSQL | Guarda zona horaria. Correcto para usuarios en múltiples regiones. |

### API

| Decisión | Razón |
|---|---|
| Versionado en la URL (`/api/v1/...`) | Visible y simple. |
| Paginación estándar de Spring Data | `?page=0&size=20&sort=createdAt,desc`. Formato `Page<T>` en la respuesta. |
| Endpoints en inglés | Convención estándar de la industria. |
| DTOs como `record` | Inmutables, concisos. |
| Entidades sin Lombok | Código explícito, sin depender de anotaciones mágicas. |
| `ddl-auto: validate` | Flyway gestiona el esquema, Hibernate solo valida. |

---

## Estructura del repositorio

```
heypudu-backend/
├── README.md                          # Este archivo
├── .gitignore                         # Ignora build/, .gradle/, etc.
├── docker-compose.yml                 # (Opcional) PostgreSQL local
├── ms-auth/                           # ✅ Completado
│   ├── build.gradle
│   ├── settings.gradle
│   ├── Dockerfile
│   ├── README.md
│   ├── .github/workflows/
│   │   └── deploy-ms-auth.yml
│   └── src/main/
│       ├── java/com/heypudu/ms_auth/
│       │   ├── MsAuthApplication.java
│       │   ├── config/
│       │   │   ├── SecurityConfig.java
│       │   │   └── JwtConfig.java
│       │   ├── controller/
│       │   │   └── AuthController.java
│       │   ├── dto/
│       │   │   ├── request/
│       │   │   │   └── SyncUserRequest.java
│       │   │   └── response/
│       │   │       ├── AuthUserResponse.java
│       │   │       └── SyncUserResponse.java
│       │   ├── exception/
│       │   │   ├── GlobalExceptionHandler.java
│       │   │   └── ResourceNotFoundException.java
│       │   ├── mapper/
│       │   │   └── AuthUserMapper.java
│       │   ├── model/
│       │   │   └── AuthUser.java
│       │   ├── repository/
│       │   │   └── AuthUserRepository.java
│       │   ├── security/
│       │   │   ├── JwtAuthConverter.java
│       │   │   └── CognitoUserExtractor.java
│       │   └── service/
│       │       ├── AuthService.java
│       │       └── impl/
│       │           └── AuthServiceImpl.java
│       └── resources/
│           ├── application.yml
│           ├── application-dev.yml
│           ├── application-prod.yml
│           └── db/migration/
│               └── V1__init_auth_schema.sql
│
├── docker-compose.prod.yml             # Producción: cinco microservicios
├── docker-compose.rabbit.yml           # Clúster RabbitMQ de dos nodos
├── docker-compose.kafka.yml            # ZooKeeper + 2 brokers + Kafka UI + init de tópicos
├── kafka/
│   └── init-topics.sh                  # Declaración idempotente del tópico
├── docker-compose.mediamtx.yml         # SFU WebRTC local (WHIP/WHEP)
├── mediamtx/
│   ├── Dockerfile                      # MediaMTX + curl y envsubst
│   ├── docker-entrypoint.sh            # Genera /mediamtx.yml desde la plantilla
│   ├── mediamtx.yml                    # Configuración local
│   └── mediamtx.prod.yml               # Plantilla de producción
├── docker-compose.live.prod.yml        # Producción de la EC2 de salas en vivo
├── live-prod.env.example               # Variables de esa EC2 (el .env real no se versiona)
├── caddy/
│   └── Caddyfile                       # TLS de WHIP/WHEP hacia MediaMTX
├── rabbit/
│   ├── cluster-init.sh                 # Unión idempotente de rabbit2 al clúster
│   └── conf/10-heypudu.conf            # Colas quorum por defecto
├── ms-users/                          # ✅ Completado
│   ├── build.gradle
│   ├── settings.gradle
│   ├── Dockerfile
│   ├── README.md
│   ├── .github/workflows/
│   │   └── deploy-ms-users.yml
│   └── src/main/
│       ├── java/com/heypudu/ms_users/
│       │   ├── MsUsersApplication.java
│       │   ├── config/
│       │   │   ├── SecurityConfig.java
│       │   │   ├── JwtConfig.java
│       │   │   └── RestClientConfig.java
│       │   ├── controller/
│       │   │   ├── UserController.java
│       │   │   └── AdminUserController.java
│       │   ├── dto/
│       │   │   ├── request/
│       │   │   │   ├── UpdateProfileRequest.java
│       │   │   │   └── CreateUserProfileRequest.java
│       │   │   └── response/
│       │   │       ├── UserProfileResponse.java
│       │   │       ├── UserSummaryResponse.java
│       │   │       └── FollowResponse.java
│       │   ├── exception/
│       │   │   ├── GlobalExceptionHandler.java
│       │   │   ├── ResourceNotFoundException.java
│       │   │   └── BusinessException.java
│       │   ├── mapper/
│       │   │   └── UserMapper.java
│       │   ├── model/
│       │   │   ├── User.java
│       │   │   └── Follow.java
│       │   ├── repository/
│       │   │   ├── UserRepository.java
│       │   │   └── FollowRepository.java
│       │   ├── security/
│       │   │   ├── JwtAuthConverter.java
│       │   │   └── CognitoUserExtractor.java
│       │   └── service/
│       │       ├── UserService.java
│       │       ├── FollowService.java
│       │       └── impl/
│       │           ├── UserServiceImpl.java
│       │           └── FollowServiceImpl.java
│       └── resources/
│           ├── application.yml
│           ├── application-dev.yml
│           ├── application-prod.yml
│           └── db/migration/
│               ├── V1__init_users_schema.sql
│               ├── V2__init_follows_table.sql
│               └── V3__init_notifications_table.sql
│
├── ms-posts/                          # ✅ Completado
│   ├── build.gradle
│   ├── settings.gradle
│   ├── Dockerfile
│   ├── README.md
│   ├── .github/workflows/
│   │   └── deploy-ms-posts.yml
│   └── src/main/
│       ├── java/com/heypudu/ms_posts/
│       │   ├── MsPostsApplication.java
│       │   ├── config/
│       │   │   ├── SecurityConfig.java
│       │   │   ├── JwtConfig.java
│       │   │   ├── RestClientConfig.java
│       │   │   └── S3Config.java
│       │   ├── controller/
│       │   │   ├── PostController.java
│       │   │   └── FeedController.java
│       │   ├── dto/
│       │   │   ├── request/
│       │   │   │   ├── CreatePostRequest.java
│       │   │   │   ├── UpdatePostRequest.java
│       │   │   │   └── PresignedUrlRequest.java
│       │   │   └── response/
│       │   │       ├── PostResponse.java
│       │   │       ├── PostSummaryResponse.java
│       │   │       └── PresignedUrlResponse.java
│       │   ├── exception/
│       │   │   ├── GlobalExceptionHandler.java
│       │   │   ├── ResourceNotFoundException.java
│       │   │   └── BusinessException.java
│       │   ├── mapper/
│       │   │   └── PostMapper.java
│       │   ├── model/
│       │   │   └── Post.java
│       │   ├── repository/
│       │   │   └── PostRepository.java
│       │   ├── security/
│       │   │   ├── JwtAuthConverter.java
│       │   │   └── CognitoUserExtractor.java
│       │   ├── service/
│       │   │   ├── PostService.java
│       │   │   ├── FeedService.java
│       │   │   ├── S3Service.java
│       │   │   └── impl/
│       │   │       ├── PostServiceImpl.java
│       │   │       ├── FeedServiceImpl.java
│       │   │       └── S3ServiceImpl.java
│       │   └── client/
│       │       └── UserClient.java
│       └── resources/
│           ├── application.yml
│           ├── application-dev.yml
│           ├── application-prod.yml
│           └── db/migration/
│               ├── V1__init_posts_schema.sql
│               ├── V2__allow_audio_description.sql
│               ├── V3__init_post_reports.sql
│               ├── V4__allow_repeat_reports.sql
│               └── V5__add_author_name.sql
│
├── ms-interactions/                   # ✅ Completado
    ├── build.gradle
    ├── settings.gradle
    ├── Dockerfile
    ├── README.md
    ├── .github/workflows/
    │   └── deploy-ms-interactions.yml
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
        │   ├── service/
        │   │   ├── LikeService.java
        │   │   ├── CommentService.java
        │   │   └── impl/
        │   │       ├── LikeServiceImpl.java
        │   │       └── CommentServiceImpl.java
        │   └── client/
        │       └── PostClient.java
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

```
└── ms-heypudu-rabbit-admin/             # ✅ Completado
    ├── build.gradle
    ├── Dockerfile
    ├── README.md
    └── src/main/java/com/heypudu/ms_rabbit_admin/
        ├── controller/RabbitAdminController.java
        ├── service/impl/RabbitAdminServiceImpl.java
        ├── config/
        └── dto/

```

```
├── ms-live/                             # ✅ Completado
│   ├── build.gradle
│   ├── Dockerfile
│   ├── README.md
│   └── src/
│       ├── main/java/com/heypudu/ms_live/
│       │   ├── MsLiveApplication.java
│       │   ├── client/                 # UserClient hacia ms-users
│       │   ├── config/                 # Seguridad, Kafka, RestClient
│       │   │   └── amqp/               # Topología y listeners RabbitMQ
│       │   ├── controller/             # LiveRoomController, MediamtxController
│       │   ├── dto/                    # Salas, reacciones y MediaMTX
│       │   ├── exception/
│       │   ├── messaging/              # Eventos Kafka y mensajes RabbitMQ
│       │   ├── model/                  # LiveRoom, LiveReaction
│       │   ├── repository/
│       │   ├── security/
│       │   └── service/                # LiveRoomService, MediamtxAuthService
│       ├── main/resources/
│       │   ├── application.yml
│       │   ├── application-dev.yml
│       │   ├── application-prod.yml
│       │   └── db/migration/           # V1 salas, V2 reacciones
│       └── test/java/com/heypudu/ms_live/service/
│           └── MediamtxAuthServiceTest.java
│
└── ms-analytics/                        # ✅ Completado
    ├── build.gradle
    ├── Dockerfile
    ├── README.md
    └── src/main/
        ├── java/com/heypudu/ms_analytics/
        │   ├── MsAnalyticsApplication.java
        │   ├── audit/                  # Persistencia idempotente de eventos
        │   ├── config/                 # Kafka (dos grupos) y seguridad
        │   ├── controller/AnalyticsController.java
        │   ├── dto/RoomStatsResponse.java
        │   ├── listener/               # LiveEventListener, LiveAuditListener
        │   ├── messaging/              # Evento y tipos compartidos
        │   ├── metrics/                # RoomMetrics y RoomMetricsService
        │   ├── model/LiveEventAudit.java
        │   ├── repository/
        │   └── security/
        └── resources/
            ├── application.yml
            ├── application-dev.yml
            └── db/migration/V1__init_live_events_audit.sql
```

Las estructuras anteriores corresponden al código actual, no a propuestas.
Cada microservicio mantiene su propio build, configuración, migraciones,
pruebas y Dockerfile.

---

## Convenciones

Estas convenciones son **obligatorias** para todos los microservicios.

### Paquetes

- Paquete base: `com.heypudu.<nombre_del_ms>`
  - `ms-auth` → `com.heypudu.ms_auth`
  - `ms-users` → `com.heypudu.ms_users`
  - `ms-posts` → `com.heypudu.ms_posts`
  - `ms-interactions` → `com.heypudu.ms_interactions`
  - `ms-live` → `com.heypudu.ms_live`
  - `ms-analytics` → `com.heypudu.ms_analytics`
- Subpaquetes: `config`, `controller`, `dto`, `exception`, `mapper`, `model`, `repository`, `security`, `service`, `service/impl`, `client`.

### Endpoints

- Prefijo: `/api/v1/<recurso>`
- Recurso en plural: `/api/v1/users`, `/api/v1/posts`.
- Endpoints del usuario actual: `/api/v1/users/me`, `/api/v1/posts/me`.
- Endpoints de administración: `/api/v1/admin/...`.
- Endpoints internos (entre microservicios): `/api/v1/internal/...`.

### DTOs

- Como `record` de Java.
- Sufijo `Request` para entrada, `Response` para salida.
- No se exponen entidades JPA en los controladores.

### Entidades

- Sin Lombok. Getters, setters y constructores explícitos.
- Constructor vacío obligatorio (requerido por JPA).
- `@Table(name = "...")` explícito.
- `Instant` para fechas, mapeado a `TIMESTAMPTZ`.

### Errores

- `GlobalExceptionHandler` en cada microservicio.
- Formato uniforme: `timestamp`, `status`, `error`, `message`, `path`.
- Excepciones propias: `ResourceNotFoundException`, `BusinessException`.

### Migraciones

- Flyway con nombres `V{n}__{descripcion}.sql`.
- Una migración por cambio estructural.
- Nunca modificar una migración ya aplicada. Crear una nueva.

### Configuración

- `application.yml`: propiedades comunes.
- `application-dev.yml`: credenciales de desarrollo (hardcodeadas).
- `application-prod.yml`: credenciales de producción (variables de entorno).
- Nunca hardcodear credenciales de producción.

### Seguridad

- Todos los microservicios usan `OAuth2 Resource Server` con Cognito.
- `JwtAuthConverter` convierte `cognito:groups` a `ROLE_*`.
- `CognitoUserExtractor` extrae `sub`, `email` y `groups` del JWT.
- Endpoints públicos: `/actuator/health`.
- Todo lo demás requiere JWT válido.

---

## Modelo de datos

### Esquema `heypudu_auth` (ms-auth)

```sql
CREATE TABLE auth_users (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    cognito_sub  UUID         NOT NULL UNIQUE,
    email        VARCHAR(255) NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_auth_users_status
        CHECK (status IN ('ACTIVE', 'BANNED', 'DELETED'))
);
```

### Esquema `heypudu_users` (ms-users)

```sql
CREATE TABLE users (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    cognito_sub     UUID         NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    bio             VARCHAR(300),
    avatar_url      VARCHAR(500),
    is_verified     BOOLEAN      NOT NULL DEFAULT FALSE,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    follower_count  INT          NOT NULL DEFAULT 0,
    following_count INT          NOT NULL DEFAULT 0,
    post_count      INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    anonymized_at   TIMESTAMPTZ,
    CONSTRAINT chk_users_status
        CHECK (status IN ('ACTIVE', 'BANNED', 'DELETED'))
);

CREATE TABLE follows (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    follower_id  UUID        NOT NULL,
    followed_id  UUID        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_follows UNIQUE (follower_id, followed_id),
    CONSTRAINT chk_follows_no_self CHECK (follower_id <> followed_id)
);
```

### Esquema `heypudu_posts` (ms-posts)

```sql
CREATE TABLE posts (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id              UUID         NOT NULL,
    author_username        VARCHAR(50)  NOT NULL,
    author_avatar_url      VARCHAR(500),
    type                   VARCHAR(20)  NOT NULL,
    content                TEXT,
    audio_key              VARCHAR(500),
    audio_duration_seconds INT,
    cover_image_key        VARCHAR(500),
    like_count             INT          NOT NULL DEFAULT 0,
    comment_count          INT          NOT NULL DEFAULT 0,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_posts_type CHECK (type IN ('TEXT', 'AUDIO')),
    CONSTRAINT chk_posts_audio_duration
        CHECK (audio_duration_seconds IS NULL OR audio_duration_seconds <= 300),
    CONSTRAINT chk_posts_content_by_type
        CHECK (
            (type = 'TEXT'  AND content IS NOT NULL AND audio_key IS NULL) OR
            (type = 'AUDIO' AND audio_key IS NOT NULL AND content IS NULL)
        )
);

CREATE INDEX idx_posts_author_id ON posts (author_id);
CREATE INDEX idx_posts_created_at ON posts (created_at DESC);
```

### Esquema `heypudu_interactions` (ms-interactions)

```sql
CREATE TABLE likes (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id    UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_likes UNIQUE (post_id, user_id)
);

CREATE INDEX idx_likes_post_id ON likes (post_id);
CREATE INDEX idx_likes_user_id ON likes (user_id);

CREATE TABLE comments (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id    UUID         NOT NULL,
    user_id    UUID         NOT NULL,
    content    VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_comments_post_id ON comments (post_id);
CREATE INDEX idx_comments_user_id ON comments (user_id);
```

> **Nota:** los scripts SQL definitivos van en `src/main/resources/db/migration/` de cada microservicio con nombres `V1__...sql`, `V2__...sql`, etc.

---

## Seguridad y autenticación

### Cognito

- **User Pool** gestiona credenciales, verificación de correo y emisión de JWT.
- **Grupos (groups)**:
  - `user` → `ROLE_USER`
  - `admin` → `ROLE_ADMIN`
- Los grupos viajan en el claim `cognito:groups` del JWT.

### Roles vs. atributos

- **Rol**: permiso de sistema (`user`, `admin`). Vive en Cognito.
- **Atributo de perfil**: cualidad del usuario (`is_verified`). Vive en `ms-users`.
- **Estado**: situación del usuario (`ACTIVE`, `BANNED`, `DELETED`). Vive en `ms-auth` y `ms-users`.

### Validación del JWT

Todos los microservicios validan el JWT contra Cognito usando `OAuth2 Resource Server`. La validación incluye:

- Firma (con claves JWKS de Cognito).
- Expiración.
- Issuer (`iss` claim).

### Registro de usuarios

1. El usuario se registra en Cognito desde el frontend con Amplify.
2. Cognito crea la cuenta y devuelve un JWT.
3. El frontend llama a `POST /api/v1/auth/sync` en `ms-auth`.
4. `ms-auth` crea el registro en `auth_users` si no existe.
5. El frontend llama a `POST /api/v1/users` en `ms-users` para crear el perfil base.
6. El usuario completa su perfil con `PUT /api/v1/users/me`.

### Baneo

1. Admin llama a `PUT /api/v1/admin/users/{id}/ban` en `ms-users`.
2. `ms-users` marca el usuario como `BANNED` y anonimiza sus datos personales (name, username, bio, avatar).
3. `ms-users` llama a `ms-auth` para deshabilitar al usuario en Cognito.
4. Las publicaciones y likes del usuario **se mantienen** pero aparecen como "Usuario eliminado".

---

## Comunicación entre microservicios

### Reglas

- **Nunca** acceder directamente a la BD de otro microservicio.
- **Sí** llamar por REST cuando se necesita información en tiempo real.
- **Sí** duplicar datos mínimos cuando el dato cambia poco y se lee mucho.

### Cliente REST

Cada microservicio que necesite llamar a otro usa `RestClient` de Spring (disponible desde Spring 6.1). Se configura en `config/RestClientConfig.java`.

```java
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient usersRestClient(@Value("${clients.users.base-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
```

### Ejemplo de uso en `ms-posts`

```java
@Service
public class UserClient {

    private final RestClient usersRestClient;

    public UserClient(RestClient usersRestClient) {
        this.usersRestClient = usersRestClient;
    }

    public UserSummaryResponse getUser(UUID userId, String jwt) {
        return usersRestClient.get()
                .uri("/api/v1/internal/users/{id}", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .retrieve()
                .body(UserSummaryResponse.class);
    }
}
```

### Endpoints internos

Los endpoints internos se prefijan con `/api/v1/internal/` y **no** se exponen en el API Gateway público. Solo son accesibles desde la VPC.

### Mensajería asíncrona con RabbitMQ

RabbitMQ se utiliza para comandos y eventos que no requieren una respuesta
HTTP del consumidor:

1. El productor publica en un exchange.
2. RabbitMQ enruta según la routing key y los bindings.
3. El consumidor procesa el mensaje.
4. El consumidor envía ACK cuando termina correctamente.
5. Ante un error no recuperable, envía NACK sin requeue y el mensaje llega a
   la DLQ correspondiente.

Los flujos actuales son:

- `ms-posts` publica `post.delete` en `ex.heypudu.cmd`; `ms-interactions`
  consume `q.heypudu.command` y elimina likes y comentarios.
- `ms-interactions` publica `interaction.liked` y
  `interaction.commented` en `ex.heypudu.events`.
- `ms-users` publica `user.followed` y consume
  `q.heypudu.notification`.
- `ms-posts` consume `q.heypudu.audit` para registrar eventos.

Las colas, exchanges, bindings y DLQ se declaran de forma idempotente mediante
configuración Spring AMQP. Los nombres se centralizan en `RabbitTopology`.
RabbitMQ no reemplaza las llamadas REST que necesitan datos síncronos.

---

## Almacenamiento de archivos

### S3

- Los audios y portadas **no** pasan por el backend.
- El frontend sube directamente a S3 con URLs prefirmadas.
- El backend solo genera las URLs y guarda las claves (`audio_key`, `cover_image_key`).

### Flujo de subida

```
[Usuario graba audio en React]
        |
        v
[Frontend pide: POST /api/v1/posts/audio/presigned-url]
        |
        v
[ms-posts genera URL prefirmada de S3 y la devuelve]
        |
        v
[Frontend sube el audio DIRECTO a S3]
        |
        v
[Frontend llama: POST /api/v1/posts con { type: "AUDIO", audioKey: "..." }]
        |
        v
[ms-posts guarda en BD: audio_key, audio_duration_seconds, ...]
        |
        v
[Cuando alguien pide GET /api/v1/posts/{id}]
        |
        v
[ms-posts devuelve la publicación con URL prefirmada de lectura]
        |
        v
[Frontend reproduce el audio desde S3]
```

### Buckets

| Bucket | Contenido | Acceso |
|---|---|---|
| `heypudu-audio` | Audios de publicaciones | Privado, solo URLs prefirmadas |
| `heypudu-covers` | Portadas de publicaciones y avatares | Privado, solo URLs prefirmadas |

---

## Configuración y entornos

### Variables de entorno comunes en producción

| Variable | Descripción |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` en producción, `dev` en desarrollo. |
| `DB_URL` | URL JDBC de PostgreSQL (incluye `currentSchema`). |
| `DB_USERNAME` | Usuario de BD. |
| `DB_PASSWORD` | Contraseña de BD. |
| `COGNITO_ISSUER_URI` | Issuer del User Pool de Cognito. |
| `COGNITO_JWK_SET_URI` | Endpoint JWKS del User Pool. |
| `APP_CORS_ALLOWED_ORIGINS` | Orígenes separados por coma. En producción: `https://heypudu.vercel.app`. |
| `RABBITMQ_HOST` | Host privado de RabbitMQ, normalmente `rabbit1` en Compose. |
| `RABBITMQ_PORT` | Puerto AMQP, normalmente `5672`. |
| `RABBIT_USER` | Usuario RabbitMQ. |
| `RABBIT_PASSWORD` | Contraseña RabbitMQ, solo en `.env`. |
| `RABBIT_VHOST` | Virtual host RabbitMQ, normalmente `/` para el administrador. |

### Variables específicas por microservicio

| Microservicio | Variable | Descripción |
|---|---|---|
| `ms-posts` | `AWS_REGION` | Región de S3. |
| `ms-posts` | `S3_AUDIO_BUCKET` | Nombre del bucket de audio. |
| `ms-posts` | `S3_COVERS_BUCKET` | Nombre del bucket de portadas. |
| `ms-users` | `CLIENTS_AUTH_BASE_URL` | URL base de `ms-auth`. |
| `ms-posts` | `CLIENTS_USERS_BASE_URL` | URL base de `ms-users`. |
| `ms-interactions` | `CLIENTS_POSTS_BASE_URL` | URL base de `ms-posts`. |
| `ms-users`, `ms-posts`, `ms-interactions`, `ms-heypudu-rabbit-admin` | `RABBITMQ_*` | Parámetros de conexión AMQP y autenticación RabbitMQ. |

### Frontend en Vercel

El frontend de producción está publicado en `https://heypudu.vercel.app`.
Configura esta variable en cada contenedor:

```text
APP_CORS_ALLOWED_ORIGINS=https://heypudu.vercel.app
```

Para desarrollo local puedes permitir ambos orígenes:

```text
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,https://heypudu.vercel.app
```

Los cuatro microservicios de dominio aplican esta lista a las peticiones CORS, incluyendo
las peticiones `OPTIONS` de preflight. No se debe usar `*` en producción.

El frontend debe usar como URL base la URL pública de API Gateway, no la IP ni
los puertos de EC2:

```text
VITE_API_BASE_URL=https://<api-id>.execute-api.us-east-1.amazonaws.com/<stage>
```

### Configuración CORS de S3

El bucket de producción es `heypudu-data-561816430875-us-east-1-an`. Debe
permanecer privado y tener esta configuración CORS para permitir las cargas
directas mediante URLs prefirmadas:

```json
[
  {
    "AllowedHeaders": ["Content-Type", "Authorization"],
    "AllowedMethods": ["GET", "PUT", "HEAD"],
    "AllowedOrigins": [
      "https://heypudu.vercel.app",
      "http://localhost:5173"
    ],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3000
  }
]
```

En producción se puede quitar `http://localhost:5173` después de terminar las
pruebas locales.

---

## Ejecución local

### Requisitos

- JDK 25
- Docker (para PostgreSQL local)
- Gradle Wrapper (incluido en cada microservicio)

### Levantar RabbitMQ

Para ejecutar localmente el clúster de dos nodos:

```bash
docker compose -f docker-compose.rabbit.yml up -d
docker compose -f docker-compose.rabbit.yml ps
docker compose -f docker-compose.rabbit.yml logs rabbit-cluster-init
docker exec heypudu-rabbit1 rabbitmqctl cluster_status
```

La Management UI queda disponible en `http://localhost:15672` para `rabbit1`
y en `http://localhost:15673` para `rabbit2`. Las credenciales se configuran
mediante `.env`; nunca deben incluirse en este README.

### Levantar Kafka

Kafka corre en modo **ZooKeeper** con **dos brokers** (ZooKeeper 3.9 + Kafka
3.9) y Kafka UI. Las imágenes quedan fijadas en la línea 7.9.0 de Confluent
porque Kafka 4.x eliminó ZooKeeper:

```bash
docker compose -f docker-compose.kafka.yml up -d
docker compose -f docker-compose.kafka.yml logs -f kafka-init
```

El contenedor `kafka-init` declara los dos tópicos y termina:

| Tópico | Particiones | Réplicas | Retención | Flujo |
|---|---|---|---|---|
| `heypudu.live.events` | 3 | 2 | 7 días | Salas de audio en vivo |
| `heypudu.posts.events` | 2 | 2 | 3 días | Publicaciones |

- Kafka UI: `http://localhost:8090`
- ZooKeeper: `localhost:2181`
- Bootstrap desde el host: `localhost:29092,localhost:29093`
- Bootstrap desde contenedores de Compose: `broker1:9092,broker2:9092`

Verificar tópicos y grupos:

```bash
docker exec heypudu-broker1 kafka-topics.sh \
  --bootstrap-server broker1:9092 --describe --topic heypudu.live.events
docker exec heypudu-broker1 kafka-consumer-groups.sh \
  --bootstrap-server broker1:9092 --list
```

### Levantar MediaMTX

SFU WebRTC local para las salas de audio (WHIP para publicar, WHEP para
escuchar). La imagen se construye sobre Alpine para incluir `curl` y poder
ejecutar los hooks.

```bash
docker compose -f docker-compose.mediamtx.yml up -d --build
```

- WHIP/WHEP: `http://localhost:8889/<roomId>/whip` y
  `http://localhost:8889/<roomId>/whep?user=<cognitoSub>`
- API de control: `http://localhost:9997`
- La autorización la resuelve `ms-live` en
  `/api/v1/internal/mediamtx/auth` con `MEDIAMTX_SERVICE_TOKEN`.
- En local el contenedor llega a `ms-live` por `host.docker.internal:8086`.

### Levantar PostgreSQL

```bash
docker run -d --name heypudu-postgres \
  -e POSTGRES_DB=heypudu \
  -e POSTGRES_USER=heypudu_admin \
  -e POSTGRES_PASSWORD=devpassword \
  -p 5432:5432 \
  postgres:16
```

Crear los esquemas y usuarios:

```sql
CREATE SCHEMA IF NOT EXISTS heypudu_auth;
CREATE SCHEMA IF NOT EXISTS heypudu_users;
CREATE SCHEMA IF NOT EXISTS heypudu_posts;
CREATE SCHEMA IF NOT EXISTS heypudu_interactions;

CREATE USER heypudu_auth_user WITH PASSWORD 'devpassword';
CREATE USER heypudu_users_user WITH PASSWORD 'devpassword';
CREATE USER heypudu_posts_user WITH PASSWORD 'devpassword';
CREATE USER heypudu_interactions_user WITH PASSWORD 'devpassword';

GRANT ALL PRIVILEGES ON SCHEMA heypudu_auth TO heypudu_auth_user;
GRANT ALL PRIVILEGES ON SCHEMA heypudu_users TO heypudu_users_user;
GRANT ALL PRIVILEGES ON SCHEMA heypudu_posts TO heypudu_posts_user;
GRANT ALL PRIVILEGES ON SCHEMA heypudu_interactions TO heypudu_interactions_user;
```

### Levantar cada microservicio

```bash
cd ms-auth
./gradlew bootRun

# En otra terminal
cd ms-users
./gradlew bootRun

# Y así con cada uno
```

### Verificar

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8085/actuator/health
```

---

## Despliegue

### EC2 con Docker Compose y PostgreSQL externo

El archivo `docker-compose.prod.yml` descarga las cinco imágenes desde Docker
Hub, ejecuta los microservicios con el perfil `prod`, los conecta a PostgreSQL
externo (Neon) y los comunica entre sí mediante una red Docker privada.
Publica los puertos `8081-8085` para el Load Balancer, con límites de memoria
por contenedor.

RabbitMQ se despliega por separado en su propia EC2 con
`docker-compose.rabbit.yml`. La EC2 backend no comparte la red Docker de
RabbitMQ: se conecta mediante `RABBITMQ_HOST` y el puerto AMQP privado.

Cadena pública: Vercel → API Gateway HTTP API (`ANY /{proxy+}`) → ALB → EC2.
La configuración completa está en `../DESPLIEGUE-AWS.md`.

Preparación en EC2:

1. Iniciar sesión en Docker Hub con `docker login`.
2. Crear swap de 2 GB **antes** de arrancar los contenedores.
3. Copiar `.env.production.example` como `.env` y completarlo con la cadena de
   conexión de Neon, credenciales por esquema, Cognito, buckets S3 y
   parámetros RabbitMQ.
4. Ejecutar:

```bash
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml ps
```

`.env` está excluido de Git y no debe subirse al repositorio. La EC2 debe tener
asignado un IAM Role (`LabInstanceRole` en AWS Academy) con permisos sobre los
tres buckets S3.

> La versión de Docker Compose instalada en Amazon Linux 2023 no soporta
> `--env-file`; por eso se usa el archivo `.env` automático.

### EC2

Cada microservicio se despliega en un contenedor Docker sobre una instancia EC2.

### Imagen Docker

Cada microservicio tiene su propio `Dockerfile` (multi-stage build).

### API Gateway

- Enruta `/api/v1/auth/**` → `ms-auth`
- Enruta `/api/v1/users/**` → `ms-users`
- Enruta `/api/v1/posts/**` → `ms-posts`
- Enruta `/api/v1/interactions/**` → `ms-interactions`
- Enruta `/api/v1/admin/rabbit/**` → `ms-heypudu-rabbit-admin`
- Enruta `/api/v1/live/**` → `ms-live`
- Enruta `/api/v1/analytics/**` → `ms-analytics`
- Valida el JWT contra Cognito antes de reenviar la petición.
- Los endpoints `/api/v1/internal/**` **no** se exponen públicamente.

---

## CI/CD

Los servicios de dominio tienen workflows de GitHub Actions en
`.github/workflows/deploy-ms-<nombre>.yml`. El administrador RabbitMQ se
publica como imagen Docker y se incorpora en
`docker-compose.prod.yml`.

### Flujo

1. Push a `main` que modifique archivos de un microservicio.
2. GitHub Actions detecta el cambio (filtro por `paths`).
3. Construye el JAR con `./gradlew build`.
4. Construye la imagen Docker.
5. Sube la imagen a Docker Hub (o ECR).
6. Se conecta al EC2 vía SSH y ejecuta el script de despliegue.
7. El EC2 hace `docker pull` de la nueva imagen y reinicia el contenedor.

### Secrets requeridos

| Secret | Descripción |
|---|---|
| `EC2_HOST` | IP pública o DNS del EC2. |
| `EC2_USERNAME` | Usuario SSH de la EC2 (`ec2-user` en Amazon Linux). |
| `EC2_SSH_KEY` | Clave privada SSH. |
| `DOCKERHUB_USERNAME` | Usuario de Docker Hub. |
| `DOCKERHUB_TOKEN` | Token de Docker Hub. |

---

## Estado actual del proyecto

### ✅ Completado

- Los siete microservicios Spring Boot están implementados:
  `ms-auth`, `ms-users`, `ms-posts`, `ms-interactions`,
  `ms-heypudu-rabbit-admin`, `ms-live` y `ms-analytics`.
- La autenticación JWT con AWS Cognito y la autorización por grupos están
  integradas.
- La persistencia productiva utiliza Neon PostgreSQL con un esquema por
  microservicio.
- Los archivos de audio, portadas y avatares utilizan AWS S3 con URLs
  prefirmadas.
- RabbitMQ está desplegado como clúster de dos nodos en una EC2 separada.
- Existen cuatro colas funcionales, cuatro DLQ, exchanges `direct` y `topic`,
  ACK/NACK manual, TTL, límites y listeners de DLQ.
- El administrador REST crea y elimina exchanges, colas y bindings y está
  protegido con `ROLE_ADMIN`.
- Las imágenes Docker de producción están publicadas en Docker Hub y el
  Compose productivo ejecuta los cinco servicios.
- Las salas de audio en vivo están implementadas y desplegadas: `ms-live`
  (WHIP/WHEP, acceso por seguimiento, reacciones), `ms-analytics` (oyentes y
  reacciones en tiempo real, con auditoría en `live_event_audit`), MediaMTX con
  autorización HTTP y hooks, Kafka en modo KRaft y TLS terminado en Caddy.
- La infraestructura productiva incluye API Gateway, ALB, EC2, Neon, S3,
  Cognito y Vercel.

### 📋 Pendientes y mejoras

- Validar estrictamente las claves S3 y su pertenencia al usuario o
  publicación.
- Eliminar explícitamente el avatar de S3 al eliminar una cuenta.
- Completar y ampliar las pruebas automatizadas de los microservicios.
- Migrar los endpoints internos a autenticación de servicio.
- Evaluar un BFF separado si se requiere por arquitectura futura.
- Mejorar observabilidad con métricas y trazas centralizadas.

---

## Trabajo futuro

- **BFF**: agregar una capa que orqueste llamadas para el frontend.
- **RabbitMQ**: ampliar eventos de dominio y estrategias de reprocesamiento
  según nuevas necesidades del producto.
- **Secrets Manager**: mover credenciales de variables de entorno a AWS Secrets Manager.
- **Tests**: unitarios, integración con Testcontainers, end-to-end.
- **Observabilidad**: CloudWatch, X-Ray, Prometheus.
- **Rate limiting**: en API Gateway.
- **Comentarios en audio**: soporte para comentarios grabados.
- **Editor de audio**: reacciones sincronizadas con el audio.
- **Publicación rápida**: shake-to-publish.
- **Grupos de publicaciones**: temporadas tipo podcast.

---

## Cómo trabajar en este proyecto (guía para IA o desarrollador)

### Antes de escribir código

1. **Leer este README completo.** Es la fuente de verdad.
2. **Identificar el microservicio** en el que se va a trabajar.
3. **Revisar `ms-auth`** como referencia. Es el patrón a seguir.
4. **Verificar las convenciones** de la sección [Convenciones](#convenciones).

### Al construir un nuevo microservicio

1. Crear la carpeta `ms-<nombre>/`.
2. Generar el proyecto en [start.spring.io](https://start.spring.io) con:
   - **Project**: Gradle - Groovy
   - **Language**: Java
   - **Spring Boot**: 3.5.x
   - **Java**: 25
   - **Group**: `com.heypudu`
   - **Artifact**: `ms-<nombre>` (ej: `ms-users`)
   - **Package name**: `com.heypudu.ms_<nombre>`
3. Dependencias base (obligatorias en todos):
   - Spring Web
   - Spring Security
   - OAuth2 Resource Server
   - Spring Data JPA
   - PostgreSQL Driver
   - Flyway Migration
   - Spring Boot Actuator
4. Dependencias adicionales según el microservicio (ver tabla en [Microservicios](#microservicios)).
5. Copiar la estructura de paquetes de `ms-auth`.
6. Copiar `SecurityConfig`, `JwtConfig`, `JwtAuthConverter` y `CognitoUserExtractor` de `ms-auth` (ajustando el paquete).
7. Implementar entidades, repositorios, servicios, controladores, DTOs y migraciones según el modelo de datos.
8. Actualizar `application.yml`, `application-dev.yml` y `application-prod.yml` con el puerto, el esquema de BD y el nombre del microservicio.
9. Crear el `README.md` del microservicio (similar al de `ms-auth`).
10. Crear el `Dockerfile` y el workflow de GitHub Actions.

### Al modificar un microservicio existente

1. Respetar las convenciones de paquetes y nombres.
2. Si cambia el esquema de BD, crear una nueva migración Flyway (`V2__...sql`).
3. Actualizar el `README.md` del microservicio si cambia algo relevante.
4. No modificar migraciones ya aplicadas.

### Al agregar una nueva dependencia

1. Agregarla en `build.gradle` del microservicio correspondiente.
2. Documentarla en el `README.md` del microservicio.

### Al agregar un nuevo endpoint

1. Crear el DTO de request y/o response.
2. Implementar el método en el servicio.
3. Exponer el endpoint en el controlador.
4. Documentarlo en el `README.md` del microservicio.

---

## Autor

Proyecto desarrollado para la asignatura **DSY1107 - Desarrollo Cloud Native**.