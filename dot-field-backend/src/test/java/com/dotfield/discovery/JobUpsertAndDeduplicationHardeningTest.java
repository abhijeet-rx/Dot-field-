package com.dotfield.discovery;

import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.JobDiscoveryResponse;
import com.dotfield.dto.RawJobListing;
import com.dotfield.entity.Job;
import com.dotfield.entity.JobStatus;
import com.dotfield.extractor.ExtractedJob;
import com.dotfield.extractor.JobExtractionPipeline;
import com.dotfield.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class JobUpsertAndDeduplicationHardeningTest {

    private JobDiscoveryService discoveryService;
    private JobSourceRegistry registry;
    private JobExtractionPipeline extractionPipeline;
    private JobDeduplicationService deduplicationService;
    private JobDiscoveryPersistenceHelper persistenceHelper;
    private JobRepository jobRepository;

    @BeforeEach
    void setUp() {
        registry = mock(JobSourceRegistry.class);
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
    @DisplayName("Upsert Strategy — Level 1 Duplicate External ID updates existing job")
    void upsert_duplicateExternalId_updatesExistingJob() {
        JobSource source = mock(JobSource.class);
        when(source.getSourceName()).thenReturn("REMOTIVE");
        when(registry.getRequiredSource("REMOTIVE")).thenReturn(source);

        RawJobListing rawListing = RawJobListing.builder()
                .externalId("REM-999")
                .title("Staff Java Engineer")
                .company("Acme")
                .location("Bengaluru, India")
                .isIndiaRelevant(true)
                .source("REMOTIVE")
                .build();
        when(source.discover(any())).thenReturn(List.of(rawListing));

        ExtractedJob extracted = ExtractedJob.builder()
                .title("Staff Java Engineer")
                .company("Acme")
                .location("Bengaluru, India")
                .isIndiaRelevant(true)
                .source("REMOTIVE")
                .build();
        when(extractionPipeline.extractAndNormalize(any(), eq("REMOTIVE"))).thenReturn(extracted);

        Job existingJob = Job.builder()
                .id(1L)
                .externalId("REM-999")
                .title("Senior Java Engineer") // changed
                .company("Acme")
                .location("Bengaluru, India")
                .isIndiaRelevant(true)
                .source("REMOTIVE")
                .status(JobStatus.SAVED)
                .build();

        // Level 1 match
        when(deduplicationService.findExistingJob(eq("REMOTIVE"), eq("REM-999"), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(existingJob));

        JobDiscoveryResponse response = discoveryService.discoverJobs(JobDiscoveryRequest.builder().source("REMOTIVE").build());

        assertNotNull(response);
        assertEquals(1, response.getDiscovered());
        assertEquals(0, response.getNewJobs());
        assertEquals(1, response.getUpdatedJobs());
        assertEquals("Staff Java Engineer", existingJob.getTitle()); // updated
        verify(persistenceHelper, times(1)).updateExistingJob(existingJob);
    }

    @Test
    @DisplayName("Upsert Strategy — Level 2 Duplicate Canonical URL updates existing job when external ID is missing")
    void upsert_duplicateCanonicalUrl_updatesExistingJob() {
        JobSource source = mock(JobSource.class);
        when(source.getSourceName()).thenReturn("COMPANY_WEBSITE");
        when(registry.getRequiredSource("COMPANY_WEBSITE")).thenReturn(source);

        RawJobListing rawListing = RawJobListing.builder()
                .jobUrl("https://acme.com/jobs/123?utm_source=linkedin")
                .title("Backend Engineer")
                .company("Acme")
                .location("Bengaluru, India")
                .isIndiaRelevant(true)
                .source("COMPANY_WEBSITE")
                .build();
        when(source.discover(any())).thenReturn(List.of(rawListing));

        ExtractedJob extracted = ExtractedJob.builder()
                .jobUrl("https://acme.com/jobs/123")
                .title("Backend Engineer")
                .company("Acme")
                .location("Bengaluru, India")
                .isIndiaRelevant(true)
                .source("COMPANY_WEBSITE")
                .build();
        when(extractionPipeline.extractAndNormalize(any(), eq("COMPANY_WEBSITE"))).thenReturn(extracted);
        when(deduplicationService.canonicalizeUrl("https://acme.com/jobs/123")).thenReturn("https://acme.com/jobs/123");

        Job existingJob = Job.builder()
                .id(2L)
                .jobUrl("https://acme.com/jobs/123")
                .canonicalUrl("https://acme.com/jobs/123")
                .title("Backend Engineer")
                .company("Acme")
                .location("Bengaluru, India")
                .isIndiaRelevant(true)
                .source("COMPANY_WEBSITE")
                .status(JobStatus.SAVED)
                .build();

        // Level 2 match
        when(deduplicationService.findExistingJob(eq("COMPANY_WEBSITE"), isNull(), eq("https://acme.com/jobs/123"), any(), any(), any(), any()))
                .thenReturn(Optional.of(existingJob));

        JobDiscoveryResponse response = discoveryService.discoverJobs(JobDiscoveryRequest.builder().source("COMPANY_WEBSITE").build());

        assertNotNull(response);
        assertEquals(1, response.getDiscovered());
        assertEquals(0, response.getNewJobs());
        assertEquals(0, response.getUpdatedJobs());
        assertEquals(1, response.getUnchangedJobs());
        verify(persistenceHelper, times(1)).updateExistingJob(existingJob);
    }

    @Test
    @DisplayName("Null Protection Strategy — Missing incoming optional fields do NOT overwrite existing non-null data")
    void upsert_missingOptionalFields_preservesExistingData() {
        JobSource source = mock(JobSource.class);
        when(source.getSourceName()).thenReturn("REMOTIVE");
        when(registry.getRequiredSource("REMOTIVE")).thenReturn(source);

        RawJobListing rawListing = RawJobListing.builder()
                .externalId("REM-500")
                .title("Java Engineer")
                .company("Acme")
                .location("Bengaluru, India")
                .isIndiaRelevant(true)
                .salaryMin(null) // incoming null salaryMin
                .source("REMOTIVE")
                .build();
        when(source.discover(any())).thenReturn(List.of(rawListing));

        ExtractedJob extracted = ExtractedJob.builder()
                .title("Java Engineer")
                .company("Acme")
                .location("Bengaluru, India")
                .isIndiaRelevant(true)
                .salaryMin(null)
                .source("REMOTIVE")
                .build();
        when(extractionPipeline.extractAndNormalize(any(), eq("REMOTIVE"))).thenReturn(extracted);

        Job existingJob = Job.builder()
                .id(10L)
                .externalId("REM-500")
                .title("Java Engineer")
                .company("Acme")
                .location("Bengaluru, India")
                .isIndiaRelevant(true)
                .salaryMin(new BigDecimal("120000")) // existing valuable salary
                .source("REMOTIVE")
                .status(JobStatus.SAVED)
                .build();

        when(deduplicationService.findExistingJob(eq("REMOTIVE"), eq("REM-500"), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(existingJob));

        JobDiscoveryResponse response = discoveryService.discoverJobs(JobDiscoveryRequest.builder().source("REMOTIVE").build());

        assertNotNull(response);
        assertEquals(0, response.getUpdatedJobs());
        assertEquals(1, response.getUnchangedJobs());

        // Verify existing salary was NOT overwritten with null!
        assertEquals("Bengaluru, India", existingJob.getLocation());
        assertEquals(new BigDecimal("120000"), existingJob.getSalaryMin());
    }

    @Test
    @DisplayName("Changed Description Strategy — Updated description on existing job modifies record without creating duplicate")
    void upsert_changedDescription_updatesExistingJobWithoutDuplicate() {
        JobSource source = mock(JobSource.class);
        when(source.getSourceName()).thenReturn("REMOTIVE");
        when(registry.getRequiredSource("REMOTIVE")).thenReturn(source);

        RawJobListing rawListing = RawJobListing.builder()
                .externalId("REM-777")
                .title("Java Engineer")
                .company("Acme")
                .location("Bengaluru, India")
                .isIndiaRelevant(true)
                .description("New updated job description with Spring Boot details.")
                .source("REMOTIVE")
                .build();
        when(source.discover(any())).thenReturn(List.of(rawListing));

        ExtractedJob extracted = ExtractedJob.builder()
                .title("Java Engineer")
                .company("Acme")
                .location("Bengaluru, India")
                .isIndiaRelevant(true)
                .description("New updated job description with Spring Boot details.")
                .source("REMOTIVE")
                .build();
        when(extractionPipeline.extractAndNormalize(any(), eq("REMOTIVE"))).thenReturn(extracted);

        Job existingJob = Job.builder()
                .id(77L)
                .externalId("REM-777")
                .title("Java Engineer")
                .company("Acme")
                .location("Bengaluru, India")
                .isIndiaRelevant(true)
                .description("Old short description.")
                .source("REMOTIVE")
                .status(JobStatus.SAVED)
                .build();

        when(deduplicationService.findExistingJob(eq("REMOTIVE"), eq("REM-777"), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(existingJob));

        JobDiscoveryResponse response = discoveryService.discoverJobs(JobDiscoveryRequest.builder().source("REMOTIVE").build());

        assertNotNull(response);
        assertEquals(1, response.getDiscovered());
        assertEquals(0, response.getNewJobs());
        assertEquals(1, response.getUpdatedJobs());
        assertEquals("New updated job description with Spring Boot details.", existingJob.getDescription());
        verify(persistenceHelper, times(1)).updateExistingJob(existingJob);
        verify(persistenceHelper, never()).saveNewJob(any());
    }

    @Test
    @DisplayName("Concurrent Insert Safety — DataIntegrityViolationException triggers re-fetch and fallback update")
    void upsert_concurrentInsert_catchesConstraintViolationAndRefetches() {
        JobSource source = mock(JobSource.class);
        when(source.getSourceName()).thenReturn("REMOTIVE");
        when(registry.getRequiredSource("REMOTIVE")).thenReturn(source);

        RawJobListing rawListing = RawJobListing.builder()
                .externalId("REM-CONCURRENT")
                .title("React Developer")
                .company("Acme")
                .location("Bengaluru, India")
                .isIndiaRelevant(true)
                .source("REMOTIVE")
                .build();
        when(source.discover(any())).thenReturn(List.of(rawListing));

        ExtractedJob extracted = ExtractedJob.builder()
                .title("React Developer")
                .company("Acme")
                .location("Bengaluru, India")
                .isIndiaRelevant(true)
                .source("REMOTIVE")
                .build();
        when(extractionPipeline.extractAndNormalize(any(), eq("REMOTIVE"))).thenReturn(extracted);

        // Pre-check returns empty (simulating race condition where concurrent worker inserts right after)
        when(deduplicationService.findExistingJob(eq("REMOTIVE"), eq("REM-CONCURRENT"), any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        // saveNewJob throws DataIntegrityViolationException (simulating DB unique constraint)
        when(persistenceHelper.saveNewJob(any()))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("uk_jobs_source_external_id"));

        // Fallback re-fetch finds the job inserted by concurrent worker
        Job concurrentInsertedJob = Job.builder()
                .id(888L)
                .externalId("REM-CONCURRENT")
                .title("React Developer")
                .company("Acme")
                .location("Bengaluru, India")
                .isIndiaRelevant(true)
                .source("REMOTIVE")
                .status(JobStatus.SAVED)
                .build();

        when(deduplicationService.findExistingJob(eq("REMOTIVE"), eq("REM-CONCURRENT"), any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty()) // first call before insert
                .thenReturn(Optional.of(concurrentInsertedJob)); // fallback call after exception

        JobDiscoveryResponse response = discoveryService.discoverJobs(JobDiscoveryRequest.builder().source("REMOTIVE").build());

        assertNotNull(response);
        assertEquals(1, response.getDiscovered());
        assertEquals(0, response.getNewJobs());

        // Verify fallback update was executed cleanly
        verify(persistenceHelper, times(1)).updateExistingJob(concurrentInsertedJob);
    }
}
