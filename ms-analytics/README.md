# ms-analytics

Microservicio de analítica en tiempo real para **heyPudú!**.

## Responsabilidad

Consume los eventos del tópico `heypudu.live.events` y calcula, por sala:

- Oyentes activos (`LISTENER_JOINED` / `LISTENER_LEFT`).
- Reacciones totales y reacciones por minuto (`REACTION`).
- Idempotencia por `eventId` para no contar dos veces tras un rebalanceo.

Las métricas viven en memoria: la demo corre una sola instancia. El histórico
de eventos se materializa en PostgreSQL mediante un **segundo consumer group**
(`live-audit-group`), de modo que el tópico se puede reprocesar sin duplicar
filas (la PK es el `eventId`).

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 25 |
| Framework | Spring Boot 3.5.x |
| Build | Gradle 9.7.1 |
| Seguridad | OAuth2 Resource Server con Cognito |
| Mensajería | Spring Kafka (`live-analytics-group`, `live-audit-group`) |
| Persistencia | PostgreSQL (`heypudu_analytics`) + Flyway |

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
| `KAFKA_AUDIT_GROUP` | Grupo de auditoría (`live-audit-group`) |
| `DB_URL` | JDBC de PostgreSQL (`...currentSchema=heypudu_analytics`) |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciales del esquema `heypudu_analytics` |

## Auditoría de eventos

Los dos grupos consumen el **mismo** tópico con offsets independientes:

| Grupo | Destino | Comportamiento ante fallo |
|---|---|---|
| `live-analytics-group` | Métricas en memoria | Best-effort: registra y confirma |
| `live-audit-group` | Tabla `heypudu_analytics.live_events_audit` | Solo confirma si la fila se escribió |

Tabla `live_events_audit`: `event_id` (PK, idempotencia), `event_type`,
`room_id`, `creator_cognito_sub`, `actor_cognito_sub`, `reaction_type`,
`schema_version`, `occurred_at`, `ingested_at`.

### Reproceso del tópico

Para reconstruir el histórico desde Kafka (por ejemplo tras una caída):

```bash
docker exec heypudu-kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group live-audit-group \
  --topic heypudu.live.events \
  --reset-offsets --to-earliest --execute
```

Al reiniciar el servicio se reinsertan los eventos retenidos y los que ya
existían se ignoran por PK, sin duplicados.

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
