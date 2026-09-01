package com.dotfield.controller;

import com.dotfield.dto.CreateJobRequest;
import com.dotfield.entity.Job;
import com.dotfield.entity.JobStatus;
import com.dotfield.entity.Role;
import com.dotfield.entity.User;
import com.dotfield.repository.JobRepository;
import com.dotfield.repository.UserRepository;
import com.dotfield.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RbacSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private String adminToken;
    private Job testJob;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();

        User user = userRepository.findByEmailIgnoreCase("user@example.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("user@example.com")
                        .passwordHash("hash")
                        .role(Role.USER)
                        .build()));
        userToken = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());

        User admin = userRepository.findByEmailIgnoreCase("admin@example.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("admin@example.com")
                        .passwordHash("hash")
                        .role(Role.ADMIN)
                        .build()));
        adminToken = jwtService.generateToken(admin.getId(), admin.getEmail(), admin.getRole());

        testJob = Job.builder()
                .title("Software Engineer")
                .company("Acme Corp")
                .status(JobStatus.SAVED)
                .source("MANUAL")
                .build();
        testJob = jobRepository.save(testJob);
    }

    @Test
    @DisplayName("USER role can read jobs (GET /jobs)")
    void userCanReadJobs() throws Exception {
        mockMvc.perform(get("/jobs")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("USER role gets 403 Forbidden on POST /jobs (Job Creation)")
    void userForbiddenOnJobCreation() throws Exception {
        CreateJobRequest request = CreateJobRequest.builder()
                .title("New Role")
                .company("Test Corp")
                .build();

        mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("USER role gets 403 Forbidden on POST /jobs/discover")
    void userForbiddenOnJobDiscovery() throws Exception {
        mockMvc.perform(post("/jobs/discover")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"COMPANY_WEBSITE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER role gets 403 Forbidden on POST /jobs/extract")
    void userForbiddenOnJobExtraction() throws Exception {
        mockMvc.perform(post("/jobs/extract")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobUrl\":\"https://example.com/jobs/1\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER role gets 403 Forbidden on PUT /jobs/{id}")
    void userForbiddenOnJobUpdate() throws Exception {
        mockMvc.perform(put("/jobs/" + testJob.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated Title\",\"company\":\"Updated Co\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER role gets 403 Forbidden on PATCH /jobs/{id}/status")
    void userForbiddenOnJobStatusPatch() throws Exception {
        mockMvc.perform(patch("/jobs/" + testJob.getId() + "/status")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPLIED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER role gets 403 Forbidden on DELETE /jobs/{id}")
    void userForbiddenOnJobDelete() throws Exception {
        mockMvc.perform(delete("/jobs/" + testJob.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN role can perform administrative job operations")
    void adminCanPerformAdminOperations() throws Exception {
        CreateJobRequest request = CreateJobRequest.builder()
                .title("Staff Engineer")
                .company("Global Corp")
                .build();

        mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/jobs/" + testJob.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated Title\",\"company\":\"Updated Co\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/jobs/" + testJob.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
