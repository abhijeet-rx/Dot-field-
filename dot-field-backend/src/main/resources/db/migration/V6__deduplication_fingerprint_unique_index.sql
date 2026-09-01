-- ──────────────────────────────────────────────────────────────
-- DOT Field — Flyway Migration V6: Concurrency-Safe Deduplication Index
-- Archives duplicate fingerprint records before creating unique index.
-- ──────────────────────────────────────────────────────────────

-- 1. Create jobs_backup table structure
CREATE TABLE IF NOT EXISTS jobs_backup (
    id                         BIGINT,
    external_id                VARCHAR(200),
    title                      VARCHAR(200),
    company                    VARCHAR(200),
    location                   VARCHAR(200),
    description                TEXT,
    job_url                    VARCHAR(2048),
    canonical_url              VARCHAR(2048),
    deduplication_fingerprint  VARCHAR(64),
    source                     VARCHAR(100),
    employment_type            VARCHAR(255),
    remote_type                VARCHAR(255),
    status                     VARCHAR(255),
    salary_min                 DECIMAL(15, 2),
    salary_max                 DECIMAL(15, 2),
    currency                   VARCHAR(10),
    posted_date                DATE,
    last_discovered_at         TIMESTAMP,
    first_seen_at              TIMESTAMP,
    last_seen_at               TIMESTAMP,
    created_at                 TIMESTAMP,
    updated_at                 TIMESTAMP,
    archived_at                TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Archive duplicate rows (retaining lowest ID in jobs table)
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

-- 3. Delete archived duplicate rows from jobs
DELETE FROM jobs
WHERE id IN (SELECT id FROM jobs_backup);

-- 4. Create partial unique index on deduplication_fingerprint
CREATE UNIQUE INDEX IF NOT EXISTS ux_job_deduplication_fingerprint
ON jobs (deduplication_fingerprint)
WHERE deduplication_fingerprint IS NOT NULL;
