package com.dotfield.discovery;

import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.JobDiscoveryResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Background scheduler for executing periodic job ingestion across all registered job sources.
 * <p>
 * Configurable properties:
 * <ul>
 *   <li>{@code job.ingestion.scheduler.enabled} (default: true)</li>
 *   <li>{@code job.ingestion.scheduler.fixed-delay-ms} (default: 1800000 ms / 30 mins)</li>
 *   <li>{@code job.ingestion.scheduler.initial-delay-ms} (default: 60000 ms / 1 min)</li>
 *   <li>{@code job.ingestion.scheduler.max-results} (default: 50)</li>
 * </ul>
 * Concurrency & Thread-Safety: Non-overlapping execution enforced via {@link AtomicBoolean}.
 * Startup Safety: Non-zero initial delay ensures application context and DB migrations complete cleanly first.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "job.ingestion.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class JobDiscoveryScheduler {

    private final JobIngestionOrchestrator orchestrator;

    @Value("${job.ingestion.scheduler.max-results:50}")
    private int maxResults = 50;

    @Scheduled(
            fixedDelayString = "${job.ingestion.scheduler.fixed-delay-ms:1800000}",
            initialDelayString = "${job.ingestion.scheduler.initial-delay-ms:60000}"
    )
    public JobDiscoveryResponse runScheduledIngestion() {
        try {
            log.info("Starting scheduled background job ingestion across all sources...");
            JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                    .source("ALL")
                    .maxResults(maxResults)
                    .build();

            JobDiscoveryResponse response = orchestrator.ingestFromAllSources(request);

            log.info("Scheduled job ingestion completed. Discovered: {}, New: {}, Updated: {}, Unchanged: {}, Duplicates: {}, Failed: {}",
                    response.getDiscovered(),
                    response.getNewJobs(),
                    response.getUpdatedJobs(),
                    response.getUnchangedJobs(),
                    response.getDuplicates(),
                    response.getFailed());

            return response;
        } catch (com.dotfield.exception.ConflictException e) {
            log.warn("Scheduled job ingestion skipped — an ingestion run is already active: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Unhandled exception during scheduled job ingestion execution", e);
            return null;
        }
    }
}
