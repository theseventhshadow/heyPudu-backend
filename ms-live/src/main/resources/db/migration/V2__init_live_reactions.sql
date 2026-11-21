CREATE TABLE live_reactions (
    id                UUID PRIMARY KEY,
    room_id           UUID NOT NULL,
    actor_cognito_sub UUID NOT NULL,
    reaction_type     VARCHAR(20) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_live_reactions_room
        FOREIGN KEY (room_id) REFERENCES live_rooms (id) ON DELETE CASCADE,
    CONSTRAINT chk_live_reactions_type CHECK (reaction_type IN ('CLAP','HEART'))
);

CREATE INDEX idx_live_reactions_room ON live_reactions (room_id, created_at DESC);
CREATE INDEX idx_live_reactions_actor ON live_reactions (actor_cognito_sub, created_at DESC);
