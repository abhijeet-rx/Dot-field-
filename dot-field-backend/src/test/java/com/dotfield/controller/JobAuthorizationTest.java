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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JobAuthorizationTest {

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
        userRepository.deleteAll();

        User user = User.builder()
                .email("regularuser@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();
        user = userRepository.save(user);
        userToken = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());

        User admin = User.builder()
                .email("adminuser@example.com")
                .passwordHash("hash")
                .role(Role.ADMIN)
                .build();
        admin = userRepository.save(admin);
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
    @DisplayName("USER role can read jobs (GET /api/jobs)")
    void userCanReadJobs() throws Exception {
        mockMvc.perform(get("/jobs")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("USER role gets 403 Forbidden on POST /api/jobs (Job Creation)")
    void userForbiddenOnJobCreation() throws Exception {
        CreateJobRequest request = CreateJobRequest.builder()
                .title("New Engineering Role")
                .company("Test Corp")
                .build();

        mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER role gets 403 Forbidden on POST /api/jobs/discover")
    void userForbiddenOnJobDiscovery() throws Exception {
        mockMvc.perform(post("/jobs/discover")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"COMPANY_WEBSITE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN role can trigger job discovery (POST /api/jobs/discover)")
    void adminCanTriggerJobDiscovery() throws Exception {
        mockMvc.perform(post("/jobs/discover")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"COMPANY_WEBSITE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN role can create jobs (POST /api/jobs)")
    void adminCanCreateJob() throws Exception {
        CreateJobRequest request = CreateJobRequest.builder()
                .title("Staff Engineer")
                .company("Global Corp")
                .build();

        mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
