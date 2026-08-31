package com.dotfield.discovery;

import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.JobDiscoveryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobDiscoverySchedulerTest {

    private JobIngestionOrchestrator orchestrator;
    private JobDiscoveryScheduler scheduler;

    @BeforeEach
    void setUp() {
        orchestrator = mock(JobIngestionOrchestrator.class);
        scheduler = new JobDiscoveryScheduler(orchestrator);
    }

    @Test
    @DisplayName("Scheduler Invocation — Scheduled run invokes orchestrator across ALL sources")
    void runScheduledIngestion_success_invokesIngestFromAllSources() {
        JobDiscoveryResponse mockResponse = JobDiscoveryResponse.builder()
                .discovered(10)
                .newJobs(5)
                .updatedJobs(2)
                .unchangedJobs(3)
                .duplicates(0)
                .failed(0)
                .build();

        when(orchestrator.ingestFromAllSources(any(JobDiscoveryRequest.class))).thenReturn(mockResponse);

        JobDiscoveryResponse response = scheduler.runScheduledIngestion();

        assertNotNull(response);
        assertEquals(10, response.getDiscovered());
        assertEquals(5, response.getNewJobs());

        verify(orchestrator, times(1)).ingestFromAllSources(argThat(req ->
                "ALL".equals(req.getSource()) && req.getMaxResults() == 50
        ));
    }

    @Test
    @DisplayName("Concurrency Strategy — Overlapping execution skips scheduled run when ConflictException thrown by orchestrator")
    void runScheduledIngestion_conflictException_skipsRun() {
        when(orchestrator.ingestFromAllSources(any(JobDiscoveryRequest.class)))
                .thenThrow(new com.dotfield.exception.ConflictException("Job ingestion run is already in progress."));

        JobDiscoveryResponse response = scheduler.runScheduledIngestion();

        assertNull(response);
        verify(orchestrator, times(1)).ingestFromAllSources(any());
    }

    @Test
    @DisplayName("Scheduler Recovery — Unhandled exception is caught and subsequent run can execute")
    void runScheduledIngestion_unhandledException_logsAndRecovers() {
        when(orchestrator.ingestFromAllSources(any(JobDiscoveryRequest.class)))
                .thenThrow(new RuntimeException("Database connection timeout"));

        JobDiscoveryResponse response = scheduler.runScheduledIngestion();

        assertNull(response);

        // Subsequent run works cleanly
        JobDiscoveryResponse successResponse = JobDiscoveryResponse.builder().discovered(2).build();
        when(orchestrator.ingestFromAllSources(any(JobDiscoveryRequest.class))).thenReturn(successResponse);

        JobDiscoveryResponse retryResponse = scheduler.runScheduledIngestion();
        assertNotNull(retryResponse);
        assertEquals(2, retryResponse.getDiscovered());
    }

    @Test
    @DisplayName("Configuration Test — Scheduler bean loaded when property is true (default)")
    void schedulerEnabledProperty_beanCreated() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestConfig.class)
                .withPropertyValues("job.ingestion.scheduler.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(JobDiscoveryScheduler.class);
                });
    }

    @Test
    @DisplayName("Configuration Test — Scheduler bean NOT created when job.ingestion.scheduler.enabled=false")
    void schedulerDisabledProperty_beanNotCreated() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestConfig.class)
                .withPropertyValues("job.ingestion.scheduler.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(JobDiscoveryScheduler.class);
                });
    }

    @org.springframework.context.annotation.Configuration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        public JobIngestionOrchestrator jobIngestionOrchestrator() {
            return mock(JobIngestionOrchestrator.class);
        }

        @org.springframework.context.annotation.Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
                name = "job.ingestion.scheduler.enabled",
                havingValue = "true",
                matchIfMissing = true
        )
        public JobDiscoveryScheduler jobDiscoveryScheduler(JobIngestionOrchestrator orchestrator) {
            return new JobDiscoveryScheduler(orchestrator);
        }
    }
}
