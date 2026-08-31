-- ──────────────────────────────────────────────────────────────
-- DOT Field — Flyway Migration V5: Job Freshness Tracking
-- Adds first_seen_at and last_seen_at timestamp columns to jobs
-- ──────────────────────────────────────────────────────────────

ALTER TABLE jobs ADD COLUMN first_seen_at TIMESTAMP;
ALTER TABLE jobs ADD COLUMN last_seen_at TIMESTAMP;

UPDATE jobs SET first_seen_at = created_at WHERE first_seen_at IS NULL;
UPDATE jobs SET last_seen_at = last_discovered_at WHERE last_seen_at IS NULL;
UPDATE jobs SET last_seen_at = created_at WHERE last_seen_at IS NULL;
