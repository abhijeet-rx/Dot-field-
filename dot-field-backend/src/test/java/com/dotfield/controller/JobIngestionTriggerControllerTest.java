package com.dotfield.controller;

import com.dotfield.discovery.JobDiscoveryService;
import com.dotfield.discovery.JobSource;
import com.dotfield.discovery.JobSourceRegistry;
import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.RawJobListing;
import com.dotfield.repository.JobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
class JobIngestionTriggerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobDiscoveryService discoveryService;

    @MockBean
    private JobSourceRegistry sourceRegistry;

    private JobSource mockJobSource;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
        mockJobSource = org.mockito.Mockito.mock(JobSource.class);
        when(sourceRegistry.getRequiredSource("REMOTIVE")).thenReturn(mockJobSource);
        when(sourceRegistry.getAllSources()).thenReturn(List.of(mockJobSource));
        when(mockJobSource.getSourceName()).thenReturn("REMOTIVE");
        discoveryService.setFreshnessThresholdDays(7);
    }

    @Test
    @DisplayName("1. Authorized Request — Admin role receives 200 OK")
    @WithMockUser(username = "1", roles = "ADMIN")
    void triggerIngestion_authorizedAdmin_returns200Ok() throws Exception {
        RawJobListing listing = RawJobListing.builder()
                .externalId("INGEST-1")
                .title("DevOps Engineer")
                .company("CloudInc")
                .location("Remote - India")
                .jobUrl("https://remotive.com/jobs/ingest-1")
                .build();
        when(mockJobSource.discover(any())).thenReturn(List.of(listing));

        mockMvc.perform(post("/jobs/ingestion/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Job ingestion completed successfully"))
                .andExpect(jsonPath("$.data.sourcesProcessed").value(1))
                .andExpect(jsonPath("$.data.jobsFetched").value(1))
                .andExpect(jsonPath("$.data.jobsInserted").value(1))
                .andExpect(jsonPath("$.data.jobsUpdated").value(0))
                .andExpect(jsonPath("$.data.duplicates").value(0))
                .andExpect(jsonPath("$.data.failed").value(0));
    }

    @Test
    @DisplayName("2. Unauthorized Request — User role receives 403 Forbidden")
    @WithMockUser(username = "2", roles = "USER")
    void triggerIngestion_unauthorizedUser_returns403Forbidden() throws Exception {
        mockMvc.perform(post("/jobs/ingestion/run"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied: You do not have permission to access this resource"));
    }

    @Test
    @DisplayName("3. Unauthenticated Request — Missing token receives 401 Unauthorized")
    void triggerIngestion_unauthenticated_returns401Unauthorized() throws Exception {
        mockMvc.perform(post("/jobs/ingestion/run"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("4. Successful Run — Correctly aggregates ingestion counts across sources")
    @WithMockUser(username = "1", roles = "ADMIN")
    void triggerIngestion_successfulRun_aggregatesCountsCorrectly() throws Exception {
        RawJobListing listing1 = RawJobListing.builder()
                .externalId("INGEST-2")
                .title("Frontend Developer")
                .company("WebCorp")
                .location("Bengaluru, India")
                .isIndiaRelevant(true)
                .build();
        RawJobListing listing2 = RawJobListing.builder()
                .externalId("INGEST-3")
                .title("Backend Developer")
                .company("WebCorp")
                .location("Bengaluru, India")
                .isIndiaRelevant(true)
                .build();
        when(mockJobSource.discover(any())).thenReturn(List.of(listing1, listing2));

        mockMvc.perform(post("/jobs/ingestion/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourcesProcessed").value(1))
                .andExpect(jsonPath("$.data.jobsFetched").value(2))
                .andExpect(jsonPath("$.data.jobsInserted").value(2))
                .andExpect(jsonPath("$.data.failed").value(0));
    }

    @Test
    @DisplayName("5. Source Failure — Isolates error per source and reports failure count")
    @WithMockUser(username = "1", roles = "ADMIN")
    void triggerIngestion_sourceFailure_reportsFailedCountAndIsolatesError() throws Exception {
        when(mockJobSource.discover(any())).thenThrow(new RuntimeException("Source connection timeout"));

        mockMvc.perform(post("/jobs/ingestion/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourcesProcessed").value(1))
                .andExpect(jsonPath("$.data.jobsFetched").value(0))
                .andExpect(jsonPath("$.data.jobsInserted").value(0))
                .andExpect(jsonPath("$.data.failed").value(1));
    }

    @Test
    @DisplayName("6. Concurrent Trigger — Simultaneous run attempt receives 409 Conflict")
    @WithMockUser(username = "1", roles = "ADMIN")
    void triggerIngestion_concurrentTrigger_returns409Conflict() throws Exception {
        CountDownLatch latchInSource = new CountDownLatch(1);
        CountDownLatch latchTest = new CountDownLatch(1);

        when(mockJobSource.discover(any())).thenAnswer(invocation -> {
            latchInSource.countDown();
            latchTest.await(5, TimeUnit.SECONDS);
            return List.of();
        });

        var executor = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> firstTriggerFuture = executor.submit(() -> {
                discoveryService.runManualIngestion(JobDiscoveryRequest.builder().source("REMOTIVE").build());
                return true;
            });

            // Wait until first trigger enters source execution
            latchInSource.await(5, TimeUnit.SECONDS);

            // Second trigger attempt should immediately fail with 409 Conflict
            mockMvc.perform(post("/jobs/ingestion/run"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("Job ingestion run is already in progress. Please wait for the current run to complete."));

            // Allow first trigger to complete cleanly
            latchTest.countDown();
            firstTriggerFuture.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("7. Response Structure — Matches required summary schema")
    @WithMockUser(username = "1", roles = "ADMIN")
    void triggerIngestion_responseStructure_matchesRequiredSummarySchema() throws Exception {
        when(mockJobSource.discover(any())).thenReturn(List.of());

        mockMvc.perform(post("/jobs/ingestion/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"REMOTIVE\",\"maxResults\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourcesProcessed").exists())
                .andExpect(jsonPath("$.data.jobsFetched").exists())
                .andExpect(jsonPath("$.data.jobsInserted").exists())
                .andExpect(jsonPath("$.data.jobsUpdated").exists())
                .andExpect(jsonPath("$.data.duplicates").exists())
                .andExpect(jsonPath("$.data.failed").exists());
    }
}
