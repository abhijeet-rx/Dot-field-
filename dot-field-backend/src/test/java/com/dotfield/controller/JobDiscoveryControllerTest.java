package com.dotfield.controller;

import com.dotfield.discovery.JobSource;
import com.dotfield.discovery.JobSourceRegistry;
import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.RawJobListing;
import com.dotfield.exception.BadRequestException;
import com.dotfield.repository.JobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "1", roles = "ADMIN")
class JobDiscoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobRepository jobRepository;

    @MockBean
    private JobSource mockJobSource;

    @MockBean
    private JobSourceRegistry sourceRegistry;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
        when(sourceRegistry.getRequiredSource("COMPANY_WEBSITE")).thenReturn(mockJobSource);
        when(sourceRegistry.getRequiredSource("LINKEDIN")).thenThrow(new BadRequestException("Unsupported job source: LINKEDIN"));
        when(mockJobSource.getSourceName()).thenReturn("COMPANY_WEBSITE");

        RawJobListing testListing = RawJobListing.builder()
                .externalId("JOB-CW-101")
                .title("Java Backend Developer")
                .company("Acme Corp")
                .location("Bengaluru, India")
                .isIndiaRelevant(true)
                .source("COMPANY_WEBSITE")
                .jobUrl("https://acme.com/jobs/101")
                .description("Java Spring Boot developer role in Bengaluru.")
                .build();

        when(mockJobSource.discover(any())).thenReturn(List.of(testListing));
    }

    @Test
    void discoverJobs_success_returns200AndApiResponse() throws Exception {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("COMPANY_WEBSITE")
                .keyword("Java")
                .location("Bangalore")
                .maxResults(10)
                .build();

        mockMvc.perform(post("/jobs/discover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Job discovery completed successfully"))
                .andExpect(jsonPath("$.data.discovered", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.newJobs", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.duplicates").value(0))
                .andExpect(jsonPath("$.data.failed").value(0))
                .andExpect(jsonPath("$.data.sourceResults", notNullValue()))
                .andExpect(jsonPath("$.data.sourceResults[0].source").value("COMPANY_WEBSITE"));
    }

    @Test
    void discoverJobs_missingSource_returns400BadRequest() throws Exception {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("")
                .build();

        mockMvc.perform(post("/jobs/discover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void discoverJobs_maxResultsExceeds100_returns400BadRequest() throws Exception {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("COMPANY_WEBSITE")
                .maxResults(150)
                .build();

        mockMvc.perform(post("/jobs/discover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void discoverJobs_maxResultsZero_returns400BadRequest() throws Exception {
        String json = "{\"source\": \"COMPANY_WEBSITE\", \"maxResults\": 0}";

        mockMvc.perform(post("/jobs/discover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void discoverJobs_maxResultsNegative_returns400BadRequest() throws Exception {
        String json = "{\"source\": \"COMPANY_WEBSITE\", \"maxResults\": -5}";

        mockMvc.perform(post("/jobs/discover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void discoverJobs_unsupportedSource_returns400WithMessage() throws Exception {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("LINKEDIN")
                .maxResults(10)
                .build();

        mockMvc.perform(post("/jobs/discover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Unsupported job source")));
    }

}
