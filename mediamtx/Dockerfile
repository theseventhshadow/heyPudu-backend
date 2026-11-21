# ============================================================
# MediaMTX + curl para poder ejecutar los hooks runOnRead/runOnUnread.
# La imagen oficial es distroless (sin shell), así que se copia el
# binario a una base Alpine.
# ============================================================
FROM alpine:3.20

# gettext aporta envsubst: MediaMTX no expande ${VAR} en su YAML, así que el
# entrypoint genera /mediamtx.yml a partir de la plantilla montada.
RUN apk add --no-cache curl ca-certificates gettext

COPY --from=bluenviron/mediamtx:1.21.4 /mediamtx /usr/local/bin/mediamtx
COPY mediamtx.yml /mediamtx.yml
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

EXPOSE 8889/tcp
EXPOSE 8189/udp
EXPOSE 9997/tcp

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
CMD ["/usr/local/bin/mediamtx"]
