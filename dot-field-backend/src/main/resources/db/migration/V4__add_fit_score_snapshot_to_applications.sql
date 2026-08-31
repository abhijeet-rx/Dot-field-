-- ──────────────────────────────────────────────────────────────
-- DOT Field — Flyway Migration V4: Persist Fit Score Snapshot
-- ──────────────────────────────────────────────────────────────

ALTER TABLE applications ADD COLUMN fit_score INT;
ALTER TABLE applications ADD COLUMN match_category VARCHAR(50);
