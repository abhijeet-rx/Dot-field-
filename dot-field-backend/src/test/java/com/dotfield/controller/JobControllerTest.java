package com.dotfield.controller;

import com.dotfield.dto.CreateJobRequest;
import com.dotfield.dto.UpdateJobRequest;
import com.dotfield.dto.UpdateJobStatusRequest;
import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.JobStatus;
import com.dotfield.entity.RemoteType;
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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "1", roles = "ADMIN")
class JobControllerTest {

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
    void createJob_success() throws Exception {
        CreateJobRequest request = CreateJobRequest.builder()
                .title("Fullstack Developer")
                .company("Acme Corp")
                .location("San Francisco, CA")
                .description("Build React & Java apps")
                .jobUrl("https://acme.com/jobs/456")
                .employmentType(EmploymentType.FULL_TIME)
                .remoteType(RemoteType.REMOTE)
                .salaryMin(new BigDecimal("100000.00"))
                .salaryMax(new BigDecimal("150000.00"))
                .currency("USD")
                .postedDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.title").value("Fullstack Developer"))
                .andExpect(jsonPath("$.data.company").value("Acme Corp"))
                .andExpect(jsonPath("$.data.source").value("MANUAL"))
                .andExpect(jsonPath("$.data.status").value("SAVED"))
                .andExpect(jsonPath("$.data.employmentType").value("FULL_TIME"))
                .andExpect(jsonPath("$.data.remoteType").value("REMOTE"))
                .andExpect(jsonPath("$.data.createdAt", notNullValue()))
                .andExpect(jsonPath("$.data.updatedAt", notNullValue()));
    }

    @Test
    void createJob_missingTitleOrCompany_returnsBadRequest() throws Exception {
        CreateJobRequest invalidRequest = CreateJobRequest.builder()
                .title("")
                .company(" ")
                .build();

        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.title").value("Job title is required"))
                .andExpect(jsonPath("$.errors.company").value("Company name is required"));
    }

    @Test
    void createJob_negativeSalaryMinOrMax_returnsBadRequest() throws Exception {
        CreateJobRequest invalidRequest1 = CreateJobRequest.builder()
                .title("Software Engineer")
                .company("Tech Co")
                .salaryMin(new BigDecimal("-100.00"))
                .build();

        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.salaryMin").value("Minimum salary cannot be negative"));

        CreateJobRequest invalidRequest2 = CreateJobRequest.builder()
                .title("Software Engineer")
                .company("Tech Co")
                .salaryMax(new BigDecimal("-50.00"))
                .build();

        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.salaryMax").value("Maximum salary cannot be negative"));
    }

    @Test
    void createJob_salaryMinGreaterThanMax_returnsBadRequest() throws Exception {
        CreateJobRequest invalidRequest = CreateJobRequest.builder()
                .title("Software Engineer")
                .company("Tech Co")
                .salaryMin(new BigDecimal("200000.00"))
                .salaryMax(new BigDecimal("100000.00"))
                .build();

        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Minimum salary cannot be greater than maximum salary"));
    }

    @Test
    void getJobById_success() throws Exception {
        CreateJobRequest request = CreateJobRequest.builder()
                .title("Backend Engineer")
                .company("Stripe")
                .source("LINKEDIN")
                .build();

        String jsonResponse = mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long jobId = objectMapper.readTree(jsonResponse).get("data").get("id").asLong();

        mockMvc.perform(get("/jobs/" + jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(jobId))
                .andExpect(jsonPath("$.data.title").value("Backend Engineer"))
                .andExpect(jsonPath("$.data.company").value("Stripe"))
                .andExpect(jsonPath("$.data.source").value("LINKEDIN"));
    }

    @Test
    void getJobById_notFound_returnsNotFound() throws Exception {
        mockMvc.perform(get("/jobs/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Job not found with id: 999"));
    }

    @Test
    void getAllJobs_paginationAndFiltering_success() throws Exception {
        CreateJobRequest job1 = CreateJobRequest.builder()
                .title("Java Developer")
                .company("Google India")
                .source("LINKEDIN")
                .remoteType(RemoteType.REMOTE)
                .status(JobStatus.SAVED)
                .build();

        CreateJobRequest job2 = CreateJobRequest.builder()
                .title("Frontend Developer")
                .company("Google LLC")
                .source("INDEED")
                .remoteType(RemoteType.HYBRID)
                .status(JobStatus.APPLIED)
                .build();

        CreateJobRequest job3 = CreateJobRequest.builder()
                .title("Python Developer")
                .company("Meta")
                .source("COMPANY_WEBSITE")
                .remoteType(RemoteType.REMOTE)
                .status(JobStatus.SAVED)
                .build();

        mockMvc.perform(post("/jobs").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(job1)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/jobs").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(job2)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/jobs").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(job3)))
                .andExpect(status().isCreated());

        // Test combined filter: status=SAVED, company=google (case-insensitive partial match), remoteType=REMOTE
        mockMvc.perform(get("/jobs?status=SAVED&company=google&source=linkedin&remoteType=REMOTE&page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].company").value("Google India"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    void getAllJobs_emptyResult_returnsEmptyPagedResponse() throws Exception {
        mockMvc.perform(get("/jobs?company=NonExistentCompany"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)))
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0));
    }

    @Test
    void updateJob_success() throws Exception {
        CreateJobRequest createRequest = CreateJobRequest.builder()
                .title("DevOps Engineer")
                .company("Amazon")
                .build();

        String jsonResponse = mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long jobId = objectMapper.readTree(jsonResponse).get("data").get("id").asLong();

        UpdateJobRequest updateRequest = UpdateJobRequest.builder()
                .title("Senior DevOps Lead")
                .company("Amazon AWS")
                .location("Seattle, WA")
                .status(JobStatus.INTERVIEW)
                .remoteType(RemoteType.HYBRID)
                .salaryMin(new BigDecimal("120000.00"))
                .salaryMax(new BigDecimal("160000.00"))
                .build();

        mockMvc.perform(put("/jobs/" + jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(jobId))
                .andExpect(jsonPath("$.data.title").value("Senior DevOps Lead"))
                .andExpect(jsonPath("$.data.company").value("Amazon AWS"))
                .andExpect(jsonPath("$.data.status").value("INTERVIEW"));
    }

    @Test
    void updateJob_validationErrors_returnsBadRequest() throws Exception {
        CreateJobRequest createRequest = CreateJobRequest.builder()
                .title("DevOps Engineer")
                .company("Amazon")
                .build();

        String jsonResponse = mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long jobId = objectMapper.readTree(jsonResponse).get("data").get("id").asLong();

        // 1. Missing title and company
        UpdateJobRequest invalidUpdateRequest1 = UpdateJobRequest.builder()
                .title("")
                .company(" ")
                .build();

        mockMvc.perform(put("/jobs/" + jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUpdateRequest1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").value("Job title is required"))
                .andExpect(jsonPath("$.errors.company").value("Company name is required"));

        // 2. Negative salary
        UpdateJobRequest invalidUpdateRequest2 = UpdateJobRequest.builder()
                .title("DevOps Lead")
                .company("Amazon")
                .salaryMin(new BigDecimal("-1.00"))
                .build();

        mockMvc.perform(put("/jobs/" + jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUpdateRequest2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.salaryMin").value("Minimum salary cannot be negative"));

        // 3. salaryMin > salaryMax
        UpdateJobRequest invalidUpdateRequest3 = UpdateJobRequest.builder()
                .title("DevOps Lead")
                .company("Amazon")
                .salaryMin(new BigDecimal("200000.00"))
                .salaryMax(new BigDecimal("100000.00"))
                .build();

        mockMvc.perform(put("/jobs/" + jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUpdateRequest3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Minimum salary cannot be greater than maximum salary"));
    }

    @Test
    void updateJobStatus_success() throws Exception {
        CreateJobRequest createRequest = CreateJobRequest.builder()
                .title("QA Engineer")
                .company("Apple")
                .build();

        String jsonResponse = mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long jobId = objectMapper.readTree(jsonResponse).get("data").get("id").asLong();

        UpdateJobStatusRequest statusRequest = UpdateJobStatusRequest.builder()
                .status(JobStatus.OFFER)
                .build();

        mockMvc.perform(patch("/jobs/" + jobId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(jobId))
                .andExpect(jsonPath("$.data.status").value("OFFER"));
    }

    @Test
    void updateJobStatus_nonExistent_returnsNotFound() throws Exception {
        UpdateJobStatusRequest statusRequest = UpdateJobStatusRequest.builder()
                .status(JobStatus.OFFER)
                .build();

        mockMvc.perform(patch("/jobs/999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteJob_success() throws Exception {
        CreateJobRequest createRequest = CreateJobRequest.builder()
                .title("Data Scientist")
                .company("Netflix")
                .build();

        String jsonResponse = mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long jobId = objectMapper.readTree(jsonResponse).get("data").get("id").asLong();

        mockMvc.perform(delete("/jobs/" + jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Job deleted successfully"));

        mockMvc.perform(get("/jobs/" + jobId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteJob_notFound_returnsNotFound() throws Exception {
        mockMvc.perform(delete("/jobs/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

}
