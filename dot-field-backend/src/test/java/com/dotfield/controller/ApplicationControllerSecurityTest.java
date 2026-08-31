package com.dotfield.controller;

import com.dotfield.entity.Job;
import com.dotfield.entity.JobStatus;
import com.dotfield.entity.Profile;
import com.dotfield.entity.Role;
import com.dotfield.entity.User;
import com.dotfield.repository.JobRepository;
import com.dotfield.repository.ProfileRepository;
import com.dotfield.repository.UserRepository;
import com.dotfield.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApplicationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JwtService jwtService;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;
    private Job sampleJob;

    @BeforeEach
    void setUp() {
        userA = userRepository.save(User.builder().email("candidatea@example.com").passwordHash("hash").role(Role.USER).build());
        Profile profileA = profileRepository.save(Profile.builder().name("Candidate A").email("candidatea@example.com").user(userA).build());
        tokenA = jwtService.generateToken(userA.getId(), userA.getEmail(), userA.getRole());

        userB = userRepository.save(User.builder().email("candidateb@example.com").passwordHash("hash").role(Role.USER).build());
        Profile profileB = profileRepository.save(Profile.builder().name("Candidate B").email("candidateb@example.com").user(userB).build());
        tokenB = jwtService.generateToken(userB.getId(), userB.getEmail(), userB.getRole());

        sampleJob = jobRepository.save(Job.builder()
                .title("Software Engineer")
                .company("Google")
                .source("LINKEDIN")
                .status(JobStatus.SAVED)
                .build());
    }

    @Test
    @DisplayName("Unauthenticated request to /applications returns 401 Unauthorized")
    void unauthenticatedAccess_returns401() throws Exception {
        mockMvc.perform(get("/applications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authenticated USER can create and retrieve application")
    void createAndGetApplication_success() throws Exception {
        String body = "{\"jobId\":" + sampleJob.getId() + ",\"status\":\"SAVED\",\"notes\":\"Interested\"}";

        mockMvc.perform(post("/applications")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.status", is("SAVED")));
    }

    @Test
    @DisplayName("Duplicate application tracking returns 409 Conflict")
    void duplicateApplication_returns409() throws Exception {
        String body = "{\"jobId\":" + sampleJob.getId() + ",\"status\":\"SAVED\"}";

        // First attempt succeeds
        mockMvc.perform(post("/applications")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Second attempt returns 409 Conflict
        mockMvc.perform(post("/applications")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("IDOR Protection: Candidate B requesting Candidate A's application receives 404 Not Found")
    void idorAccess_returns404Masked() throws Exception {
        String body = "{\"jobId\":" + sampleJob.getId() + ",\"status\":\"SAVED\"}";

        String responseStr = mockMvc.perform(post("/applications")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long appId = com.fasterxml.jackson.databind.ObjectMapper.class.getDeclaredConstructor().newInstance()
                .readTree(responseStr).get("data").get("id").asLong();

        // Candidate B attempts GET -> 404
        mockMvc.perform(get("/applications/" + appId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // Candidate B attempts PATCH status -> 404
        mockMvc.perform(patch("/applications/" + appId + "/status")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPLIED\"}"))
                .andExpect(status().isNotFound());

        // Candidate B attempts DELETE -> 404
        mockMvc.perform(delete("/applications/" + appId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Cross-User Isolation: Candidate B listing applications receives empty list, not Candidate A's application")
    void listApplications_isolatedPerCandidate() throws Exception {
        String body = "{\"jobId\":" + sampleJob.getId() + ",\"status\":\"SAVED\"}";

        // Candidate A creates application
        mockMvc.perform(post("/applications")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Candidate A lists -> contains 1 application
        mockMvc.perform(get("/applications")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(1)));

        // Candidate B lists -> contains 0 applications
        mockMvc.perform(get("/applications")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(0)));
    }

    @Test
    @DisplayName("Sort parameter whitelist validation returns 400 for invalid sort field")
    void getApplications_invalidSortField_returns400() throws Exception {
        mockMvc.perform(get("/applications?sortBy=maliciousColumn")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid sort field")));
    }
}
