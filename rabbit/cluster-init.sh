#!/bin/sh
# ============================================================
# Une rabbit@rabbit2 al cluster que forma rabbit@rabbit1.
# Se ejecuta una sola vez, cuando ambos nodos ya responden.
# Es idempotente: si ya estan unidos, no hace nada.
# ============================================================
set -eu

echo "$RABBITMQ_ERLANG_COOKIE" > "$HOME/.erlang.cookie"
chmod 400 "$HOME/.erlang.cookie"

echo "Esperando a que rabbit@rabbit1 responda..."
until rabbitmqctl -n rabbit@rabbit1 ping >/dev/null 2>&1; do
  sleep 2
done

echo "Esperando a que rabbit@rabbit2 responda..."
until rabbitmqctl -n rabbit@rabbit2 ping >/dev/null 2>&1; do
  sleep 2
done

if rabbitmqctl -n rabbit@rabbit1 cluster_status 2>/dev/null | grep -q 'rabbit@rabbit2'; then
  echo "rabbit@rabbit2 ya forma parte del cluster."
else
  echo "Uniendo rabbit@rabbit2 al cluster de rabbit@rabbit1..."
  rabbitmqctl -n rabbit@rabbit2 stop_app
  rabbitmqctl -n rabbit@rabbit2 reset
  rabbitmqctl -n rabbit@rabbit2 join_cluster rabbit@rabbit1
  rabbitmqctl -n rabbit@rabbit2 start_app
fi

echo "Estado del cluster:"
rabbitmqctl -n rabbit@rabbit1 cluster_status
