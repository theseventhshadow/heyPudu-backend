# ============================================================
# MediaMTX + curl para poder ejecutar los hooks runOnRead/runOnUnread.
# La imagen oficial es distroless (sin shell), así que se copia el
# binario a una base Alpine.
# ============================================================
FROM alpine:3.20

RUN apk add --no-cache curl ca-certificates

COPY --from=bluenviron/mediamtx:1.21.4 /mediamtx /usr/local/bin/mediamtx
COPY mediamtx.yml /mediamtx.yml

EXPOSE 8889/tcp
EXPOSE 8189/udp
EXPOSE 9997/tcp

ENTRYPOINT ["/usr/local/bin/mediamtx"]
