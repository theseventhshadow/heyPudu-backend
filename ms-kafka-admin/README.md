# ms-kafka-admin

Microservicio administrador de Kafka para heyPudú!. Expone por REST las
operaciones de administración del clúster mediante el `AdminClient` de Kafka,
igual que `ms-heypudu-rabbit-admin` lo hace con `RabbitAdmin` para AMQP.

- Puerto: **8088**
- Seguridad: JWT de Cognito, todas las rutas exigen `ROLE_ADMIN`
- Sin base de datos: solo habla con el clúster

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/v1/admin/kafka/cluster` | Brokers vivos, controlador y `clusterId` |
| `GET` | `/api/v1/admin/kafka/topics` | Lista tópicos con particiones, RF e `internal` |
| `GET` | `/api/v1/admin/kafka/topics/{topic}` | Detalle con líder/réplicas/ISR por partición y configuraciones |
| `POST` | `/api/v1/admin/kafka/topics` | Crea un tópico con particiones, RF, retención y cleanup policy |
| `PATCH` | `/api/v1/admin/kafka/topics/{topic}/config` | Modifica configuraciones de forma incremental |
| `DELETE` | `/api/v1/admin/kafka/topics/{topic}` | Elimina el tópico |
| `GET` | `/api/v1/admin/kafka/groups` | Lista los consumer groups |
| `GET` | `/api/v1/admin/kafka/groups/{group}` | Estado, miembros y LAG por partición |

Ejemplo de creación:

```bash
curl -X POST "$API/api/v1/admin/kafka/topics" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"heypudu.tmp.events","partitions":2,"replicationFactor":2,
       "retentionMs":86400000,"cleanupPolicy":"delete","minInsyncReplicas":1}'
```

Ejemplo de cambio de retención:

```bash
curl -X PATCH "$API/api/v1/admin/kafka/topics/heypudu.live.events/config" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"configs":{"retention.ms":"1209600000"}}'
```

## Variables de entorno

| Variable | Uso |
|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | Brokers, por ejemplo `broker1:9092,broker2:9092` |
| `COGNITO_ISSUER_URI` / `COGNITO_JWK_SET_URI` | Validación del JWT |
| `SPRING_PROFILES_ACTIVE` | `dev` o `prod` |

## Ejecución local

```bash
./gradlew bootRun                 # requiere Kafka en localhost:9092
./gradlew build                   # compila y pasa las pruebas
```

Con Docker:

```bash
docker build -t unalmaermitana/heypudu-ms-kafka-admin:latest .
```
