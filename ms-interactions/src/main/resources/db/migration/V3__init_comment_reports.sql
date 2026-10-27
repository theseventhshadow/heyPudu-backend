CREATE TABLE comment_reports (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comment_id   UUID NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    reporter_id  UUID NOT NULL,
    reason       VARCHAR(40) NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at  TIMESTAMPTZ,
    CONSTRAINT uq_comment_reports_comment_reporter UNIQUE (comment_id, reporter_id),
    CONSTRAINT ck_comment_reports_reason CHECK (
        reason IN ('SPAM', 'HARASSMENT', 'HATE_SPEECH', 'INAPPROPRIATE_CONTENT', 'OTHER')
    ),
    CONSTRAINT ck_comment_reports_status CHECK (
        status IN ('OPEN', 'RESOLVED', 'DISMISSED')
    )
);

CREATE INDEX idx_comment_reports_status_created
    ON comment_reports (status, created_at);
