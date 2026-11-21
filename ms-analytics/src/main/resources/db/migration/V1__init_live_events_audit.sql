-- ============================================================
-- Auditoria de eventos de salas en vivo.
-- Segundo consumer group (live-audit-group) materializa el
-- topico de Kafka para conservar historico y permitir reproceso.
-- ============================================================

CREATE TABLE live_events_audit (
    event_id            UUID PRIMARY KEY,
    event_type          VARCHAR(30) NOT NULL,
    room_id             UUID NOT NULL,
    creator_cognito_sub UUID,
    actor_cognito_sub   UUID,
    reaction_type       VARCHAR(20),
    schema_version      INTEGER NOT NULL DEFAULT 1,
    occurred_at         TIMESTAMPTZ NOT NULL,
    ingested_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_live_events_audit_type
        CHECK (event_type IN ('ROOM_CREATED', 'LISTENER_JOINED', 'LISTENER_LEFT', 'REACTION'))
);

CREATE INDEX idx_live_events_audit_room ON live_events_audit (room_id, occurred_at DESC);
CREATE INDEX idx_live_events_audit_type ON live_events_audit (event_type, occurred_at DESC);
CREATE INDEX idx_live_events_audit_actor ON live_events_audit (actor_cognito_sub, occurred_at DESC);
