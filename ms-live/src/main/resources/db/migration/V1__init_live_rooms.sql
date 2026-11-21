CREATE TABLE live_rooms (
    id                  UUID PRIMARY KEY,
    creator_id          UUID NOT NULL,
    creator_cognito_sub UUID NOT NULL,
    title               VARCHAR(120) NOT NULL,
    description         VARCHAR(500),
    status              VARCHAR(20) NOT NULL DEFAULT 'LIVE',
    started_at          TIMESTAMPTZ NOT NULL,
    ended_at            TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_live_rooms_status CHECK (status IN ('LIVE','CLOSED'))
);

CREATE INDEX idx_live_rooms_status ON live_rooms (status, started_at DESC);
CREATE INDEX idx_live_rooms_creator ON live_rooms (creator_cognito_sub);
CREATE UNIQUE INDEX uq_live_room_creator_live
    ON live_rooms (creator_cognito_sub)
    WHERE status = 'LIVE';
