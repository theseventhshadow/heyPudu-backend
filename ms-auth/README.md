# ms-auth

Microservicio de autenticación y sincronización de usuarios para **heyPudú!**.

## Descripción

`ms-auth` es el punto de entrada entre AWS Cognito y el resto del sistema. No gestiona credenciales (eso lo hace Cognito), sino que:

- Sincroniza el usuario de Cognito con la base de datos propia del microservicio.
- Devuelve los datos del usuario autenticado a partir del JWT.
- Registra qué usuarios han iniciado sesión y su estado (`ACTIVE`, `BANNED`, `DELETED`).
- Al crear un usuario nuevo, crea también su perfil base en `ms-users`.
- Expone endpoints internos para deshabilitar, habilitar o eliminar usuarios en Cognito (llamados por `ms-users` al banear o eliminar cuentas).

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 25 |
| Framework | Spring Boot 3.5.x |
| Build | Gradle 8.10.2+ |
| Seguridad | Spring Security + OAuth2 Resource Server |
| Autenticación | AWS Cognito (JWT) |
| Administración Cognito | AWS SDK v2 (cognitoidentityprovider) |
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
[ms-auth :8081]  --->  [ms-users :8082]   (crear perfil base)
        |
        | (AdminDisable / AdminEnable / AdminDelete)
        v
[AWS Cognito]

[ms-users] ---> [ms-auth] (endpoints internos para habilitar/deshabilitar/eliminar)
```

- **Puerto:** `8081`
- **Esquema de BD:** `heypudu_auth`
- **IdP:** AWS Cognito (User Pool)
- **Depende de:** `ms-users`

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
    │   │   ├── JwtConfig.java
    │   │   ├── RestClientConfig.java
    │   │   └── CognitoConfig.java
    │   ├── controller/
    │   │   ├── AuthController.java
    │   │   └── InternalAuthController.java
    │   ├── client/
    │   │   └── UserClient.java
    │   ├── dto/
    │   │   ├── request/
    │   │   │   ├── SyncUserRequest.java
    │   │   │   └── CreateUserProfileRequest.java
    │   │   └── response/
    │   │       ├── AuthUserResponse.java
    │   │       ├── SyncUserResponse.java
    │   │       └── UserSummaryResponse.java
    │   ├── exception/
    │   │   ├── GlobalExceptionHandler.java
    │   │   ├── ResourceNotFoundException.java
    │   │   └── BusinessException.java
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
    │       ├── CognitoAdminService.java
    │       └── impl/
    │           ├── AuthServiceImpl.java
    │           └── CognitoAdminServiceImpl.java
    └── resources/
        ├── application.yml
        ├── application-dev.yml
        ├── application-prod.yml
        └── db/migration/
            └── V1__init_auth_schema.sql
```

## Endpoints

### Endpoints públicos

| Método | Endpoint | Descripción | Rol |
|---|---|---|---|
| `GET` | `/api/v1/auth/me` | Datos del usuario autenticado | `user`, `admin` |
| `POST` | `/api/v1/auth/sync` | Sincronizar usuario de Cognito con la BD y con `ms-users` | `user`, `admin` |

### Endpoints internos

| Método | Endpoint | Descripción |
|---|---|---|
| `PUT` | `/api/v1/internal/users/{cognitoSub}/disable` | Deshabilitar usuario en Cognito |
| `PUT` | `/api/v1/internal/users/{cognitoSub}/enable` | Habilitar usuario en Cognito |
| `DELETE` | `/api/v1/internal/users/{cognitoSub}` | Eliminar usuario de Cognito |

### Salud

| Método | Endpoint |
|---|---|
| `GET` | `/actuator/health` |
| `GET` | `/actuator/info` |

## Detalle de los endpoints principales

### `GET /api/v1/auth/me`

Devuelve los datos del usuario autenticado a partir del JWT.

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

### `POST /api/v1/auth/sync`

Sincroniza el usuario. **Idempotente**.

- Si el usuario no existe:
  1. Lo crea en `auth_users` con `status = ACTIVE`.
  2. Llama a `ms-users` para crear el perfil base con valores temporales.
  3. Devuelve `created = true`.
- Si el usuario ya existe:
  1. Devuelve el registro existente con `created = false`.
  2. **No** vuelve a llamar a `ms-users`.

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

## Modelo de datos

### Tabla `auth_users` (esquema `heypudu_auth`)

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | `UUID` | `PRIMARY KEY` |
| `cognito_sub` | `UUID` | `NOT NULL UNIQUE` |
| `email` | `VARCHAR(255)` | `NOT NULL` |
| `status` | `VARCHAR(20)` | `NOT NULL DEFAULT 'ACTIVE'` |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` |

> **Nota:** los datos completos del perfil viven en `ms-users`.

## Flujo de sincronización

```
[Usuario se registra en Cognito desde React con Amplify]
        |
        v
[Cognito crea la cuenta y devuelve JWT]
        |
        v
[React llama a POST /api/v1/auth/sync]
        |
        v
[ms-auth valida el JWT y extrae sub + email]
        |
        v
[ms-auth verifica si ya existe por cognito_sub]
        |
        +--- Existe → devuelve created = false
        |
        +--- No existe
                |
                v
        [ms-auth crea registro en auth_users]
                |
                v
        [ms-auth llama a ms-users: POST /api/v1/internal/users
         con cognitoSub, name="Usuario", username="user_<sub8>"]
                |
                v
        [ms-users crea el perfil base]
                |
                v
        [ms-auth devuelve created = true]
                |
                v
        [React redirige a "completar perfil"]
                |
                v
        [Usuario llama a PUT /api/v1/users/me con datos reales]
```

## Flujo de baneo

```
[Admin: PUT /api/v1/admin/users/{id}/ban en ms-users]
        |
        v
[ms-users cambia status a BANNED]
        |
        v
[ms-users llama a PUT /api/v1/internal/users/{sub}/disable en ms-auth]
        |
        v
[ms-auth llama a Cognito AdminDisableUser]
        |
        v
[Cognito deshabilita al usuario]
```

## Flujo de eliminación de cuenta

```
[Usuario: DELETE /api/v1/users/me en ms-users]
        |
        v
[ms-users anonimiza: name, username, bio, avatar_key]
        |
        v
[ms-users cambia status a DELETED]
        |
        v
[ms-users llama a DELETE /api/v1/internal/users/{sub} en ms-auth]
        |
        v
[ms-auth llama a Cognito AdminDeleteUser]
        |
        v
[Cognito elimina al usuario]
```

## Configuración

### Variables de entorno en producción

| Variable | Descripción |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | JDBC con `currentSchema=heypudu_auth` |
| `DB_USERNAME` | Usuario BD |
| `DB_PASSWORD` | Contraseña BD |
| `COGNITO_ISSUER_URI` | Issuer Cognito |
| `COGNITO_JWK_SET_URI` | JWKS Cognito |
| `COGNITO_USER_POOL_ID` | ID del User Pool de Cognito |
| `AWS_REGION` | Región de AWS |
| `CLIENTS_USERS_BASE_URL` | URL de `ms-users` |

### Permisos requeridos en Cognito

El rol de IAM del EC2 debe tener permisos para:

- `cognito-idp:AdminDisableUser`
- `cognito-idp:AdminEnableUser`
- `cognito-idp:AdminDeleteUser`

## Ejecución local

### Requisitos

- JDK 25
- PostgreSQL (Docker)
- Credenciales AWS configuradas (`aws configure` o variables de entorno)
- `ms-users` corriendo en `:8082`

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
CREATE SCHEMA IF NOT EXISTS heypudu_auth;
CREATE USER heypudu_auth_user WITH PASSWORD 'devpassword';
GRANT ALL PRIVILEGES ON SCHEMA heypudu_auth TO heypudu_auth_user;
```

### Ejecutar

```bash
./gradlew bootRun
```

## Decisiones de diseño

| Decisión | Razón |
|---|---|
| `ms-auth` separado de `ms-users` | Responsabilidades distintas: autenticación vs perfil de negocio |
| BD propia (`heypudu_auth`) | Principio de database-per-service |
| `cognito_sub` como clave externa | Identidad del usuario en Cognito |
| El sync llama a `ms-users` | El frontend solo hace una llamada; el sistema crea ambos registros |
| `username` temporal `user_<sub8>` | El usuario completa su perfil después |
| Endpoints internos abiertos (EP1) | Consistencia con los demás microservicios |
| SDK de Cognito solo en `ms-auth` | Es el único microservicio que administra usuarios en Cognito |

## Trabajo futuro

- Token de servicio para endpoints internos
- Retry con Resilience4j hacia `ms-users`
- Sincronización asíncrona con SQS/SNS

## Autor

Proyecto desarrollado para la asignatura **DSY1107 - Desarrollo Cloud Native**.