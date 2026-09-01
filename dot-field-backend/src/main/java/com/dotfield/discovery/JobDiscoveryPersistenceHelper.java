package com.dotfield.discovery;

import com.dotfield.entity.Job;
import com.dotfield.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Handles per-listing persistence in its own transaction ({@code REQUIRES_NEW}).
 * <p>
 * This is necessary because catching {@code DataIntegrityViolationException}
 * inside the same {@code @Transactional} boundary invalidates the Hibernate
 * Session. By using a separate bean with {@code REQUIRES_NEW}, the inner
 * transaction rolls back independently while the outer orchestration loop
 * continues processing remaining listings.
 * <p>
 * <strong>Important:</strong> {@code saveNewJob} does NOT catch
 * {@code DataIntegrityViolationException} internally. The exception must
 * propagate out of the transactional boundary so that the REQUIRES_NEW
 * transaction rolls back cleanly. The caller ({@link JobDiscoveryService})
 * catches it in a non-transactional context and performs a re-fetch.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobDiscoveryPersistenceHelper {

    private final JobRepository jobRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Saves a new Job entity in its own REQUIRES_NEW transaction.
     * <p>
     * If a unique constraint violation occurs (concurrent duplicate insert),
     * the exception propagates — the inner transaction rolls back cleanly,
     * and the caller handles the re-fetch in a non-transactional context.
     *
     * @param job the new Job to persist
     * @return the saved Job
     * @throws org.springframework.dao.DataIntegrityViolationException on unique constraint violation
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Job saveNewJob(Job job) {
        return jobRepository.save(job);
    }

    /**
     * Updates an existing Job entity in its own transaction.
     *
     * @param job the existing Job to update
     * @return the saved Job
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Job updateExistingJob(Job job) {
        return jobRepository.save(job);
    }

    /**
     * Atomically upserts a job or fetches the existing record using native JdbcTemplate SQL when a fingerprint is present.
     * Preserves existing candidate tracking fields (such as status).
     *
     * @param job the job entity to upsert
     * @return managed Job entity
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Job upsertOrGetExisting(Job job) {
        if (job.getDeduplicationFingerprint() == null || job.getDeduplicationFingerprint().isBlank()) {
            return jobRepository.save(job);
        }

        try {
            String sql = """
                INSERT INTO jobs (
                    external_id, title, company, location, description, job_url, canonical_url,
                    deduplication_fingerprint, source, employment_type, remote_type, status,
                    salary_min, salary_max, currency, posted_date, last_discovered_at,
                    first_seen_at, last_seen_at, created_at, updated_at
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                ON CONFLICT (deduplication_fingerprint) WHERE deduplication_fingerprint IS NOT NULL
                DO UPDATE SET
                    last_seen_at = EXCLUDED.last_seen_at,
                    last_discovered_at = EXCLUDED.last_discovered_at,
                    updated_at = CURRENT_TIMESTAMP
                """;

            jdbcTemplate.update(sql,
                    job.getExternalId(),
                    job.getTitle(),
                    job.getCompany(),
                    job.getLocation(),
                    job.getDescription(),
                    job.getJobUrl(),
                    job.getCanonicalUrl(),
                    job.getDeduplicationFingerprint(),
                    job.getSource(),
                    job.getEmploymentType() != null ? job.getEmploymentType().name() : null,
                    job.getRemoteType() != null ? job.getRemoteType().name() : null,
                    job.getStatus() != null ? job.getStatus().name() : "ACTIVE",
                    job.getSalaryMin(),
                    job.getSalaryMax(),
                    job.getCurrency(),
                    job.getPostedDate(),
                    job.getLastDiscoveredAt() != null ? java.sql.Timestamp.valueOf(job.getLastDiscoveredAt()) : java.sql.Timestamp.valueOf(LocalDateTime.now()),
                    job.getFirstSeenAt() != null ? java.sql.Timestamp.valueOf(job.getFirstSeenAt()) : java.sql.Timestamp.valueOf(LocalDateTime.now()),
                    job.getLastSeenAt() != null ? java.sql.Timestamp.valueOf(job.getLastSeenAt()) : java.sql.Timestamp.valueOf(LocalDateTime.now())
            );
        } catch (Exception e) {
            log.debug("Native upsert execution notice (falling back to standard JPA path): {}", e.getMessage());
            return jobRepository.save(job);
        }

        return jobRepository.findByDeduplicationFingerprint(job.getDeduplicationFingerprint())
                .orElseGet(() -> jobRepository.save(job));
    }
}
