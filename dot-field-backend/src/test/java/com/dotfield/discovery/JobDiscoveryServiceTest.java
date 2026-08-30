package com.dotfield.discovery;

import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.JobDiscoveryResponse;
import com.dotfield.dto.RawJobListing;
import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.Job;
import com.dotfield.entity.JobStatus;
import com.dotfield.entity.RemoteType;
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
    private JobRepository jobRepository;

    @Mock
    private JobSource jobSource;

    private JobDiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        discoveryService = new JobDiscoveryService(sourceRegistry, extractionPipeline, deduplicationService, jobRepository);
    }

    @Test
    void discoverJobs_newJobCreated_defaultStatusSavedAndLastDiscoveredAtSet() {
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

        Job savedEntity = Job.builder().id(100L).status(JobStatus.SAVED).lastDiscoveredAt(LocalDateTime.now()).build();
        when(jobRepository.save(any(Job.class))).thenReturn(savedEntity);

        JobDiscoveryResponse response = discoveryService.discoverJobs(request);

        assertEquals(1, response.getDiscovered());
        assertEquals(1, response.getNewJobs());
        assertEquals(0, response.getUpdatedJobs());
        assertEquals(0, response.getUnchangedJobs());

        verify(jobRepository).save(argThat(job ->
                job.getStatus() == JobStatus.SAVED && job.getLastDiscoveredAt() != null
        ));
    }

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
                .status(JobStatus.APPLIED) // User-tracked status!
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

        // CRITICAL CHECK: Existing status remains APPLIED!
        assertEquals(JobStatus.APPLIED, existingJob.getStatus());
        assertEquals("Updated description with higher salary", existingJob.getDescription());
        assertNotNull(existingJob.getLastDiscoveredAt());
        verify(jobRepository).save(existingJob);
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

            // Status MUST remain completely unchanged
            assertEquals(initialStatus, existingJob.getStatus());
        }
    }

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

}
