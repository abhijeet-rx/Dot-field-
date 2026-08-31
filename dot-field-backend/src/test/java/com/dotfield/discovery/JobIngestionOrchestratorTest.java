package com.dotfield.discovery;

import com.dotfield.dto.*;
import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.Job;
import com.dotfield.entity.JobStatus;
import com.dotfield.entity.RemoteType;
import com.dotfield.extractor.ExtractedJob;
import com.dotfield.extractor.JobExtractionPipeline;
import com.dotfield.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobIngestionOrchestratorTest {

    private JobDiscoveryService orchestrator;
    private JobSource sourceRemotive;
    private JobSource sourceFailing;
    private JobSourceRegistry registry;
    private JobExtractionPipeline extractionPipeline;
    private JobDeduplicationService deduplicationService;
    private JobDiscoveryPersistenceHelper persistenceHelper;
    private JobRepository jobRepository;

    @BeforeEach
    void setUp() {
        // Remotive Source producing 2 raw jobs
        sourceRemotive = new JobSource() {
            @Override
            public String getSourceName() {
                return "REMOTIVE";
            }

            @Override
            public boolean supports(String source) {
                return "REMOTIVE".equalsIgnoreCase(source);
            }

            @Override
            public List<RawJobListing> discover(JobDiscoveryRequest request) {
                return List.of(
                        RawJobListing.builder()
                                .externalId("REM-101")
                                .title("Senior Java Developer")
                                .company("Acme Corp")
                                .location("Remote")
                                .jobUrl("https://remotive.com/jobs/101")
                                .source("REMOTIVE")
                                .employmentType(EmploymentType.FULL_TIME)
                                .remoteType(RemoteType.REMOTE)
                                .postedDate(LocalDate.now())
                                .build(),
                        RawJobListing.builder()
                                .externalId("REM-102")
                                .title("DevOps Engineer")
                                .company("Cloud Ops")
                                .location("Remote")
                                .jobUrl("https://remotive.com/jobs/102")
                                .source("REMOTIVE")
                                .employmentType(EmploymentType.CONTRACT)
                                .remoteType(RemoteType.REMOTE)
                                .postedDate(LocalDate.now())
                                .build()
                );
            }
        };

        // Failing source
        sourceFailing = new JobSource() {
            @Override
            public String getSourceName() {
                return "FAILING_SOURCE";
            }

            @Override
            public boolean supports(String source) {
                return "FAILING_SOURCE".equalsIgnoreCase(source);
            }

            @Override
            public List<RawJobListing> discover(JobDiscoveryRequest request) {
                throw new RuntimeException("HTTP 503 Service Unavailable");
            }
        };

        registry = new JobSourceRegistry(List.of(sourceRemotive, sourceFailing));
        extractionPipeline = mock(JobExtractionPipeline.class);
        deduplicationService = mock(JobDeduplicationService.class);
        persistenceHelper = mock(JobDiscoveryPersistenceHelper.class);
        jobRepository = mock(JobRepository.class);

        orchestrator = new JobDiscoveryService(
                registry,
                extractionPipeline,
                deduplicationService,
                persistenceHelper,
                jobRepository,
                new JobIngestionMonitor()
        );
    }

    @Test
    @DisplayName("End-to-End Ingestion Flow — Fetch -> Transform -> Normalize -> Deduplicate -> Persist (New Jobs)")
    void ingestFromSource_newJobs_persistedSuccessfully() {
        ExtractedJob extracted1 = ExtractedJob.builder()
                .title("Senior Java Developer")
                .company("Acme Corp")
                .location("Remote")
                .jobUrl("https://remotive.com/jobs/101")
                .source("REMOTIVE")
                .employmentType(EmploymentType.FULL_TIME)
                .remoteType(RemoteType.REMOTE)
                .build();

        ExtractedJob extracted2 = ExtractedJob.builder()
                .title("DevOps Engineer")
                .company("Cloud Ops")
                .location("Remote")
                .jobUrl("https://remotive.com/jobs/102")
                .source("REMOTIVE")
                .employmentType(EmploymentType.CONTRACT)
                .remoteType(RemoteType.REMOTE)
                .build();

        when(extractionPipeline.extractAndNormalize(any(), eq("REMOTIVE")))
                .thenReturn(extracted1)
                .thenReturn(extracted2);

        when(deduplicationService.canonicalizeUrl(any())).thenAnswer(i -> i.getArgument(0));
        when(deduplicationService.generateFingerprint(any(), any(), any(), any())).thenAnswer(i -> "fp-" + i.getArgument(1));
        when(deduplicationService.findExistingJob(any(), any(), any(), any(), any(), any(), any())).thenReturn(Optional.empty());

        when(persistenceHelper.saveNewJob(any(Job.class))).thenAnswer(i -> {
            Job j = i.getArgument(0);
            j.setId(99L);
            return j;
        });

        JobDiscoveryRequest request = JobDiscoveryRequest.builder().source("REMOTIVE").build();
        JobDiscoveryResponse response = orchestrator.ingestFromSource(request);

        assertNotNull(response);
        assertEquals(2, response.getDiscovered());
        assertEquals(2, response.getNewJobs());
        assertEquals(0, response.getUpdatedJobs());
        assertEquals(0, response.getUnchangedJobs());
        assertEquals(0, response.getFailed());

        // Verify saveNewJob called twice in separate REQUIRES_NEW transactions
        verify(persistenceHelper, times(2)).saveNewJob(any(Job.class));
        verify(persistenceHelper, never()).updateExistingJob(any(Job.class));
    }

    @Test
    @DisplayName("Idempotence Strategy — Second ingestion pass of identical data produces zero new jobs (0 new, 2 unchanged)")
    void ingestFromSource_idempotentSecondPass_producesZeroNewJobs() {
        ExtractedJob extracted1 = ExtractedJob.builder()
                .title("Senior Java Developer")
                .company("Acme Corp")
                .location("Remote")
                .jobUrl("https://remotive.com/jobs/101")
                .source("REMOTIVE")
                .build();

        ExtractedJob extracted2 = ExtractedJob.builder()
                .title("DevOps Engineer")
                .company("Cloud Ops")
                .location("Remote")
                .jobUrl("https://remotive.com/jobs/102")
                .source("REMOTIVE")
                .build();

        Job existingJob1 = Job.builder()
                .id(101L)
                .externalId("REM-101")
                .title("Senior Java Developer")
                .company("Acme Corp")
                .location("Remote")
                .jobUrl("https://remotive.com/jobs/101")
                .canonicalUrl("https://remotive.com/jobs/101")
                .deduplicationFingerprint("fp-Senior Java Developer")
                .source("REMOTIVE")
                .status(JobStatus.SAVED)
                .build();

        Job existingJob2 = Job.builder()
                .id(102L)
                .externalId("REM-102")
                .title("DevOps Engineer")
                .company("Cloud Ops")
                .location("Remote")
                .jobUrl("https://remotive.com/jobs/102")
                .canonicalUrl("https://remotive.com/jobs/102")
                .deduplicationFingerprint("fp-DevOps Engineer")
                .source("REMOTIVE")
                .status(JobStatus.SAVED)
                .build();

        when(extractionPipeline.extractAndNormalize(any(), eq("REMOTIVE")))
                .thenReturn(extracted1)
                .thenReturn(extracted2);

        when(deduplicationService.canonicalizeUrl(any())).thenAnswer(i -> i.getArgument(0));
        when(deduplicationService.generateFingerprint(any(), any(), any(), any())).thenAnswer(i -> "fp-" + i.getArgument(1));

        when(deduplicationService.findExistingJob(any(), eq("REM-101"), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(existingJob1));
        when(deduplicationService.findExistingJob(any(), eq("REM-102"), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(existingJob2));

        JobDiscoveryRequest request = JobDiscoveryRequest.builder().source("REMOTIVE").build();
        JobDiscoveryResponse response = orchestrator.ingestFromSource(request);

        assertNotNull(response);
        assertEquals(2, response.getDiscovered());
        assertEquals(0, response.getNewJobs());
        assertEquals(0, response.getUpdatedJobs());
        assertEquals(2, response.getUnchangedJobs());

        verify(persistenceHelper, never()).saveNewJob(any(Job.class));
        verify(persistenceHelper, times(2)).updateExistingJob(any(Job.class));
    }

    @Test
    @DisplayName("Multi-Source Error Isolation — Partial source failure (Source A fails, Source B succeeds)")
    void ingestFromAllSources_partialSourceFailure_isolatedPerSource() {
        ExtractedJob extracted = ExtractedJob.builder()
                .title("Senior Java Developer")
                .company("Acme Corp")
                .source("REMOTIVE")
                .build();

        when(extractionPipeline.extractAndNormalize(any(), eq("REMOTIVE"))).thenReturn(extracted);
        when(deduplicationService.canonicalizeUrl(any())).thenReturn("url");
        when(deduplicationService.generateFingerprint(any(), any(), any(), any())).thenReturn("fp");
        when(deduplicationService.findExistingJob(any(), any(), any(), any(), any(), any(), any())).thenReturn(Optional.empty());
        when(persistenceHelper.saveNewJob(any(Job.class))).thenReturn(Job.builder().id(1L).build());

        JobDiscoveryRequest request = JobDiscoveryRequest.builder().source("ALL").build();
        JobDiscoveryResponse response = orchestrator.ingestFromAllSources(request);

        assertNotNull(response);
        assertEquals(2, response.getDiscovered());
        assertEquals(2, response.getNewJobs());
        assertEquals(1, response.getFailed()); // FAILING_SOURCE failed

        assertEquals(2, response.getSourceResults().size());

        SourceDiscoveryResult remotiveResult = response.getSourceResults().stream()
                .filter(r -> "REMOTIVE".equals(r.getSource()))
                .findFirst().orElseThrow();
        assertEquals("SUCCESS", remotiveResult.getStatus());
        assertEquals(2, remotiveResult.getDiscovered());

        SourceDiscoveryResult failingResult = response.getSourceResults().stream()
                .filter(r -> "FAILING_SOURCE".equals(r.getSource()))
                .findFirst().orElseThrow();
        assertEquals("FAILED", failingResult.getStatus());
        assertEquals("HTTP 503 Service Unavailable", failingResult.getErrorMessage());
    }
}
