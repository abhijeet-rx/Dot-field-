-- ──────────────────────────────────────────────────────────────
-- Flyway Migration V7: Add India Location Normalization & Relevance Fields
-- ──────────────────────────────────────────────────────────────

ALTER TABLE jobs
    ADD COLUMN IF NOT EXISTS normalized_country VARCHAR(10),
    ADD COLUMN IF NOT EXISTS normalized_city VARCHAR(100),
    ADD COLUMN IF NOT EXISTS remote_country VARCHAR(10),
    ADD COLUMN IF NOT EXISTS is_india_relevant BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_jobs_india_relevant ON jobs(is_india_relevant);
CREATE INDEX IF NOT EXISTS idx_jobs_source ON jobs(source);
