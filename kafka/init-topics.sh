#!/usr/bin/env bash
# ============================================================
# Declara los tópicos de Kafka de heyPudú! y termina.
#
# Idempotente: si el tópico ya existe no falla ni cambia su
# configuración.
#
# Variables (con valores por defecto para local):
#   KAFKA_BOOTSTRAP_SERVERS         kafka:9092
#   KAFKA_TOPIC_LIVE_EVENTS         heypudu.live.events
#   KAFKA_TOPIC_PARTITIONS          3
#   KAFKA_TOPIC_REPLICATION_FACTOR  1
#   KAFKA_TOPIC_RETENTION_MS        604800000 (7 días)
# ============================================================
set -euo pipefail

KAFKA_BIN="/opt/kafka/bin"
BOOTSTRAP="${KAFKA_BOOTSTRAP_SERVERS:-kafka:9092}"
TOPIC="${KAFKA_TOPIC_LIVE_EVENTS:-heypudu.live.events}"
PARTITIONS="${KAFKA_TOPIC_PARTITIONS:-3}"
REPLICATION_FACTOR="${KAFKA_TOPIC_REPLICATION_FACTOR:-1}"
RETENTION_MS="${KAFKA_TOPIC_RETENTION_MS:-604800000}"

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

if "${KAFKA_BIN}/kafka-topics.sh" --bootstrap-server "${BOOTSTRAP}" --list | grep -qx "${TOPIC}"; then
  echo "El tópico ${TOPIC} ya existe; no se modifica."
else
  echo "Creando tópico ${TOPIC} (particiones=${PARTITIONS}, replicas=${REPLICATION_FACTOR})..."
  "${KAFKA_BIN}/kafka-topics.sh" --bootstrap-server "${BOOTSTRAP}" --create \
    --topic "${TOPIC}" \
    --partitions "${PARTITIONS}" \
    --replication-factor "${REPLICATION_FACTOR}" \
    --config retention.ms="${RETENTION_MS}" \
    --config cleanup.policy=delete
fi

echo "Configuración actual del tópico:"
"${KAFKA_BIN}/kafka-topics.sh" --bootstrap-server "${BOOTSTRAP}" --describe --topic "${TOPIC}"
