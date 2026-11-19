# ms-analytics

Microservicio de analítica en tiempo real para **heyPudú!**.

## Responsabilidad

Consume los eventos del tópico `heypudu.live.events` y calcula, por sala:

- Oyentes activos (`LISTENER_JOINED` / `LISTENER_LEFT`).
- Reacciones totales y reacciones por minuto (`REACTION`).
- Idempotencia por `eventId` para no contar dos veces tras un rebalanceo.

Las métricas viven en memoria: la demo corre una sola instancia. El histórico
de eventos queda en Kafka según la retención del tópico.

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 25 |
| Framework | Spring Boot 3.5.x |
| Build | Gradle 9.7.1 |
| Seguridad | OAuth2 Resource Server con Cognito |
| Mensajería | Spring Kafka (`live-analytics-group`) |

## Endpoints

| Método | Ruta | Uso |
|---|---|---|
| `GET` | `/api/v1/analytics/rooms/{id}/stats` | Métricas de la sala |

Requiere JWT de Cognito.

## Configuración

| Variable | Descripción |
|---|---|
| `COGNITO_ISSUER_URI` | Issuer del User Pool |
| `COGNITO_JWK_SET_URI` | URL JWKS |
| `KAFKA_BOOTSTRAP_SERVERS` | Brokers Kafka |
| `KAFKA_TOPIC_LIVE_EVENTS` | Tópico de eventos (`heypudu.live.events`) |
| `KAFKA_CONSUMER_GROUP` | Grupo consumidor (`live-analytics-group`) |

## Ejecución local

```bash
cd heyPudu-backend
docker compose -f docker-compose.kafka.yml up -d
docker compose up -d ms-analytics
```

Verificar:

```bash
curl http://localhost:8087/actuator/health
```
