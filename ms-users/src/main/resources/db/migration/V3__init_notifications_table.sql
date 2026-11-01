CREATE TABLE notifications (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id     UUID         NOT NULL UNIQUE,
    recipient_id UUID         NOT NULL,
    actor_id     UUID         NOT NULL,
    type         VARCHAR(40)  NOT NULL,
    post_id      UUID,
    comment_id   UUID,
    message      VARCHAR(255) NOT NULL,
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_recipient_created
    ON notifications (recipient_id, created_at DESC);

CREATE INDEX idx_notifications_recipient_unread
    ON notifications (recipient_id, is_read, created_at DESC);
