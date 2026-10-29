-- ============================================================
-- Migracion V4 del esquema heypudu_interactions
-- Permite volver a reportar un comentario cuando el reporte
-- anterior fue descartado, conservando el historial completo.
-- ============================================================

ALTER TABLE comment_reports
    DROP CONSTRAINT IF EXISTS uq_comment_reports_comment_reporter;

CREATE UNIQUE INDEX IF NOT EXISTS uq_comment_reports_open
    ON comment_reports (comment_id, reporter_id)
    WHERE status = 'OPEN';

ALTER TABLE comment_reports
    ADD COLUMN IF NOT EXISTS resolved_by UUID;

CREATE INDEX IF NOT EXISTS idx_comment_reports_reporter
    ON comment_reports (reporter_id);
