-- ============================================================
-- Migracion V4 del esquema heypudu_posts
-- Permite volver a reportar una publicacion cuando el reporte
-- anterior fue descartado, conservando el historial completo.
-- ============================================================

ALTER TABLE post_reports
    DROP CONSTRAINT IF EXISTS uq_post_reports_post_reporter;

CREATE UNIQUE INDEX IF NOT EXISTS uq_post_reports_open
    ON post_reports (post_id, reporter_id)
    WHERE status = 'OPEN';

ALTER TABLE post_reports
    ADD COLUMN IF NOT EXISTS resolved_by UUID;

CREATE INDEX IF NOT EXISTS idx_post_reports_reporter
    ON post_reports (reporter_id);
