# ms-live

Microservicio de salas de audio en vivo para **heyPudú!**.

## Responsabilidad

Gestiona el ciclo de vida de las salas activas:

- Crear una sala activa.
- Impedir que un creador tenga más de una sala activa.
- Consultar salas activas y el detalle de una sala.
- Cerrar una sala solo por su creador.
- Consultar si un oyente puede escuchar una sala.
- Registrar reacciones de oyentes y publicarlas en Kafka.
- Construir las URLs WHIP/WHEP desde la URL pública de MediaMTX.

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 25 |
| Framework | Spring Boot 3.5.x |
| Build | Gradle 9.7.1 |
| Seguridad | OAuth2 Resource Server con Cognito |
| Persistencia | Spring Data JPA + PostgreSQL + Flyway |
| Kafka | Spring Kafka |
| RabbitMQ | Spring AMQP |
| Cliente HTTP | RestClient hacia `ms-users` |

## Endpoints

| Método | Ruta | Uso |
|---|---|---|
| `POST` | `/api/v1/live/rooms` | Crear sala |
| `DELETE` | `/api/v1/live/rooms/{id}` | Cerrar sala |
| `GET` | `/api/v1/live/rooms` | Listar salas activas |
| `GET` | `/api/v1/live/rooms/{id}` | Obtener detalle |
| `GET` | `/api/v1/live/rooms/{id}/access` | Consultar acceso |
| `POST` | `/api/v1/live/rooms/{id}/reactions` | Enviar reacción (`CLAP`/`HEART`) → 202 |

Las operaciones públicas requieren JWT de Cognito. Las reacciones se persisten en
`heypudu_live.live_reactions` y se publican en el tópico `heypudu.live.events`.

## RabbitMQ

- Publica `room.created` en `ex.heypudu.events` al abrir una sala; lo consume
  `ms-users` para notificar a los seguidores.
- Publica `room.closed` en `ex.heypudu.cmd` al cerrar la sala.
- Consume `q.heypudu.live.command` (con DLQ `q.heypudu.live.command.dlq`) para
  borrar las reacciones persistidas de la sala (decisión D1).

## Configuración

| Variable | Descripción |
|---|---|
| `DB_URL` | JDBC de PostgreSQL con `currentSchema=heypudu_live` |
| `DB_USERNAME` | Usuario de base de datos |
| `DB_PASSWORD` | Contraseña de base de datos |
| `COGNITO_ISSUER_URI` | Issuer del User Pool |
| `COGNITO_JWK_SET_URI` | URL JWKS |
| `CLIENTS_USERS_BASE_URL` | URL de `ms-users` |
| `KAFKA_BOOTSTRAP_SERVERS` | Brokers Kafka |
| `KAFKA_TOPIC_LIVE_EVENTS` | Tópico de eventos (por defecto `heypudu.live.events`) |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | Conexión AMQP |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | Credenciales AMQP |
| `MEDIAMTX_PUBLIC_URL` | URL pública de MediaMTX |
| `MEDIAMTX_SERVICE_TOKEN` | Token compartido futuro con MediaMTX |
| `LIVE_ROOM_MAX_DURATION_MINUTES` | Duración máxima configurada |

No versionar secretos reales.

## Ejecución local

```bash
cd heyPudu-backend
docker compose up -d postgres ms-live
```

Verificar:

```bash
curl http://localhost:8086/actuator/health
```
