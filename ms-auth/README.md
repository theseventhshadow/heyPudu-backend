# ms-auth

Microservicio de autenticación y sincronización de usuarios para **heyPudú!**.

## Descripción

`ms-auth` es el punto de entrada entre AWS Cognito y el resto del sistema. No gestiona credenciales (eso lo hace Cognito), sino que:

- Sincroniza el usuario de Cognito con la base de datos propia del microservicio.
- Devuelve los datos del usuario autenticado a partir del JWT.
- Registra qué usuarios han iniciado sesión y su estado (`ACTIVE`, `BANNED`, `DELETED`).

Forma parte de la arquitectura de microservicios de heyPudú!, junto a `ms-users`, `ms-posts` y `ms-interactions`.

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
| Monitoreo | Spring Boot Actuator |

## Arquitectura

```
[React + AWS Amplify]
        |
        | JWT (Authorization: Bearer <token>)
        v
[AWS API Gateway]
        |
        v
[ms-auth :8081]  <-- valida el JWT contra Cognito
        |
        v
[PostgreSQL - esquema heypudu_auth]
```

- **Puerto:** `8081`
- **Esquema de BD:** `heypudu_auth`
- **IdP:** AWS Cognito (User Pool)
- **Rol en el sistema:** sincronización y consulta del usuario autenticado.

## Estructura del proyecto

```
ms-auth/
├── build.gradle
├── settings.gradle
├── Dockerfile
└── src/main/
    ├── java/com/heypudu/ms_auth/
    │   ├── MsAuthApplication.java
    │   ├── config/
    │   │   ├── SecurityConfig.java
    │   │   └── JwtConfig.java
    │   ├── controller/
    │   │   └── AuthController.java
    │   ├── dto/
    │   │   ├── request/
    │   │   │   └── SyncUserRequest.java
    │   │   └── response/
    │   │       ├── AuthUserResponse.java
    │   │       └── SyncUserResponse.java
    │   ├── exception/
    │   │   ├── GlobalExceptionHandler.java
    │   │   └── ResourceNotFoundException.java
    │   ├── mapper/
    │   │   └── AuthUserMapper.java
    │   ├── model/
    │   │   └── AuthUser.java
    │   ├── repository/
    │   │   └── AuthUserRepository.java
    │   ├── security/
    │   │   ├── JwtAuthConverter.java
    │   │   └── CognitoUserExtractor.java
    │   └── service/
    │       ├── AuthService.java
    │       └── impl/
    │           └── AuthServiceImpl.java
    └── resources/
        ├── application.yml
        ├── application-dev.yml
        ├── application-prod.yml
        └── db/migration/
            └── V1__init_auth_schema.sql
```

## Endpoints

Todos los endpoints están versionados bajo `/api/v1/auth`.

### `GET /api/v1/auth/me`

Devuelve los datos del usuario autenticado a partir del JWT.

- **Autenticación:** requerida (JWT de Cognito).
- **Roles permitidos:** `user`, `admin`.
- **Body:** no recibe.
- **Respuesta `200 OK`:**

```json
{
  "id": "3f1c9e2b-...",
  "cognitoSub": "a1b2c3d4-...",
  "email": "usuario@ejemplo.com",
  "status": "ACTIVE",
  "roles": ["user"],
  "createdAt": "2026-10-06T12:00:00Z"
}
```

- **Respuesta `404 Not Found`:** si el usuario aún no ha sido sincronizado.

### `POST /api/v1/auth/sync`

Sincroniza el usuario de Cognito con la BD de `ms-auth`. Se llama una sola vez, tras el primer login exitoso. Es **idempotente**: si el usuario ya existe, no lo duplica.

- **Autenticación:** requerida (JWT de Cognito).
- **Roles permitidos:** `user`, `admin`.
- **Body:** no recibe (los datos se extraen del JWT).
- **Respuesta `200 OK`:**

```json
{
  "id": "3f1c9e2b-...",
  "cognitoSub": "a1b2c3d4-...",
  "email": "usuario@ejemplo.com",
  "status": "ACTIVE",
  "created": true,
  "createdAt": "2026-10-06T12:00:00Z"
}
```

- **`created: true`** → el usuario es nuevo, debe completar su perfil en `ms-users`.
- **`created: false`** → el usuario ya existía, puede ir directo al feed.

### Endpoints de salud

| Método | Endpoint | Acceso |
|---|---|---|
| `GET` | `/actuator/health` | Público |
| `GET` | `/actuator/info` | Público (solo en `dev`) |

## Modelo de datos

### Tabla `auth_users` (esquema `heypudu_auth`)

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | `UUID` | `PRIMARY KEY` | Clave primaria interna. |
| `cognito_sub` | `UUID` | `NOT NULL`, `UNIQUE` | `sub` del usuario en Cognito. |
| `email` | `VARCHAR(255)` | `NOT NULL` | Correo sincronizado desde el JWT. |
| `status` | `VARCHAR(20)` | `NOT NULL`, `CHECK` | `ACTIVE`, `BANNED`, `DELETED`. |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` | Fecha de creación del registro. |

> **Nota:** los datos completos del perfil (nombre, username, bio, avatar) viven en `ms-users`. `ms-auth` solo registra quién ha iniciado sesión y si está habilitado.

## Roles y seguridad

### Roles definidos en Cognito

| Grupo de Cognito | Rol de Spring | Uso |
|---|---|---|
| `user` | `ROLE_USER` | Usuario estándar. |
| `admin` | `ROLE_ADMIN` | Administrador del sistema. |

Los roles viajan en el claim `cognito:groups` del JWT y son convertidos a `GrantedAuthority` por `JwtAuthConverter`.

> **Nota:** la verificación de perfil (`is_verified`) **no** es un rol. Es una cualidad del perfil que vive en `ms-users`.

## Configuración

### Variables de entorno requeridas en producción

| Variable | Descripción |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Debe ser `prod` en producción. |
| `DB_URL` | URL JDBC de PostgreSQL (incluye `currentSchema=heypudu_auth`). |
| `DB_USERNAME` | Usuario de BD. |
| `DB_PASSWORD` | Contraseña de BD. |
| `COGNITO_ISSUER_URI` | Issuer del User Pool de Cognito. |
| `COGNITO_JWK_SET_URI` | Endpoint JWKS del User Pool. |

### Perfiles

| Perfil | Uso | Credenciales |
|---|---|---|
| `dev` | Desarrollo local | Hardcodeadas en `application-dev.yml` |
| `prod` | Producción | Leídas de variables de entorno |

## Ejecución local

### Requisitos

- JDK 25
- Docker (opcional, para PostgreSQL)
- Gradle Wrapper (incluido en el repo)

### Levantar PostgreSQL con Docker

```bash
docker run -d --name heypudu-postgres \
  -e POSTGRES_DB=heypudu \
  -e POSTGRES_USER=heypudu_auth_user \
  -e POSTGRES_PASSWORD=devpassword \
  -p 5432:5432 \
  postgres:16
```

Luego crear el esquema:

```sql
CREATE SCHEMA IF NOT EXISTS heypudu_auth;
```

### Ejecutar el microservicio

```bash
./gradlew bootRun
```

El servicio queda disponible en `http://localhost:8081`.

### Probar un endpoint

```bash
curl -H "Authorization: Bearer <JWT>" \
  http://localhost:8081/api/v1/auth/me
```

## Build y despliegue

### Construir el JAR

```bash
./gradlew build
```

El JAR queda en `build/libs/ms-auth-0.0.1-SNAPSHOT.jar`.

### Construir la imagen Docker

```bash
docker build -t heypudu/ms-auth:latest .
```

### Ejecutar el contenedor

```bash
docker run -d --name ms-auth \
  -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL="jdbc:postgresql://host:5432/heypudu?currentSchema=heypudu_auth" \
  -e DB_USERNAME="heypudu_auth_user" \
  -e DB_PASSWORD="********" \
  -e COGNITO_ISSUER_URI="https://cognito-idp.<region>.amazonaws.com/<poolId>" \
  -e COGNITO_JWK_SET_URI="https://cognito-idp.<region>.amazonaws.com/<poolId>/.well-known/jwks.json" \
  heypudu/ms-auth:latest
```

## Flujo de sincronización

```
[Usuario se registra en Cognito desde React con Amplify]
        |
        v
[Cognito crea la cuenta y devuelve JWT]
        |
        v
[React llama a POST /api/v1/auth/sync con el JWT]
        |
        v
[ms-auth valida el JWT y extrae sub + email]
        |
        v
[ms-auth verifica si ya existe por cognito_sub]
        |
        +--- Existe  → devuelve created = false
        |
        +--- No existe → crea registro con status ACTIVE → devuelve created = true
        |
        v
[React redirige a completar perfil (si created = true)]
o directamente al feed (si created = false)
```

## Decisiones de diseño

| Decisión | Razón |
|---|---|
| `ms-auth` separado de `ms-users` | Responsabilidades distintas: autenticación vs perfil de negocio. |
| BD propia (`heypudu_auth`) | Principio de database-per-service. |
| `cognito_sub` como clave externa | Es la identidad del usuario en Cognito. |
| `status` como `VARCHAR` con `CHECK` | Evita valores inválidos sin usar `enum` de Java. |
| `created` en `SyncUserResponse` | Permite al frontend decidir si redirige a completar perfil. |
| Roles desde el JWT, no desde BD | Cognito es la fuente de verdad de roles. |
| Prefijo `ROLE_` en authorities | Convención de Spring Security para `hasRole(...)`. |
| `ddl-auto: validate` | Flyway gestiona el esquema, Hibernate solo valida. |

## Trabajo futuro

- **Comunicación con `ms-users`**: al crear un usuario en `ms-auth`, notificar a `ms-users` para crear el perfil base. Para la EP1, el frontend lo hace llamando directamente a `POST /api/v1/users`.
- **Sincronización asíncrona**: usar SQS/SNS para propagar cambios de perfil entre microservicios.
- **Rotación de credenciales**: integrar AWS Secrets Manager para las credenciales de BD.
- **Tests**: agregar pruebas unitarias e integración con Testcontainers.

## Autor

Proyecto desarrollado para la asignatura **DSY1107 - Desarrollo Cloud Native**.