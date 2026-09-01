package com.dotfield.discovery.india;

import com.dotfield.discovery.*;
import com.dotfield.discovery.source.CompanyCareerPageSource;
import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.JobDiscoveryResponse;
import com.dotfield.dto.RawJobListing;
import com.dotfield.entity.Job;
import com.dotfield.extractor.JobExtractionPipeline;
import com.dotfield.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IndiaJobPipelineIntegrationTest {

    private JobSourceRegistry sourceRegistry;
    private JobExtractionPipeline extractionPipeline;
    private JobDeduplicationService deduplicationService;
    private JobDiscoveryPersistenceHelper persistenceHelper;
    private JobRepository jobRepository;
    private JobIngestionMonitor ingestionMonitor;
    private IndiaJobFilter indiaJobFilter;
    private IndiaLocationNormalizer locationNormalizer;

    private JobDiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        locationNormalizer = new IndiaLocationNormalizer();
        indiaJobFilter = new IndiaJobFilter(locationNormalizer);

        JobSource mockSource = mock(JobSource.class);
        when(mockSource.getSourceName()).thenReturn("MOCK_INDIA");
        when(mockSource.supports(any())).thenReturn(true);

        RawJobListing indiaJob = RawJobListing.builder()
                .externalId("IND-101")
                .title("Backend Engineer")
                .company("Swiggy")
                .location("Bengaluru, India")
                .jobUrl("https://swiggy.com/jobs/101")
                .source("MOCK_INDIA")
                .build();

        RawJobListing usJob = RawJobListing.builder()
                .externalId("US-202")
                .title("Product Manager")
                .company("US Corp")
                .location("San Francisco, USA")
                .jobUrl("https://uscorp.com/jobs/202")
                .source("MOCK_INDIA")
                .build();

        when(mockSource.discover(any())).thenReturn(List.of(indiaJob, usJob));

        sourceRegistry = new JobSourceRegistry(List.of(mockSource));
        extractionPipeline = mock(JobExtractionPipeline.class);
        deduplicationService = mock(JobDeduplicationService.class);
        persistenceHelper = mock(JobDiscoveryPersistenceHelper.class);
        jobRepository = mock(JobRepository.class);
        ingestionMonitor = mock(JobIngestionMonitor.class);

        when(extractionPipeline.extractAndNormalize(any(), any())).thenAnswer(invocation -> {
            var raw = (java.util.Map<String, Object>) invocation.getArgument(0);
            return com.dotfield.extractor.ExtractedJob.builder()
                    .title((String) raw.get("title"))
                    .company((String) raw.get("company"))
                    .location((String) raw.get("location"))
                    .jobUrl((String) raw.get("jobUrl"))
                    .source((String) invocation.getArgument(1))
                    .build();
        });

        when(deduplicationService.canonicalizeUrl(any())).thenAnswer(i -> i.getArgument(0));
        when(deduplicationService.generateFingerprint(any(), any(), any(), any())).thenReturn("mock-fingerprint");
        when(deduplicationService.findExistingJob(any(), any(), any(), any(), any(), any(), any())).thenReturn(Optional.empty());

        discoveryService = new JobDiscoveryService(
                sourceRegistry,
                extractionPipeline,
                deduplicationService,
                persistenceHelper,
                jobRepository,
                ingestionMonitor,
                indiaJobFilter,
                locationNormalizer
        );
    }

    @Test
    @DisplayName("End-to-end pipeline accepts India jobs and filters out foreign jobs")
    void pipelineFiltersForeignJobsAndPersistsIndiaJobs() {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("MOCK_INDIA")
                .build();

        JobDiscoveryResponse response = discoveryService.discoverJobs(request);

        assertThat(response.getDiscovered()).isEqualTo(2);
        assertThat(response.getNewJobs()).isEqualTo(1); // Only the 1 India job should be persisted

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(persistenceHelper, times(1)).saveNewJob(jobCaptor.capture());

        Job savedJob = jobCaptor.getValue();
        assertThat(savedJob.getTitle()).isEqualTo("Backend Engineer");
        assertThat(savedJob.getNormalizedCity()).isEqualTo("Bengaluru");
        assertThat(savedJob.getNormalizedCountry()).isEqualTo("IN");
        assertThat(savedJob.getIsIndiaRelevant()).isTrue();
    }
}
