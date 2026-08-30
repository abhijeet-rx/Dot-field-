package com.dotfield.discovery;

import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.JobDiscoveryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "job-discovery.scheduler.enabled", havingValue = "true", matchIfMissing = false)
public class JobDiscoveryScheduler {

    private final JobDiscoveryService discoveryService;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Scheduled(cron = "${job-discovery.scheduler.cron:0 0 */6 * * *}")
    public void runScheduledDiscovery() {
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("Scheduled job discovery skipped because a previous execution is still running.");
            return;
        }

        try {
            log.info("Starting scheduled background job discovery...");
            JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                    .source("COMPANY_WEBSITE")
                    .maxResults(50)
                    .build();

            JobDiscoveryResponse response = discoveryService.discoverJobs(request);
            log.info("Scheduled job discovery completed. Discovered: {}, New: {}, Updated: {}, Unchanged: {}",
                    response.getDiscovered(), response.getNewJobs(), response.getUpdatedJobs(), response.getUnchangedJobs());
        } catch (Exception e) {
            log.error("Error during scheduled job discovery execution", e);
        } finally {
            isRunning.set(false);
        }
    }

}
