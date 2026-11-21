#!/bin/sh
# ============================================================
# Entrypoint de la imagen de MediaMTX.
#
# MediaMTX no expande variables de entorno dentro del YAML, así que
# si existe una plantilla (por defecto /mediamtx.yml.template) se
# sustituyen solo las variables ${MEDIAMTX_*} y se escribe el
# /mediamtx.yml definitivo antes de arrancar.
#
# Se usa una lista explícita en envsubst para no romper las variables
# propias de los hooks ($MTX_PATH, $MTX_QUERY).
# ============================================================
set -e

template="${MEDIAMTX_CONFIG_TEMPLATE:-/mediamtx.yml.template}"

if [ -f "$template" ]; then
	envsubst '${MEDIAMTX_HOST} ${MEDIAMTX_SERVICE_TOKEN} ${MEDIAMTX_ALLOWED_ORIGIN}' \
		< "$template" > /mediamtx.yml
fi

exec "$@"
