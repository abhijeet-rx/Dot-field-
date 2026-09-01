package com.dotfield.discovery;

import com.dotfield.dto.*;
import com.dotfield.entity.Job;
import com.dotfield.entity.JobStatus;
import com.dotfield.extractor.ExtractedJob;
import com.dotfield.extractor.JobExtractionPipeline;
import com.dotfield.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobSourceErrorIsolationTest {

    private JobDiscoveryService discoveryService;
    private JobSource sourceA;
    private JobSource sourceB;
    private JobSourceRegistry registry;
    private JobExtractionPipeline extractionPipeline;
    private JobDeduplicationService deduplicationService;
    private JobDiscoveryPersistenceHelper persistenceHelper;
    private JobRepository jobRepository;

    @BeforeEach
    void setUp() {
        // Source A fails with an exception
        sourceA = new JobSource() {
            @Override
            public String getSourceName() {
                return "SOURCE_A_FAILING";
            }

            @Override
            public boolean supports(String source) {
                return "SOURCE_A_FAILING".equalsIgnoreCase(source);
            }

            @Override
            public List<RawJobListing> discover(JobDiscoveryRequest request) {
                throw new RuntimeException("External API network connection timeout");
            }
        };

        // Source B succeeds
        sourceB = new JobSource() {
            @Override
            public String getSourceName() {
                return "SOURCE_B_SUCCESSFUL";
            }

            @Override
            public boolean supports(String source) {
                return "SOURCE_B_SUCCESSFUL".equalsIgnoreCase(source);
            }

            @Override
            public List<RawJobListing> discover(JobDiscoveryRequest request) {
                return List.of(
                        RawJobListing.builder()
                                .externalId("SRC-B-101")
                                .title("Python Engineer")
                                .company("Tech Corp")
                                .location("Remote - India")
                                .source("SOURCE_B_SUCCESSFUL")
                                .build()
                );
            }
        };

        registry = new JobSourceRegistry(List.of(sourceA, sourceB));

        extractionPipeline = mock(JobExtractionPipeline.class);
        deduplicationService = mock(JobDeduplicationService.class);
        persistenceHelper = mock(JobDiscoveryPersistenceHelper.class);
        jobRepository = mock(JobRepository.class);

        discoveryService = new JobDiscoveryService(
                registry,
                extractionPipeline,
                deduplicationService,
                persistenceHelper,
                jobRepository,
                new JobIngestionMonitor()
        );
    }

    @Test
    @DisplayName("Error Isolation — Failure in Source A does NOT fail Source B or the overall pipeline")
    void discoverFromAllSources_sourceFailureIsIsolated() {
        ExtractedJob extractedB = ExtractedJob.builder()
                .title("Python Engineer")
                .company("Tech Corp")
                .location("Bengaluru, India")
                .isIndiaRelevant(true)
                .jobUrl("https://techcorp.com/jobs/101")
                .source("SOURCE_B_SUCCESSFUL")
                .build();

        when(extractionPipeline.extractAndNormalize(any(), eq("SOURCE_B_SUCCESSFUL"))).thenReturn(extractedB);
        when(deduplicationService.canonicalizeUrl(any())).thenReturn("https://techcorp.com/jobs/101");
        when(deduplicationService.generateFingerprint(any(), any(), any(), any())).thenReturn("fingerprint-b");
        when(deduplicationService.findExistingJob(any(), any(), any(), any(), any(), any(), any())).thenReturn(java.util.Optional.empty());
        when(persistenceHelper.saveNewJob(any(Job.class))).thenReturn(Job.builder().id(1L).status(JobStatus.SAVED).build());

        JobDiscoveryRequest request = JobDiscoveryRequest.builder().source("ALL").build();
        JobDiscoveryResponse response = discoveryService.discoverFromAllSources(request);

        assertNotNull(response);
        assertEquals(1, response.getDiscovered());
        assertEquals(1, response.getNewJobs());
        assertEquals(1, response.getFailed()); // Source A failed

        List<SourceDiscoveryResult> results = response.getSourceResults();
        assertEquals(2, results.size());

        // Verify Source A result: FAILED
        SourceDiscoveryResult resultA = results.stream()
                .filter(r -> "SOURCE_A_FAILING".equals(r.getSource()))
                .findFirst()
                .orElseThrow();
        assertEquals("FAILED", resultA.getStatus());
        assertEquals("External API network connection timeout", resultA.getErrorMessage());
        assertEquals(1, resultA.getFailed());
        assertEquals(0, resultA.getDiscovered());

        // Verify Source B result: SUCCESS
        SourceDiscoveryResult resultB = results.stream()
                .filter(r -> "SOURCE_B_SUCCESSFUL".equals(r.getSource()))
                .findFirst()
                .orElseThrow();
        assertEquals("SUCCESS", resultB.getStatus());
        assertNull(resultB.getErrorMessage());
        assertEquals(1, resultB.getDiscovered());
        assertEquals(1, resultB.getNewJobs());
        assertEquals(0, resultB.getFailed());
    }

    @Test
    @DisplayName("Error Isolation — Single source failure returned cleanly without unhandled exception")
    void discoverJobs_singleSourceFailure_returnsFailedResultCleanly() {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder().source("SOURCE_A_FAILING").build();
        JobDiscoveryResponse response = discoveryService.discoverJobs(request);

        assertNotNull(response);
        assertEquals(0, response.getDiscovered());
        assertEquals(1, response.getFailed());
        assertEquals(1, response.getSourceResults().size());

        SourceDiscoveryResult result = response.getSourceResults().get(0);
        assertEquals("SOURCE_A_FAILING", result.getSource());
        assertEquals("FAILED", result.getStatus());
        assertEquals("External API network connection timeout", result.getErrorMessage());
    }
}
