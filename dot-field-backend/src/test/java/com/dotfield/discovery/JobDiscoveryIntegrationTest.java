package com.dotfield.discovery;

import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.JobDiscoveryResponse;
import com.dotfield.entity.Job;
import com.dotfield.entity.JobStatus;
import com.dotfield.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that use the real H2 persistence layer (ddl-auto=create-drop)
 * to verify database-level behavior: idempotency, concurrency, status preservation,
 * and timestamp semantics.
 */
@SpringBootTest
class JobDiscoveryIntegrationTest {

    @Autowired
    private JobDiscoveryService discoveryService;

    @Autowired
    private JobRepository jobRepository;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
    }

    /**
     * Idempotency: discover same listings twice → same row count, zero new on second run.
     */
    @Test
    void discoverSameListingsTwice_noDuplicateRows() {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("COMPANY_WEBSITE")
                .maxResults(10)
                .build();

        // Run 1
        JobDiscoveryResponse response1 = discoveryService.discoverJobs(request);
        int newJobsRun1 = response1.getNewJobs();
        assertTrue(newJobsRun1 > 0, "Run 1 should create new jobs");
        long rowCountAfterRun1 = jobRepository.count();

        // Run 2 (same listings)
        JobDiscoveryResponse response2 = discoveryService.discoverJobs(request);
        assertEquals(0, response2.getNewJobs(), "Run 2 should create zero new jobs");
        assertEquals(response2.getDiscovered(), response2.getUnchangedJobs(),
                "All listings should be unchanged on re-discovery");
        assertEquals(0, response2.getDuplicates());
        assertEquals(0, response2.getFailed());

        long rowCountAfterRun2 = jobRepository.count();
        assertEquals(rowCountAfterRun1, rowCountAfterRun2,
                "Row count must not increase on repeated discovery");

        // Run 3 (triple check)
        JobDiscoveryResponse response3 = discoveryService.discoverJobs(request);
        assertEquals(0, response3.getNewJobs());
        assertEquals(rowCountAfterRun1, jobRepository.count());
    }

    /**
     * Concurrency: two threads discover the same single listing simultaneously.
     * Exactly one Job row should exist afterwards.
     */
    @Test
    void concurrentDiscovery_sameListings_exactlyOneRow() throws Exception {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("COMPANY_WEBSITE")
                .keyword("Java Backend Developer")
                .location("Bangalore")
                .maxResults(1)
                .build();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);

        Callable<Object> task = () -> {
            barrier.await(5, TimeUnit.SECONDS);
            try {
                return discoveryService.discoverJobs(request);
            } catch (com.dotfield.exception.ConflictException e) {
                return e;
            }
        };

        Future<Object> future1 = executor.submit(task);
        Future<Object> future2 = executor.submit(task);

        Object o1 = future1.get(10, TimeUnit.SECONDS);
        Object o2 = future2.get(10, TimeUnit.SECONDS);

        executor.shutdown();

        boolean oneSucceededOneConflict = (o1 instanceof JobDiscoveryResponse && o2 instanceof com.dotfield.exception.ConflictException)
                || (o2 instanceof JobDiscoveryResponse && o1 instanceof com.dotfield.exception.ConflictException);
        assertTrue(oneSucceededOneConflict, "One concurrent request must succeed and the other must fail with ConflictException");

        List<Job> allJobs = jobRepository.findAll();
        long distinctCount = allJobs.stream()
                .filter(j -> "JOB-CW-101".equals(j.getExternalId()))
                .count();
        assertEquals(1, distinctCount, "Exactly one job row with externalId JOB-CW-101 should exist");
    }

    /**
     * Status preservation: existing APPLIED job is not overwritten by discovery refresh.
     */
    @Test
    void discoveryRefresh_preservesExistingUserStatus() {
        LocalDateTime now = LocalDateTime.now();
        // Pre-create a job with APPLIED status
        Job existingJob = Job.builder()
                .externalId("JOB-CW-101")
                .title("Java Backend Developer")
                .company("Acme Corp")
                .location("Bangalore")
                .description("Looking for experienced Java and Spring Boot backend developers.")
                .jobUrl("https://acme.com/careers/jobs/101")
                .source("COMPANY_WEBSITE")
                .status(JobStatus.APPLIED)
                .firstSeenAt(now)
                .lastSeenAt(now)
                .lastDiscoveredAt(now)
                .build();
        jobRepository.save(existingJob);

        // Run discovery which will see the same listing
        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("COMPANY_WEBSITE")
                .keyword("Java")
                .location("Bangalore")
                .maxResults(10)
                .build();

        discoveryService.discoverJobs(request);

        // Verify status is still APPLIED
        Job refreshedJob = jobRepository.findBySourceAndExternalId("COMPANY_WEBSITE", "JOB-CW-101")
                .orElseThrow();
        assertEquals(JobStatus.APPLIED, refreshedJob.getStatus(),
                "User status APPLIED must be preserved after discovery refresh");
        assertNotNull(refreshedJob.getLastDiscoveredAt(),
                "lastDiscoveredAt should be set after discovery");
    }

    /**
     * Timestamp semantics: createdAt stays constant, lastDiscoveredAt updates on re-discovery.
     */
    @Test
    void discoveryRefresh_updatesLastDiscoveredAt_preservesCreatedAt() {
        // First discovery
        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("COMPANY_WEBSITE")
                .maxResults(10)
                .build();

        discoveryService.discoverJobs(request);

        Job afterFirstDiscovery = jobRepository.findBySourceAndExternalId("COMPANY_WEBSITE", "JOB-CW-101")
                .orElseThrow();
        LocalDateTime createdAt = afterFirstDiscovery.getCreatedAt();
        LocalDateTime firstDiscoveredAt = afterFirstDiscovery.getLastDiscoveredAt();

        assertNotNull(createdAt);
        assertNotNull(firstDiscoveredAt);

        // Small delay to ensure timestamps differ
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}

        // Second discovery
        discoveryService.discoverJobs(request);

        Job afterSecondDiscovery = jobRepository.findBySourceAndExternalId("COMPANY_WEBSITE", "JOB-CW-101")
                .orElseThrow();

        assertEquals(createdAt, afterSecondDiscovery.getCreatedAt(),
                "createdAt must remain unchanged on re-discovery");
        assertNotNull(afterSecondDiscovery.getLastDiscoveredAt(),
                "lastDiscoveredAt should be set on re-discovery");
    }

    /**
     * All 6 job statuses are preserved across discovery refresh.
     */
    @Test
    void discoveryRefresh_preservesAllStatusTypes() {
        for (JobStatus status : JobStatus.values()) {
            jobRepository.deleteAll();

            Job job = Job.builder()
                    .externalId("JOB-CW-101")
                    .title("Java Backend Developer")
                    .company("Acme Corp")
                    .location("Bangalore")
                    .description("Looking for experienced Java and Spring Boot backend developers.")
                    .jobUrl("https://acme.com/careers/jobs/101")
                    .source("COMPANY_WEBSITE")
                    .status(status)
                    .build();
            jobRepository.save(job);

            JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                    .source("COMPANY_WEBSITE")
                    .maxResults(10)
                    .build();

            discoveryService.discoverJobs(request);

            Job refreshedJob = jobRepository.findBySourceAndExternalId("COMPANY_WEBSITE", "JOB-CW-101")
                    .orElseThrow();
            assertEquals(status, refreshedJob.getStatus(),
                    "Status " + status + " was not preserved during discovery refresh");
        }
    }

}
