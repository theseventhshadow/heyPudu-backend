# heyPudú! — Backend

Backend del proyecto **heyPudú!**, una red social de audio breve ("briefcasts") con texto, audio, likes, comentarios y perfiles. Construido con una arquitectura de microservicios en Spring Boot, desplegado en AWS y autenticado con Cognito.

Este README es la **fuente de verdad** del backend. Debe contener todo lo necesario para que cualquier desarrollador (o IA) pueda retomar el trabajo sin contexto adicional.

---

## Índice

1. [Descripción del proyecto](#descripción-del-proyecto)
2. [Arquitectura general](#arquitectura-general)
3. [Microservicios](#microservicios)
4. [Decisiones de diseño](#decisiones-de-diseño)
5. [Estructura del repositorio](#estructura-del-repositorio)
6. [Convenciones](#convenciones)
7. [Modelo de datos](#modelo-de-datos)
8. [Seguridad y autenticación](#seguridad-y-autenticación)
9. [Comunicación entre microservicios](#comunicación-entre-microservicios)
10. [Almacenamiento de archivos](#almacenamiento-de-archivos)
11. [Configuración y entornos](#configuración-y-entornos)
12. [Ejecución local](#ejecución-local)
13. [Despliegue](#despliegue)
14. [CI/CD](#cicd)
15. [Estado actual del proyecto](#estado-actual-del-proyecto)
16. [Trabajo futuro](#trabajo-futuro)

---

## Descripción del proyecto

**heyPudú!** es una red social de audio breve. Los usuarios pueden:

- Publicar **briefcasts**: audios de máximo 5 minutos, con portada opcional.
- Publicar texto.
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
   ┌───────────┼───────────┬───────────────┐
   ▼           ▼           ▼               ▼
┌────────┐ ┌────────┐ ┌─────────┐ ┌──────────────┐
│ms-auth │ │ms-users│ │ms-posts │ │ms-interactions│
│ :8081  │ │ :8082  │ │ :8083   │ │ :8084         │
└───┬────┘ └───┬────┘ └────┬────┘ └──────┬────────┘
    │          │           │             │
    ▼          ▼           ▼             ▼
┌────────┐ ┌────────┐ ┌─────────┐ ┌──────────────┐
│auth_db │ │users_db│ │posts_db │ │interactions_db│
└────────┘ └────────┘ └────┬────┘ └──────────────┘
                            │
                            ▼
                     ┌────────────┐
                     │  AWS S3    │
                     │ (audio +   │
                     │  portadas) │
                     └────────────┘
```

**Tecnologías transversales:**

- **Java 25**
- **Spring Boot 3.5.x**
- **Gradle** (cada microservicio con su propio `build.gradle`)
- **PostgreSQL** (una instancia RDS, esquemas separados)
- **AWS Cognito** (autenticación)
- **AWS S3** (almacenamiento de audio y portadas)
- **AWS API Gateway** (enrutamiento y validación de JWT)
- **AWS EC2** (despliegue de microservicios vía Docker)
- **GitHub Actions** (CI/CD)

---

## Microservicios

| Microservicio | Puerto | Esquema de BD | Responsabilidad |
|---|---|---|---|
| `ms-auth` | 8081 | `heypudu_auth` | Sincronización con Cognito, datos del usuario autenticado. |
| `ms-users` | 8082 | `heypudu_users` | Perfil, seguir/dejar de seguir, búsqueda de usuarios, administración. |
| `ms-posts` | 8083 | `heypudu_posts` | Publicaciones (texto/audio), feed, búsqueda de posts, URLs prefirmadas S3. |
| `ms-interactions` | 8084 | `heypudu_interactions` | Likes y comentarios. |

Cada microservicio es un **proyecto Spring Boot independiente** con su propio `build.gradle`, su propia clase `@SpringBootApplication`, su propio `Dockerfile` y su propia configuración.

---

## Decisiones de diseño

Estas decisiones son **vinculantes** para el proyecto. Cualquier IA o desarrollador que continúe debe respetarlas.

### Arquitectura

| Decisión | Razón |
|---|---|
| Microservicios separados | Requisito de la EP1. Cada servicio puede escalar y desplegarse de forma independiente. |
| Monorepo (todos los `ms-*` en un mismo repo) | Simplifica la gestión para un equipo pequeño. Cada carpeta es un proyecto Spring Boot autónomo. |
| Sin pom padre de Maven | Se usa Gradle. Cada microservicio tiene su propio build. |
| Base de datos por microservicio | Principio de database-per-service. Un solo RDS con esquemas separados. |
| Sin BFF en la EP1 | El API Gateway cumple parcialmente ese rol. Se documenta como trabajo futuro. |
| Sin SQS/SNS en la EP1 | Se usa REST puntual o duplicación mínima de datos. Se documenta como trabajo futuro. |

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
├── ms-users/                          # 🚧 Pendiente
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
│               └── V2__init_follows_table.sql
│
├── ms-posts/                          # 🚧 Pendiente
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
│               └── V1__init_posts_schema.sql
│
└── ms-interactions/                   # 🚧 Pendiente
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
                └── V2__init_comments_table.sql
```

> **Nota:** las estructuras de `ms-users`, `ms-posts` y `ms-interactions` son **propuestas**. Al construir cada uno, se debe verificar que la estructura respete el patrón usado en `ms-auth`.

---

## Convenciones

Estas convenciones son **obligatorias** para todos los microservicios.

### Paquetes

- Paquete base: `com.heypudu.<nombre_del_ms>`
  - `ms-auth` → `com.heypudu.ms_auth`
  - `ms-users` → `com.heypudu.ms_users`
  - `ms-posts` → `com.heypudu.ms_posts`
  - `ms-interactions` → `com.heypudu.ms_interactions`
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

### Variables específicas por microservicio

| Microservicio | Variable | Descripción |
|---|---|---|
| `ms-posts` | `AWS_REGION` | Región de S3. |
| `ms-posts` | `S3_AUDIO_BUCKET` | Nombre del bucket de audio. |
| `ms-posts` | `S3_COVERS_BUCKET` | Nombre del bucket de portadas. |
| `ms-users` | `CLIENTS_AUTH_BASE_URL` | URL base de `ms-auth`. |
| `ms-posts` | `CLIENTS_USERS_BASE_URL` | URL base de `ms-users`. |
| `ms-interactions` | `CLIENTS_POSTS_BASE_URL` | URL base de `ms-posts`. |

---

## Ejecución local

### Requisitos

- JDK 25
- Docker (para PostgreSQL local)
- Gradle Wrapper (incluido en cada microservicio)

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
```

---

## Despliegue

### EC2

Cada microservicio se despliega en un contenedor Docker sobre una instancia EC2.

### Imagen Docker

Cada microservicio tiene su propio `Dockerfile` (multi-stage build).

### API Gateway

- Enruta `/api/v1/auth/**` → `ms-auth`
- Enruta `/api/v1/users/**` → `ms-users`
- Enruta `/api/v1/posts/**` → `ms-posts`
- Enruta `/api/v1/interactions/**` → `ms-interactions`
- Valida el JWT contra Cognito antes de reenviar la petición.
- Los endpoints `/api/v1/internal/**` **no** se exponen públicamente.

---

## CI/CD

Cada microservicio tiene su propio workflow de GitHub Actions en `.github/workflows/deploy-ms-<nombre>.yml`.

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
| `EC2_USERNAME` | Usuario SSH (`ubuntu`). |
| `EC2_SSH_KEY` | Clave privada SSH. |
| `DOCKERHUB_USERNAME` | Usuario de Docker Hub. |
| `DOCKERHUB_TOKEN` | Token de Docker Hub. |

---

## Estado actual del proyecto

### ✅ Completado

- **ms-auth**: estructura completa, endpoints, seguridad, BD, configuración, README.

### 🚧 Pendiente

- **ms-users**: perfil, seguir, búsqueda, administración.
- **ms-posts**: publicaciones, feed, S3, búsqueda.
- **ms-interactions**: likes, comentarios.

### 📋 Próximos pasos sugeridos

1. Construir **ms-users** siguiendo el patrón de `ms-auth`.
2. Construir **ms-posts** (incluye integración con S3).
3. Construir **ms-interactions**.
4. Configurar API Gateway y Cognito en AWS.
5. Configurar RDS con los esquemas.
6. Configurar EC2 y los pipelines de GitHub Actions.
7. Integrar con el frontend React.

---

## Trabajo futuro

- **BFF**: agregar una capa que orqueste llamadas para el frontend.
- **SQS/SNS**: sincronización asíncrona entre microservicios.
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