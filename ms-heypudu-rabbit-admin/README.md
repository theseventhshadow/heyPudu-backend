# ms-heypudu-rabbit-admin

Microservicio administrativo de la topología RabbitMQ de heyPudu.

## Seguridad

Todos los endpoints de administración requieren un JWT válido con el grupo
Cognito `ADMIN`, convertido a la autoridad `ROLE_ADMIN`.

## Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/v1/admin/rabbit/exchanges/{name}` | Crear exchange `direct` o `topic` |
| `DELETE` | `/api/v1/admin/rabbit/exchanges/{name}` | Eliminar exchange |
| `POST` | `/api/v1/admin/rabbit/queues/{name}` | Crear cola quorum durable |
| `DELETE` | `/api/v1/admin/rabbit/queues/{name}` | Eliminar cola |
| `POST` | `/api/v1/admin/rabbit/bindings` | Crear binding |
| `DELETE` | `/api/v1/admin/rabbit/bindings` | Eliminar binding |

Los nombres se restringen al namespace `ex.heypudu.*` y `q.heypudu.*`.
Las colas se crean con TTL, longitud máxima, límite de entregas y overflow
`reject-publish`; sus valores tienen defaults seguros y pueden sobreescribirse
en la solicitud.
