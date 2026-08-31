package com.dotfield.discovery;

import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.JobDiscoveryResponse;
import com.dotfield.dto.RawJobListing;
import com.dotfield.entity.Job;
import com.dotfield.entity.JobStatus;
import com.dotfield.exception.BadRequestException;
import com.dotfield.extractor.ExtractedJob;
import com.dotfield.extractor.JobExtractionPipeline;
import com.dotfield.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobDiscoveryServiceTest {

    @Mock
    private JobSourceRegistry sourceRegistry;

    @Mock
    private JobExtractionPipeline extractionPipeline;

    @Mock
    private JobDeduplicationService deduplicationService;

    @Mock
    private JobDiscoveryPersistenceHelper persistenceHelper;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobSource jobSource;

    private JobDiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        discoveryService = new JobDiscoveryService(sourceRegistry, extractionPipeline, deduplicationService, persistenceHelper, jobRepository, new JobIngestionMonitor());
    }

    // ──────────────────────────────────────────────
    // New job creation
    // ──────────────────────────────────────────────

    @Test
    void discoverJobs_newJobCreated_defaultStatusActiveAndLastDiscoveredAtSet() {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("COMPANY_WEBSITE")
                .maxResults(10)
                .build();

        RawJobListing rawListing = RawJobListing.builder()
                .externalId("JOB-1")
                .title("Java Developer")
                .company("Acme")
                .location("Bangalore")
                .jobUrl("https://acme.com/jobs/1")
                .build();

        ExtractedJob extractedJob = ExtractedJob.builder()
                .title("Java Developer")
                .company("Acme")
                .location("Bangalore")
                .jobUrl("https://acme.com/jobs/1")
                .source("COMPANY_WEBSITE")
                .build();

        when(sourceRegistry.getRequiredSource("COMPANY_WEBSITE")).thenReturn(jobSource);
        when(jobSource.getSourceName()).thenReturn("COMPANY_WEBSITE");
        when(jobSource.discover(request)).thenReturn(List.of(rawListing));
        when(extractionPipeline.extractAndNormalize(any(), eq("COMPANY_WEBSITE"))).thenReturn(extractedJob);
        when(deduplicationService.canonicalizeUrl(anyString())).thenReturn("https://acme.com/jobs/1");
        when(deduplicationService.generateFingerprint(any(), any(), any(), any())).thenReturn("hash123");
        when(deduplicationService.findExistingJob(any(), any(), any(), any(), any(), any(), any())).thenReturn(Optional.empty());

        Job savedEntity = Job.builder().id(100L).status(JobStatus.ACTIVE).lastDiscoveredAt(LocalDateTime.now()).build();
        when(persistenceHelper.saveNewJob(any(Job.class))).thenReturn(savedEntity);

        JobDiscoveryResponse response = discoveryService.discoverJobs(request);

        assertEquals(1, response.getDiscovered());
        assertEquals(1, response.getNewJobs());
        assertEquals(0, response.getUpdatedJobs());
        assertEquals(0, response.getUnchangedJobs());
        assertEquals(0, response.getDuplicates());
        assertEquals(0, response.getFailed());

        verify(persistenceHelper).saveNewJob(argThat(job ->
                job.getStatus() == JobStatus.ACTIVE && job.getLastDiscoveredAt() != null
        ));
    }

    // ──────────────────────────────────────────────
    // Status preservation
    // ──────────────────────────────────────────────

    @Test
    void discoverJobs_existingJobRefresh_preservesUserStatusApplied() {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("COMPANY_WEBSITE")
                .maxResults(10)
                .build();

        RawJobListing rawListing = RawJobListing.builder()
                .externalId("JOB-1")
                .title("Java Developer")
                .company("Acme")
                .location("Bangalore")
                .description("Updated description with higher salary")
                .salaryMin(new BigDecimal("100000"))
                .salaryMax(new BigDecimal("150000"))
                .jobUrl("https://acme.com/jobs/1")
                .build();

        ExtractedJob extractedJob = ExtractedJob.builder()
                .title("Java Developer")
                .company("Acme")
                .location("Bangalore")
                .description("Updated description with higher salary")
                .salaryMin(new BigDecimal("100000"))
                .salaryMax(new BigDecimal("150000"))
                .jobUrl("https://acme.com/jobs/1")
                .source("COMPANY_WEBSITE")
                .build();

        Job existingJob = Job.builder()
                .id(50L)
                .externalId("JOB-1")
                .title("Java Developer")
                .company("Acme")
                .location("Bangalore")
                .description("Old description")
                .jobUrl("https://acme.com/jobs/1")
                .source("COMPANY_WEBSITE")
                .status(JobStatus.APPLIED)
                .build();

        when(sourceRegistry.getRequiredSource("COMPANY_WEBSITE")).thenReturn(jobSource);
        when(jobSource.getSourceName()).thenReturn("COMPANY_WEBSITE");
        when(jobSource.discover(request)).thenReturn(List.of(rawListing));
        when(extractionPipeline.extractAndNormalize(any(), eq("COMPANY_WEBSITE"))).thenReturn(extractedJob);
        when(deduplicationService.findExistingJob(any(), any(), any(), any(), any(), any(), any())).thenReturn(Optional.of(existingJob));

        JobDiscoveryResponse response = discoveryService.discoverJobs(request);

        assertEquals(1, response.getDiscovered());
        assertEquals(0, response.getNewJobs());
        assertEquals(1, response.getUpdatedJobs());

        assertEquals(JobStatus.APPLIED, existingJob.getStatus());
        assertEquals("Updated description with higher salary", existingJob.getDescription());
        assertNotNull(existingJob.getLastDiscoveredAt());
        verify(persistenceHelper).updateExistingJob(existingJob);
    }

    @Test
    void discoverJobs_allUserStatusesPreservedOnRefresh() {
        for (JobStatus initialStatus : JobStatus.values()) {
            Job existingJob = Job.builder()
                    .id(1L)
                    .title("Old Title")
                    .company("Acme")
                    .status(initialStatus)
                    .build();

            RawJobListing rawListing = RawJobListing.builder().title("New Title").company("Acme").build();
            ExtractedJob extractedJob = ExtractedJob.builder().title("New Title").company("Acme").source("COMPANY_WEBSITE").build();

            when(sourceRegistry.getRequiredSource("COMPANY_WEBSITE")).thenReturn(jobSource);
            when(jobSource.getSourceName()).thenReturn("COMPANY_WEBSITE");
            when(jobSource.discover(any())).thenReturn(List.of(rawListing));
            when(extractionPipeline.extractAndNormalize(any(), anyString())).thenReturn(extractedJob);
            when(deduplicationService.findExistingJob(any(), any(), any(), any(), any(), any(), any())).thenReturn(Optional.of(existingJob));

            discoveryService.discoverJobs(JobDiscoveryRequest.builder().source("COMPANY_WEBSITE").build());

            assertEquals(initialStatus, existingJob.getStatus(),
                    "Status " + initialStatus + " was not preserved during refresh");
        }
    }

    // ──────────────────────────────────────────────
    // Unchanged listing
    // ──────────────────────────────────────────────

    @Test
    void discoverJobs_unchangedListing_updatesLastDiscoveredAtOnly() {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder().source("COMPANY_WEBSITE").build();
        RawJobListing rawListing = RawJobListing.builder().externalId("JOB-1").title("Java Dev").company("Acme").build();
        ExtractedJob extractedJob = ExtractedJob.builder().title("Java Dev").company("Acme").source("COMPANY_WEBSITE").build();

        Job existingJob = Job.builder()
                .id(1L)
                .externalId("JOB-1")
                .title("Java Dev")
                .company("Acme")
                .source("COMPANY_WEBSITE")
                .status(JobStatus.INTERVIEW)
                .build();

        when(sourceRegistry.getRequiredSource("COMPANY_WEBSITE")).thenReturn(jobSource);
        when(jobSource.getSourceName()).thenReturn("COMPANY_WEBSITE");
        when(jobSource.discover(request)).thenReturn(List.of(rawListing));
        when(extractionPipeline.extractAndNormalize(any(), anyString())).thenReturn(extractedJob);
        when(deduplicationService.findExistingJob(any(), any(), any(), any(), any(), any(), any())).thenReturn(Optional.of(existingJob));

        JobDiscoveryResponse response = discoveryService.discoverJobs(request);

        assertEquals(0, response.getNewJobs());
        assertEquals(0, response.getUpdatedJobs());
        assertEquals(1, response.getUnchangedJobs());
        assertNotNull(existingJob.getLastDiscoveredAt());
    }

    // ──────────────────────────────────────────────
    // Statistics: failed listings
    // ──────────────────────────────────────────────

    @Test
    void discoverJobs_failedListing_incrementsFailedNotDuplicates() {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder().source("COMPANY_WEBSITE").build();
        RawJobListing rawListing = RawJobListing.builder().title("Bad Job").company("Acme").build();

        when(sourceRegistry.getRequiredSource("COMPANY_WEBSITE")).thenReturn(jobSource);
        when(jobSource.getSourceName()).thenReturn("COMPANY_WEBSITE");
        when(jobSource.discover(request)).thenReturn(List.of(rawListing));
        when(extractionPipeline.extractAndNormalize(any(), anyString()))
                .thenThrow(new RuntimeException("Extraction failed"));

        JobDiscoveryResponse response = discoveryService.discoverJobs(request);

        assertEquals(1, response.getDiscovered());
        assertEquals(0, response.getNewJobs());
        assertEquals(0, response.getUpdatedJobs());
        assertEquals(0, response.getUnchangedJobs());
        assertEquals(0, response.getDuplicates());
        assertEquals(1, response.getFailed());
    }

    @Test
    void discoverJobs_sourceException_returnsFailedResponse() {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder().source("COMPANY_WEBSITE").build();

        when(sourceRegistry.getRequiredSource("COMPANY_WEBSITE")).thenReturn(jobSource);
        when(jobSource.getSourceName()).thenReturn("COMPANY_WEBSITE");
        when(jobSource.discover(request)).thenThrow(new RuntimeException("Network timeout"));

        JobDiscoveryResponse response = discoveryService.discoverJobs(request);

        assertEquals(0, response.getDiscovered());
        assertEquals(0, response.getNewJobs());
        assertEquals(1, response.getFailed());
        assertEquals(0, response.getDuplicates());
    }

    // ──────────────────────────────────────────────
    // Field change detection
    // ──────────────────────────────────────────────

    @Test
    void discoverJobs_changedSalary_countsAsUpdated() {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder().source("COMPANY_WEBSITE").build();
        RawJobListing rawListing = RawJobListing.builder().externalId("JOB-1").title("Dev").company("Acme")
                .salaryMin(new BigDecimal("120000")).salaryMax(new BigDecimal("180000")).build();
        ExtractedJob extractedJob = ExtractedJob.builder().title("Dev").company("Acme").source("COMPANY_WEBSITE")
                .salaryMin(new BigDecimal("120000")).salaryMax(new BigDecimal("180000")).build();

        Job existingJob = Job.builder().id(1L).externalId("JOB-1").title("Dev").company("Acme")
                .source("COMPANY_WEBSITE").status(JobStatus.SAVED)
                .salaryMin(new BigDecimal("100000")).salaryMax(new BigDecimal("150000")).build();

        when(sourceRegistry.getRequiredSource("COMPANY_WEBSITE")).thenReturn(jobSource);
        when(jobSource.getSourceName()).thenReturn("COMPANY_WEBSITE");
        when(jobSource.discover(request)).thenReturn(List.of(rawListing));
        when(extractionPipeline.extractAndNormalize(any(), anyString())).thenReturn(extractedJob);
        when(deduplicationService.findExistingJob(any(), any(), any(), any(), any(), any(), any())).thenReturn(Optional.of(existingJob));

        JobDiscoveryResponse response = discoveryService.discoverJobs(request);

        assertEquals(1, response.getUpdatedJobs());
        assertEquals(0, response.getUnchangedJobs());
    }

    @Test
    void discoverJobs_changedDescription_countsAsUpdated() {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder().source("COMPANY_WEBSITE").build();
        RawJobListing rawListing = RawJobListing.builder().externalId("JOB-1").title("Dev").company("Acme")
                .description("New description").build();
        ExtractedJob extractedJob = ExtractedJob.builder().title("Dev").company("Acme").source("COMPANY_WEBSITE")
                .description("New description").build();

        Job existingJob = Job.builder().id(1L).externalId("JOB-1").title("Dev").company("Acme")
                .source("COMPANY_WEBSITE").description("Old description").status(JobStatus.SAVED).build();

        when(sourceRegistry.getRequiredSource("COMPANY_WEBSITE")).thenReturn(jobSource);
        when(jobSource.getSourceName()).thenReturn("COMPANY_WEBSITE");
        when(jobSource.discover(request)).thenReturn(List.of(rawListing));
        when(extractionPipeline.extractAndNormalize(any(), anyString())).thenReturn(extractedJob);
        when(deduplicationService.findExistingJob(any(), any(), any(), any(), any(), any(), any())).thenReturn(Optional.of(existingJob));

        JobDiscoveryResponse response = discoveryService.discoverJobs(request);

        assertEquals(1, response.getUpdatedJobs());
    }

    @Test
    void discoverJobs_changedLocation_countsAsUpdated() {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder().source("COMPANY_WEBSITE").build();
        RawJobListing rawListing = RawJobListing.builder().externalId("JOB-1").title("Dev").company("Acme")
                .location("Mumbai").build();
        ExtractedJob extractedJob = ExtractedJob.builder().title("Dev").company("Acme").source("COMPANY_WEBSITE")
                .location("Mumbai").build();

        Job existingJob = Job.builder().id(1L).externalId("JOB-1").title("Dev").company("Acme")
                .source("COMPANY_WEBSITE").location("Bangalore").status(JobStatus.SAVED).build();

        when(sourceRegistry.getRequiredSource("COMPANY_WEBSITE")).thenReturn(jobSource);
        when(jobSource.getSourceName()).thenReturn("COMPANY_WEBSITE");
        when(jobSource.discover(request)).thenReturn(List.of(rawListing));
        when(extractionPipeline.extractAndNormalize(any(), anyString())).thenReturn(extractedJob);
        when(deduplicationService.findExistingJob(any(), any(), any(), any(), any(), any(), any())).thenReturn(Optional.of(existingJob));

        JobDiscoveryResponse response = discoveryService.discoverJobs(request);

        assertEquals(1, response.getUpdatedJobs());
    }

    // ──────────────────────────────────────────────
    // Validation
    // ──────────────────────────────────────────────

    @Test
    void discoverJobs_nullSource_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
                discoveryService.discoverJobs(JobDiscoveryRequest.builder().source(null).build()));
    }

    @Test
    void discoverJobs_blankSource_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
                discoveryService.discoverJobs(JobDiscoveryRequest.builder().source("  ").build()));
    }

    @Test
    void discoverJobs_maxResultsZero_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
                discoveryService.discoverJobs(JobDiscoveryRequest.builder().source("COMPANY_WEBSITE").maxResults(0).build()));
    }

    @Test
    void discoverJobs_maxResultsNegative_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
                discoveryService.discoverJobs(JobDiscoveryRequest.builder().source("COMPANY_WEBSITE").maxResults(-5).build()));
    }

    @Test
    void discoverJobs_maxResultsOver100_throwsBadRequest() {
        assertThrows(BadRequestException.class, () ->
                discoveryService.discoverJobs(JobDiscoveryRequest.builder().source("COMPANY_WEBSITE").maxResults(101).build()));
    }

}
