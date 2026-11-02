# ms-auth

Microservicio de autenticación y sincronización de usuarios para **heyPudú!**.

## Responsabilidad

`ms-auth` es el punto de integración entre AWS Cognito y el dominio de
usuarios. No almacena contraseñas ni implementa el login; Cognito gestiona las
credenciales y emite los JWT.

El servicio:

- Sincroniza usuarios autenticados de Cognito con `heypudu_auth`.
- Devuelve los datos del usuario autenticado a partir del JWT.
- Crea el perfil base correspondiente en `ms-users`.
- Administra operaciones de Cognito solicitadas por `ms-users`: deshabilitar,
  habilitar y eliminar usuarios.

`ms-auth` no utiliza RabbitMQ. Sus operaciones de sincronización y
administración de Cognito son llamadas HTTP síncronas, mientras que RabbitMQ
se utiliza entre los microservicios de publicaciones, interacciones y
usuarios.

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 25 |
| Framework | Spring Boot 3.5.x |
| Build | Gradle 8.10.2+ |
| Seguridad | Spring Security + OAuth2 Resource Server |
| Identidad | AWS Cognito User Pool |
| Administración Cognito | AWS SDK v2 |
| Persistencia | Spring Data JPA + PostgreSQL |
| Migraciones | Flyway |
| Comunicación HTTP | RestClient hacia `ms-users` |
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
[ms-auth :8081] ---> [ms-users :8082]
        |
        `--> [AWS Cognito User Pool]
```

- **Puerto:** `8081`
- **Esquema de BD:** `heypudu_auth`
- **Dependencia HTTP:** `ms-users` durante el primer `sync`.
- **Proveedor de identidad:** AWS Cognito.

## Estructura del proyecto

```text
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
            └── V1__init_auth_schema.sql
```

## Endpoints

### Autenticación y sincronización

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/v1/auth/me` | Datos del usuario autenticado |
| `POST` | `/api/v1/auth/sync` | Sincronizar Cognito, `ms-auth` y `ms-users` |

Ambos endpoints requieren un JWT válido de Cognito.

### Internos

| Método | Endpoint | Descripción |
|---|---|---|
| `PUT` | `/api/v1/internal/users/{cognitoSub}/disable` | Deshabilitar en Cognito |
| `PUT` | `/api/v1/internal/users/{cognitoSub}/enable` | Habilitar en Cognito |
| `DELETE` | `/api/v1/internal/users/{cognitoSub}` | Eliminar de Cognito |

Estos endpoints son invocados por `ms-users` durante el baneo, desbaneo o
eliminación de una cuenta.

### Salud

| Método | Endpoint |
|---|---|
| `GET` | `/actuator/health` |
| `GET` | `/actuator/info` |

## Detalle del `sync`

`POST /api/v1/auth/sync` es idempotente:

- Si el usuario no existe en `auth_users`, crea el registro con estado `ACTIVE`
  y llama a `ms-users` para crear el perfil base.
- Si ya existe, devuelve el registro existente y no duplica el perfil.

Respuesta de ejemplo:

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

El perfil base usa valores temporales (`Usuario` y `user_<sub8>`) hasta que el
usuario completa sus datos desde el frontend.

## Modelo de datos

### Tabla `auth_users` (esquema `heypudu_auth`)

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | `UUID` | `PRIMARY KEY` |
| `cognito_sub` | `UUID` | `NOT NULL UNIQUE` |
| `email` | `VARCHAR(255)` | `NOT NULL` |
| `status` | `VARCHAR(20)` | `NOT NULL DEFAULT 'ACTIVE'` |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` |

Los datos completos del perfil, seguidores y notificaciones viven en
`ms-users`.

## Flujos principales

### Registro y primer acceso

```text
[Usuario se registra en Cognito mediante Amplify]
        |
        v
[Cognito emite el JWT]
        |
        v
[React llama a POST /api/v1/auth/sync]
        |
        v
[ms-auth extrae sub y email del JWT]
        |
        +--> Usuario existente: devuelve created=false
        |
        `--> Usuario nuevo:
              +--> Inserta auth_users
              +--> POST /api/v1/internal/users en ms-users
              `--> Devuelve created=true
```

### Baneo

```text
[Admin solicita ban en ms-users]
        |
        v
[ms-users actualiza status y llama a ms-auth]
        |
        v
[ms-auth ejecuta Cognito AdminDisableUser]
```

### Desbaneo

```text
[Admin solicita unban en ms-users]
        |
        v
[ms-users llama a ms-auth]
        |
        v
[ms-auth ejecuta Cognito AdminEnableUser]
```

### Eliminación de cuenta

```text
[Usuario elimina su cuenta en ms-users]
        |
        v
[ms-users anonimiza el perfil y cambia status a DELETED]
        |
        v
[ms-users llama a ms-auth]
        |
        v
[ms-auth ejecuta Cognito AdminDeleteUser]
```

## Configuración

| Variable | Descripción |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Perfil activo, normalmente `prod` |
| `DB_URL` | JDBC con `currentSchema=heypudu_auth` |
| `DB_USERNAME` | Usuario de PostgreSQL |
| `DB_PASSWORD` | Contraseña de PostgreSQL |
| `COGNITO_ISSUER_URI` | Issuer del User Pool |
| `COGNITO_JWK_SET_URI` | URL JWKS de Cognito |
| `COGNITO_USER_POOL_ID` | ID del User Pool |
| `AWS_REGION` | Región de AWS |
| `CLIENTS_USERS_BASE_URL` | URL base de `ms-users` |

No se deben versionar contraseñas, tokens ni archivos `.env` reales.

### Permisos IAM requeridos

El rol de ejecución necesita:

- `cognito-idp:AdminDisableUser`
- `cognito-idp:AdminEnableUser`
- `cognito-idp:AdminDeleteUser`

## Ejecución local

### Requisitos

- JDK 25.
- Docker y PostgreSQL.
- Credenciales AWS configuradas para el SDK.
- `ms-users` ejecutándose en `:8082` para el flujo de sincronización.

Desde `heyPudu-backend/` se puede levantar PostgreSQL con la infraestructura
local del proyecto o iniciar un contenedor compatible con `DB_URL`.

Luego, desde este directorio:

```bash
./gradlew bootRun
```

Verificar:

```bash
curl http://localhost:8081/actuator/health
```

## Decisiones de diseño

| Decisión | Razón |
|---|---|
| Cognito como proveedor de identidad | Evita almacenar contraseñas propias |
| `ms-auth` separado de `ms-users` | Aísla identidad de la lógica de perfil |
| BD propia (`heypudu_auth`) | Aplica database-per-service |
| `cognito_sub` como identidad externa | Relaciona el JWT con el registro local |
| `sync` idempotente | Evita duplicar usuarios y perfiles |
| SDK de Cognito solo en `ms-auth` | Centraliza operaciones administrativas |
| Comunicación HTTP con `ms-users` | El flujo de sync requiere respuesta inmediata |

## Trabajo futuro

- Token de servicio para proteger los endpoints internos.
- Retry y circuit breaker para llamadas a `ms-users`.
- Auditoría centralizada de operaciones administrativas.
- Reconciliación periódica entre Cognito y `auth_users`.

## Autor

Proyecto desarrollado para la asignatura **DSY1107 - Desarrollo Cloud Native**.
