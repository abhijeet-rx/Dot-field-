package com.dotfield.discovery;

import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.JobDiscoveryResponse;
import com.dotfield.dto.JobIngestionRunResponse;
import com.dotfield.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobDiscoveryConcurrencyTest {

    @Mock
    private JobSourceRegistry sourceRegistry;
    @Mock
    private com.dotfield.extractor.JobExtractionPipeline extractionPipeline;
    @Mock
    private JobDeduplicationService deduplicationService;
    @Mock
    private JobDiscoveryPersistenceHelper persistenceHelper;
    @Mock
    private com.dotfield.repository.JobRepository jobRepository;
    @Mock
    private JobIngestionMonitor ingestionMonitor;
    @Mock
    private JobSource mockJobSource;

    private JobDiscoveryService discoveryService;
    private JobDiscoveryScheduler scheduler;

    @BeforeEach
    void setUp() {
        lenient().when(sourceRegistry.getAllSources()).thenReturn(List.of(mockJobSource));
        lenient().when(sourceRegistry.getRequiredSource(any())).thenReturn(mockJobSource);
        lenient().when(mockJobSource.getSourceName()).thenReturn("REMOTIVE");
        lenient().when(mockJobSource.discover(any())).thenReturn(Collections.emptyList());

        discoveryService = new JobDiscoveryService(
                sourceRegistry,
                extractionPipeline,
                deduplicationService,
                persistenceHelper,
                jobRepository,
                ingestionMonitor
        );

        scheduler = new JobDiscoveryScheduler(discoveryService);
    }

    @Test
    @DisplayName("Scenario A — Manual ingestion starts, Scheduled ingestion attempts -> Only manual executes, scheduled skips")
    void scenarioA_manualIngestionActive_scheduledIngestionSkips() throws Exception {
        CountDownLatch manualStarted = new CountDownLatch(1);
        CountDownLatch allowManualToFinish = new CountDownLatch(1);

        when(mockJobSource.discover(any())).thenAnswer(invocation -> {
            manualStarted.countDown();
            allowManualToFinish.await(5, TimeUnit.SECONDS);
            return Collections.emptyList();
        });

        AtomicReference<JobIngestionRunResponse> manualResult = new AtomicReference<>();
        Thread manualThread = new Thread(() -> {
            manualResult.set(discoveryService.runManualIngestion(JobDiscoveryRequest.builder().source("ALL").build()));
        });
        manualThread.start();

        assertTrue(manualStarted.await(2, TimeUnit.SECONDS), "Manual ingestion should start");

        // Attempt scheduled ingestion while manual run is active
        JobDiscoveryResponse scheduledResponse = scheduler.runScheduledIngestion();
        assertNull(scheduledResponse, "Scheduled ingestion must skip and return null when manual ingestion is active");

        allowManualToFinish.countDown();
        manualThread.join(2000);

        assertNotNull(manualResult.get(), "Manual ingestion should complete successfully");
        assertFalse(discoveryService.isIngestionRunning(), "Lock must be released after completion");
    }

    @Test
    @DisplayName("Scenario B — Scheduled ingestion starts, Manual ingestion attempts -> Only scheduled executes, manual receives ConflictException")
    void scenarioB_scheduledIngestionActive_manualIngestionThrowsConflict() throws Exception {
        CountDownLatch scheduledStarted = new CountDownLatch(1);
        CountDownLatch allowScheduledToFinish = new CountDownLatch(1);

        when(mockJobSource.discover(any())).thenAnswer(invocation -> {
            scheduledStarted.countDown();
            allowScheduledToFinish.await(5, TimeUnit.SECONDS);
            return Collections.emptyList();
        });

        AtomicReference<JobDiscoveryResponse> scheduledResult = new AtomicReference<>();
        Thread scheduledThread = new Thread(() -> {
            scheduledResult.set(scheduler.runScheduledIngestion());
        });
        scheduledThread.start();

        assertTrue(scheduledStarted.await(2, TimeUnit.SECONDS), "Scheduled ingestion should start");

        // Attempt manual ingestion while scheduled run is active
        assertThrows(ConflictException.class, () -> {
            discoveryService.runManualIngestion(JobDiscoveryRequest.builder().source("ALL").build());
        }, "Manual ingestion must throw ConflictException (409) when scheduled ingestion is active");

        allowScheduledToFinish.countDown();
        scheduledThread.join(2000);

        assertNotNull(scheduledResult.get(), "Scheduled ingestion should complete successfully");
        assertFalse(discoveryService.isIngestionRunning(), "Lock must be released after completion");
    }

    @Test
    @DisplayName("Scenario C — Ingestion throws exception -> lock is released in finally block and subsequent run succeeds")
    void scenarioC_ingestionThrowsException_lockIsReleased() {
        when(sourceRegistry.getRequiredSource(any())).thenThrow(new RuntimeException("Registry lookup failure"));

        assertThrows(RuntimeException.class, () -> {
            discoveryService.discoverJobs(JobDiscoveryRequest.builder().source("REMOTIVE").build());
        });

        assertFalse(discoveryService.isIngestionRunning(), "Lock MUST be released in finally block after exception");

        // Reset mock behavior and verify next ingestion run can execute cleanly
        doReturn(mockJobSource).when(sourceRegistry).getRequiredSource(any());
        when(mockJobSource.discover(any())).thenReturn(Collections.emptyList());

        JobDiscoveryResponse retryResponse = discoveryService.discoverJobs(JobDiscoveryRequest.builder().source("REMOTIVE").build());
        assertNotNull(retryResponse, "Subsequent ingestion must succeed after previous failure recovery");
        assertFalse(discoveryService.isIngestionRunning());
    }

    @Test
    @DisplayName("Scenario D — Ingestion completes normally -> lock is released and subsequent run succeeds")
    void scenarioD_ingestionCompletesNormally_lockIsReleased() {
        when(mockJobSource.discover(any())).thenReturn(Collections.emptyList());

        JobDiscoveryResponse response1 = discoveryService.discoverJobs(JobDiscoveryRequest.builder().source("ALL").build());
        assertNotNull(response1);
        assertFalse(discoveryService.isIngestionRunning(), "Lock must be false after normal completion");

        JobDiscoveryResponse response2 = discoveryService.discoverJobs(JobDiscoveryRequest.builder().source("ALL").build());
        assertNotNull(response2);
        assertFalse(discoveryService.isIngestionRunning(), "Lock must be false after second normal completion");
    }
}
