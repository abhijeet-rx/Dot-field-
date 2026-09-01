-- ──────────────────────────────────────────────────────────────
-- Standalone Detection & Deduplication Script for DOT Field
-- ──────────────────────────────────────────────────────────────

-- 1. Detect duplicate fingerprints
SELECT deduplication_fingerprint, COUNT(*) AS duplicate_count
FROM jobs
WHERE deduplication_fingerprint IS NOT NULL
GROUP BY deduplication_fingerprint
HAVING COUNT(*) > 1;

-- 2. Inspect duplicate rows before removal
SELECT j.*
FROM jobs j
JOIN (
    SELECT deduplication_fingerprint
    FROM jobs
    WHERE deduplication_fingerprint IS NOT NULL
    GROUP BY deduplication_fingerprint
    HAVING COUNT(*) > 1
) dupes ON j.deduplication_fingerprint = dupes.deduplication_fingerprint
ORDER BY j.deduplication_fingerprint, j.created_at ASC;

-- 3. Archive duplicate rows into jobs_backup
INSERT INTO jobs_backup (
    id, external_id, title, company, location, description, job_url, canonical_url,
    deduplication_fingerprint, source, employment_type, remote_type, status,
    salary_min, salary_max, currency, posted_date, last_discovered_at,
    first_seen_at, last_seen_at, created_at, updated_at
)
SELECT j.id, j.external_id, j.title, j.company, j.location, j.description, j.job_url, j.canonical_url,
       j.deduplication_fingerprint, j.source, j.employment_type, j.remote_type, j.status,
       j.salary_min, j.salary_max, j.currency, j.posted_date, j.last_discovered_at,
       j.first_seen_at, j.last_seen_at, j.created_at, j.updated_at
FROM jobs j
JOIN (
    SELECT deduplication_fingerprint, MIN(id) AS keep_id
    FROM jobs
    WHERE deduplication_fingerprint IS NOT NULL
    GROUP BY deduplication_fingerprint
    HAVING COUNT(*) > 1
) dupes ON j.deduplication_fingerprint = dupes.deduplication_fingerprint
WHERE j.id <> dupes.keep_id;

-- 4. Remove duplicate rows from jobs
DELETE FROM jobs
WHERE id IN (SELECT id FROM jobs_backup);
