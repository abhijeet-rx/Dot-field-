package com.dotfield.controller;

import com.dotfield.discovery.JobDiscoveryService;
import com.dotfield.discovery.JobSource;
import com.dotfield.discovery.JobSourceRegistry;
import com.dotfield.dto.RawJobListing;
import com.dotfield.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobIngestionMonitoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    @DisplayName("1. Successful Run — Status reports overall metrics and per-source SUCCESS state")
    @WithMockUser(username = "1", roles = "ADMIN")
    void getIngestionStatus_successfulRun_reportsCorrectMetricsAndStatus() throws Exception {
        RawJobListing listing = RawJobListing.builder()
                .externalId("MON-1")
                .title("Site Reliability Engineer")
                .company("CloudOps")
                .location("Remote - India")
                .jobUrl("https://remotive.com/jobs/mon-1")
                .build();
        when(mockJobSource.discover(any())).thenReturn(List.of(listing));

        // Trigger manual run first
        mockMvc.perform(post("/jobs/ingestion/run"))
                .andExpect(status().isOk());

        // Query monitoring status endpoint
        mockMvc.perform(get("/jobs/ingestion/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ingestion status retrieved successfully"))
                .andExpect(jsonPath("$.data.lastRun", notNullValue()))
                .andExpect(jsonPath("$.data.sourcesProcessed").value(1))
                .andExpect(jsonPath("$.data.jobsFetched").value(1))
                .andExpect(jsonPath("$.data.jobsInserted").value(1))
                .andExpect(jsonPath("$.data.failures").value(0))
                .andExpect(jsonPath("$.data.sources[0].source").value("REMOTIVE"))
                .andExpect(jsonPath("$.data.sources[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.sources[0].lastSuccessfulRun", notNullValue()))
                .andExpect(jsonPath("$.data.sources[0].jobsFetched").value(1));
    }

    @Test
    @DisplayName("2. Failed Source — Reports FAILED status and redacts sensitive tokens in error message")
    @WithMockUser(username = "1", roles = "ADMIN")
    void getIngestionStatus_failedSource_reportsFailedAndSanitizesErrorMessage() throws Exception {
        when(mockJobSource.discover(any())).thenThrow(
                new RuntimeException("Connection timeout accessing endpoint api_key=secret_token_12345 with Authorization Bearer secret_hdr_token")
        );

        // Trigger run with failing source
        mockMvc.perform(post("/jobs/ingestion/run"))
                .andExpect(status().isOk());

        // Query monitoring status
        mockMvc.perform(get("/jobs/ingestion/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.failures").value(1))
                .andExpect(jsonPath("$.data.sources[0].source").value("REMOTIVE"))
                .andExpect(jsonPath("$.data.sources[0].status").value("FAILED"))
                .andExpect(jsonPath("$.data.sources[0].lastFailure", notNullValue()))
                .andExpect(jsonPath("$.data.sources[0].errorMessage").value(
                        "Connection timeout accessing endpoint api_key=[REDACTED] with Authorization [REDACTED]"
                ));
    }

    @Test
    @DisplayName("3. Partial Success — Reports failure counts when individual listings fail during extraction")
    @WithMockUser(username = "1", roles = "ADMIN")
    void getIngestionStatus_partialSuccess_reportsFailureCounts() throws Exception {
        RawJobListing validListing = RawJobListing.builder()
                .externalId("MON-2")
                .title("Valid Job Title")
                .company("GoodCorp")
                .location("Bengaluru, India")
                .isIndiaRelevant(true)
                .build();
        RawJobListing invalidListing = RawJobListing.builder()
                .externalId("MON-3")
                .title(null) // Missing title causes extraction exception
                .company("GoodCorp")
                .location("Bengaluru, India")
                .isIndiaRelevant(true)
                .build();
        when(mockJobSource.discover(any())).thenReturn(List.of(validListing, invalidListing));

        mockMvc.perform(post("/jobs/ingestion/run"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/jobs/ingestion/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobsFetched").value(2))
                .andExpect(jsonPath("$.data.jobsInserted").value(1))
                .andExpect(jsonPath("$.data.failures").value(1))
                .andExpect(jsonPath("$.data.sources[0].status").value("PARTIAL_SUCCESS"));
    }

    @Test
    @DisplayName("4. Empty Source — Reports NO_JOBS status when source returns 0 listings")
    @WithMockUser(username = "1", roles = "ADMIN")
    void getIngestionStatus_emptySource_reportsNoJobsStatus() throws Exception {
        when(mockJobSource.discover(any())).thenReturn(List.of());

        mockMvc.perform(post("/jobs/ingestion/run"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/jobs/ingestion/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobsFetched").value(0))
                .andExpect(jsonPath("$.data.sources[0].status").value("NO_JOBS"))
                .andExpect(jsonPath("$.data.sources[0].lastSuccessfulRun", notNullValue()));
    }

    @Test
    @DisplayName("5. Unauthorized Monitoring Request — User role gets 403 Forbidden; unauthenticated gets 401 Unauthorized")
    void getIngestionStatus_unauthorized_returnsAppropriateErrorCodes() throws Exception {
        // Unauthenticated request
        mockMvc.perform(get("/jobs/ingestion/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        // Candidate USER role request
        mockMvc.perform(get("/jobs/ingestion/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("5b. Candidate USER Role Access — Receives 403 Forbidden")
    @WithMockUser(username = "2", roles = "USER")
    void getIngestionStatus_candidateUserRole_returns403Forbidden() throws Exception {
        mockMvc.perform(get("/jobs/ingestion/status"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied: You do not have permission to access this resource"));
    }
}
