# ms-heypudu-rabbit-admin

Microservicio administrativo para declarar y eliminar exchanges, colas y
bindings de RabbitMQ en heyPudú!.

El servicio utiliza `RabbitAdmin` de Spring AMQP y se conecta al mismo broker
que usan los microservicios de publicaciones, interacciones y usuarios.

## Responsabilidad

Permite administrar dinámicamente:

- Exchanges `direct` y `topic`.
- Colas quorum durables.
- Bindings entre exchanges y colas.
- TTL, longitud máxima, límite de entregas y dead lettering de las colas.

No consume mensajes de negocio ni reemplaza RabbitMQ Management UI. Su
objetivo es exponer una API autenticada para operaciones controladas sobre la
topología.

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 25 |
| Framework | Spring Boot 3.5.x |
| Build | Gradle 8.10.2+ |
| Seguridad | Spring Security + OAuth2 Resource Server |
| Autenticación | AWS Cognito (JWT) |
| Mensajería | RabbitMQ mediante Spring AMQP |
| Monitoreo | Spring Boot Actuator |

## Arquitectura

```text
[Administrador con JWT Cognito]
        |
        v
[API Gateway / ALB]
        |
        v
[ms-heypudu-rabbit-admin :8085]
        |
        v
[RabbitMQ]
```

- **Puerto:** `8085`
- **Base de datos:** no utiliza base de datos propia.
- **Broker:** RabbitMQ configurado mediante variables `RABBITMQ_*`.
- **Acceso:** todas las operaciones administrativas requieren `ROLE_ADMIN`.

## Seguridad y roles

El JWT debe incluir el grupo Cognito `admin`.

El conversor de seguridad transforma el valor del claim `cognito:groups` a
autoridad Spring:

```text
cognito:groups = ["admin"]
        |
        v
ROLE_ADMIN
```

`admin` es el nombre del grupo en Cognito; `ROLE_ADMIN` es la autoridad que
Spring utiliza en `@PreAuthorize`. No son dos grupos diferentes que deban
crearse en Cognito.

## Endpoints

Todos están bajo `/api/v1/admin/rabbit` y requieren:

- JWT válido.
- Grupo Cognito `admin`.
- Autoridad Spring `ROLE_ADMIN`.

| Método | Endpoint | Respuesta exitosa |
|---|---|---|
| `POST` | `/api/v1/admin/rabbit/exchanges/{name}` | `204 No Content` |
| `DELETE` | `/api/v1/admin/rabbit/exchanges/{name}` | `204 No Content` |
| `POST` | `/api/v1/admin/rabbit/queues/{name}` | `204 No Content` |
| `DELETE` | `/api/v1/admin/rabbit/queues/{name}` | `204 No Content` |
| `POST` | `/api/v1/admin/rabbit/bindings` | `204 No Content` |
| `DELETE` | `/api/v1/admin/rabbit/bindings` | `204 No Content` |

### Crear exchange

`POST /api/v1/admin/rabbit/exchanges/{name}`

Payload:

```json
{
  "type": "topic"
}
```

`type` solo admite `direct` o `topic`, sin distinguir mayúsculas y
minúsculas. Los exchanges se declaran durables y no autoeliminables.

### Eliminar exchange

`DELETE /api/v1/admin/rabbit/exchanges/{name}`

No recibe body. RabbitMQ eliminará el exchange y sus bindings asociados según
las reglas del broker. El nombre debe pertenecer al namespace permitido.

### Crear cola

`POST /api/v1/admin/rabbit/queues/{name}`

Payload completo:

```json
{
  "messageTtlMs": 3600000,
  "maxLength": 10000,
  "deliveryLimit": 3,
  "deadLetterExchange": "ex.heypudu.dlx",
  "deadLetterRoutingKey": "q.heypudu.command"
}
```

Todos los campos son opcionales. Si no se envían, se utilizan:

| Propiedad | Default | Validación |
|---|---:|---|
| `messageTtlMs` | `3600000` | Mínimo `1000` |
| `maxLength` | `10000` | Mínimo `1` |
| `deliveryLimit` | `3` | Mínimo `1` |
| `deadLetterExchange` | Sin DLX | Debe usar `ex.heypudu.*` |
| `deadLetterRoutingKey` | Sin routing key DLX | Debe enviarse junto al exchange |

Las colas se declaran como quorum, durables y con overflow
`reject-publish`. Si se configura dead lettering, ambos campos deben enviarse
juntos.

### Eliminar cola

`DELETE /api/v1/admin/rabbit/queues/{name}`

No recibe body. La cola debe usar el namespace permitido.

### Crear o eliminar binding

`POST` o `DELETE /api/v1/admin/rabbit/bindings`

Payload:

```json
{
  "exchange": "ex.heypudu.events",
  "queue": "q.heypudu.notification",
  "routingKey": "interaction.*"
}
```

Los bindings requieren exchange, cola y routing key no vacíos. La routing key
solo puede contener letras minúsculas, números, puntos, guiones, `*` o `#`.

## Validaciones de nombres

| Recurso | Formato permitido | Ejemplo |
|---|---|---|
| Exchange | `ex.heypudu.<nombre>` | `ex.heypudu.events` |
| Cola | `q.heypudu.<nombre>` | `q.heypudu.notification` |
| Routing key | minúsculas, números, `.`, `-`, `*`, `#` | `interaction.*` |

Los nombres de exchanges y colas aceptan segmentos en minúsculas, números,
puntos y guiones después del prefijo.

## Errores HTTP

Las respuestas de error tienen esta forma:

```json
{
  "timestamp": "2026-10-06T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "El tipo de exchange debe ser direct o topic",
  "path": "/api/v1/admin/rabbit/exchanges/ex.heypudu.events"
}
```

| Estado | Causa |
|---:|---|
| `400` | Nombre, tipo, routing key o payload inválido |
| `401` | JWT ausente o inválido |
| `403` | JWT válido sin grupo `admin`/autoridad `ROLE_ADMIN` |
| `503` | RabbitMQ no disponible |
| `500` | Error interno no controlado |

## Topología actual de heyPudú

La infraestructura productiva define:

| Tipo | Nombre |
|---|---|
| Exchange direct | `ex.heypudu.cmd` |
| Exchange topic | `ex.heypudu.events` |
| Exchange DLX | `ex.heypudu.dlx` |
| Cola | `q.heypudu.command` |
| Cola | `q.heypudu.notification` |
| Cola | `q.heypudu.audit` |

Las colas funcionales tienen sus DLQ correspondientes. Los comandos, eventos
y consumidores se documentan en el README principal del backend y en los
README de cada microservicio.

## Configuración

| Variable | Descripción |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Perfil activo, normalmente `prod` |
| `COGNITO_ISSUER_URI` | Issuer del User Pool de Cognito |
| `COGNITO_JWK_SET_URI` | URL JWKS de Cognito |
| `RABBITMQ_HOST` | Host del broker |
| `RABBITMQ_PORT` | Puerto AMQP, normalmente `5672` |
| `RABBITMQ_USERNAME` | Usuario AMQP |
| `RABBITMQ_PASSWORD` | Contraseña AMQP |
| `RABBITMQ_VHOST` | Virtual host AMQP, por defecto `/` |

No se deben versionar contraseñas, tokens ni archivos `.env` reales.

## Ejecución local

### Requisitos

- JDK 25.
- Docker y Docker Compose.
- RabbitMQ accesible.
- User Pool de Cognito para probar autenticación administrativa.

Desde `heyPudu-backend/`:

```bash
docker compose -f docker-compose.rabbit.yml up -d
```

Luego, desde este directorio:

```bash
./gradlew bootRun
```

Verificar:

```bash
curl http://localhost:8085/actuator/health
```

Para probar la API se debe enviar un JWT de un usuario perteneciente al grupo
Cognito `admin`:

```bash
curl -X POST "http://localhost:8085/api/v1/admin/rabbit/exchanges/ex.heypudu.demo" \
  -H "Authorization: Bearer <id-token>" \
  -H "Content-Type: application/json" \
  -d '{"type":"topic"}'
```

## Relación con RabbitMQ Management UI

Este servicio administra la topología mediante HTTP, pero no muestra métricas
ni gráficos. Para inspección visual de exchanges, colas, bindings, mensajes y
consumidores se utiliza RabbitMQ Management UI, normalmente a través de un
túnel SSH hacia el host del broker.

## Decisiones de diseño

| Decisión | Razón |
|---|---|
| Namespace obligatorio | Evita modificar recursos ajenos al proyecto |
| Colas quorum | Mayor tolerancia a fallos que una cola clásica |
| Defaults seguros | Mantiene TTL, límites y DLQ aunque el cliente omita campos |
| `reject-publish` | Evita aceptar mensajes por encima del límite configurado |
| `ROLE_ADMIN` | Protege la administración con el mismo modelo Cognito del backend |
| Respuesta `503` para RabbitMQ | Expone claramente la indisponibilidad del broker |

## Autor

Proyecto desarrollado para la asignatura **DSY1107 - Desarrollo Cloud Native**.
