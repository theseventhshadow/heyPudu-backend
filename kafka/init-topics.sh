#!/usr/bin/env bash
# ============================================================
# Declara los tópicos de Kafka de heyPudú! y termina.
#
# Idempotente: si el tópico ya existe no falla ni cambia su
# configuración.
#
# Variables (con valores por defecto para local):
#   KAFKA_BOOTSTRAP_SERVERS          broker1:9092,broker2:9092
#   KAFKA_TOPIC_LIVE_EVENTS          heypudu.live.events
#   KAFKA_TOPIC_POSTS_EVENTS         heypudu.posts.events
#   KAFKA_TOPIC_PARTITIONS           3   (salas en vivo)
#   KAFKA_TOPIC_POSTS_PARTITIONS     2   (publicaciones)
#   KAFKA_TOPIC_REPLICATION_FACTOR   2
#   KAFKA_TOPIC_RETENTION_MS         604800000 (7 días)
#   KAFKA_TOPIC_POSTS_RETENTION_MS   259200000 (3 días)
#   KAFKA_TOPIC_MIN_INSYNC_REPLICAS  1
# ============================================================
set -euo pipefail

# La ruta del binario cambia entre la imagen de Apache y la de Confluent.
if command -v kafka-topics.sh >/dev/null 2>&1; then
  KAFKA_BIN="$(dirname "$(command -v kafka-topics.sh)")"
else
  KAFKA_BIN="/opt/kafka/bin"
fi

BOOTSTRAP="${KAFKA_BOOTSTRAP_SERVERS:-broker1:9092,broker2:9092}"
LIVE_TOPIC="${KAFKA_TOPIC_LIVE_EVENTS:-heypudu.live.events}"
POSTS_TOPIC="${KAFKA_TOPIC_POSTS_EVENTS:-heypudu.posts.events}"
LIVE_PARTITIONS="${KAFKA_TOPIC_PARTITIONS:-3}"
POSTS_PARTITIONS="${KAFKA_TOPIC_POSTS_PARTITIONS:-2}"
REPLICATION_FACTOR="${KAFKA_TOPIC_REPLICATION_FACTOR:-2}"
RETENTION_MS="${KAFKA_TOPIC_RETENTION_MS:-604800000}"
POSTS_RETENTION_MS="${KAFKA_TOPIC_POSTS_RETENTION_MS:-259200000}"
MIN_INSYNC="${KAFKA_TOPIC_MIN_INSYNC_REPLICAS:-1}"

echo "Esperando a Kafka en ${BOOTSTRAP}..."
for _ in $(seq 1 30); do
  if "${KAFKA_BIN}/kafka-topics.sh" --bootstrap-server "${BOOTSTRAP}" --list >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

if ! "${KAFKA_BIN}/kafka-topics.sh" --bootstrap-server "${BOOTSTRAP}" --list >/dev/null 2>&1; then
  echo "ERROR: Kafka no respondió en ${BOOTSTRAP}" >&2
  exit 1
fi

create_topic() {
  local topic="$1" partitions="$2" retention="$3"
  if "${KAFKA_BIN}/kafka-topics.sh" --bootstrap-server "${BOOTSTRAP}" --list | grep -qx "${topic}"; then
    echo "El tópico ${topic} ya existe; no se modifica."
    return
  fi
  echo "Creando tópico ${topic} (particiones=${partitions}, replicas=${REPLICATION_FACTOR}, retencion=${retention}ms)..."
  "${KAFKA_BIN}/kafka-topics.sh" --bootstrap-server "${BOOTSTRAP}" --create \
    --topic "${topic}" \
    --partitions "${partitions}" \
    --replication-factor "${REPLICATION_FACTOR}" \
    --config retention.ms="${retention}" \
    --config cleanup.policy=delete \
    --config min.insync.replicas="${MIN_INSYNC}"
}

create_topic "${LIVE_TOPIC}" "${LIVE_PARTITIONS}" "${RETENTION_MS}"
create_topic "${POSTS_TOPIC}" "${POSTS_PARTITIONS}" "${POSTS_RETENTION_MS}"

echo
echo "Tópicos declarados:"
"${KAFKA_BIN}/kafka-topics.sh" --bootstrap-server "${BOOTSTRAP}" --describe --topic "${LIVE_TOPIC}"
"${KAFKA_BIN}/kafka-topics.sh" --bootstrap-server "${BOOTSTRAP}" --describe --topic "${POSTS_TOPIC}"
