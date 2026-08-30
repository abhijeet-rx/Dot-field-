package com.dotfield.discovery;

import com.dotfield.entity.Job;
import com.dotfield.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
}
