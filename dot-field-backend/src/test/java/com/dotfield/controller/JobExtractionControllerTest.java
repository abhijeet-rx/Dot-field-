package com.dotfield.controller;

import com.dotfield.dto.ExtractJobRequest;
import com.dotfield.repository.JobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.security.test.context.support.WithMockUser;

import java.util.HashMap;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "1", roles = "ADMIN")
class JobExtractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobRepository jobRepository;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
    }

    @Test
    void extractJob_success() throws Exception {
        var rawData = new HashMap<String, Object>();
        rawData.put("title", "Cloud Security Architect");
        rawData.put("company", "Datadog");
        rawData.put("location", "New York, NY");
        rawData.put("description", "Secure cloud platform APIs");
        rawData.put("jobUrl", "https://datadog.com/careers/789");
        rawData.put("employmentType", "Full Time");
        rawData.put("remoteType", "Hybrid");
        rawData.put("salaryMin", 190000);
        rawData.put("salaryMax", 250000);
        rawData.put("currency", "USD");

        ExtractJobRequest request = ExtractJobRequest.builder()
                .source("COMPANY_WEBSITE")
                .rawData(rawData)
                .build();

        mockMvc.perform(post("/jobs/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.title").value("Cloud Security Architect"))
                .andExpect(jsonPath("$.data.company").value("Datadog"))
                .andExpect(jsonPath("$.data.source").value("COMPANY_WEBSITE"))
                .andExpect(jsonPath("$.data.employmentType").value("FULL_TIME"))
                .andExpect(jsonPath("$.data.remoteType").value("HYBRID"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        assertEquals(1, jobRepository.count());
    }

    @Test
    void extractJob_unsupportedSource_returnsBadRequest() throws Exception {
        var rawData = new HashMap<String, Object>();
        rawData.put("title", "Software Engineer");
        rawData.put("company", "Meta");

        ExtractJobRequest request = ExtractJobRequest.builder()
                .source("LINKEDIN")
                .rawData(rawData)
                .build();

        mockMvc.perform(post("/jobs/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Unsupported job source: LINKEDIN"));
    }

    @Test
    void extractJob_missingTitle_returnsBadRequest() throws Exception {
        var rawData = new HashMap<String, Object>();
        rawData.put("company", "Meta"); // Missing title

        ExtractJobRequest request = ExtractJobRequest.builder()
                .source("COMPANY_WEBSITE")
                .rawData(rawData)
                .build();

        mockMvc.perform(post("/jobs/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Job title is required"));
    }

    @Test
    void extractJob_missingCompany_returnsBadRequest() throws Exception {
        var rawData = new HashMap<String, Object>();
        rawData.put("title", "Software Engineer"); // Missing company

        ExtractJobRequest request = ExtractJobRequest.builder()
                .source("COMPANY_WEBSITE")
                .rawData(rawData)
                .build();

        mockMvc.perform(post("/jobs/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Company name is required"));
    }

    @Test
    void extractJob_missingSource_returnsBadRequest() throws Exception {
        var rawData = new HashMap<String, Object>();
        rawData.put("title", "Software Engineer");
        rawData.put("company", "Meta");

        ExtractJobRequest request = ExtractJobRequest.builder()
                .source("") // Missing source
                .rawData(rawData)
                .build();

        mockMvc.perform(post("/jobs/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.source").value("Source is required"));
    }

    @Test
    void extractJob_invalidSalaryRange_returnsBadRequest() throws Exception {
        var rawData = new HashMap<String, Object>();
        rawData.put("title", "Software Engineer");
        rawData.put("company", "Meta");
        rawData.put("salaryMin", 300000);
        rawData.put("salaryMax", 150000);

        ExtractJobRequest request = ExtractJobRequest.builder()
                .source("COMPANY_WEBSITE")
                .rawData(rawData)
                .build();

        mockMvc.perform(post("/jobs/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Minimum salary cannot be greater than maximum salary"));
    }
}
