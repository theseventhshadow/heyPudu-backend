-- ============================================================
-- Migracion V3 del esquema heypudu_posts
-- Registra los reportes que los usuarios hacen sobre publicaciones.
-- ============================================================

CREATE TABLE post_reports (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id      UUID         NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    reporter_id  UUID         NOT NULL,
    reason       VARCHAR(40)  NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resolved_at  TIMESTAMPTZ,
    CONSTRAINT uq_post_reports_post_reporter UNIQUE (post_id, reporter_id),
    CONSTRAINT ck_post_reports_reason CHECK (
        reason IN ('SPAM', 'HARASSMENT', 'HATE_SPEECH', 'INAPPROPRIATE_CONTENT', 'OTHER')
    ),
    CONSTRAINT ck_post_reports_status CHECK (
        status IN ('OPEN', 'RESOLVED', 'DISMISSED')
    )
);

CREATE INDEX idx_post_reports_status_created
    ON post_reports (status, created_at);
