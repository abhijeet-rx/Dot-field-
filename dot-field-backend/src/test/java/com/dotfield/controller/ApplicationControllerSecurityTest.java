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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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

        // Candidate A creates application
        String responseStr = mockMvc.perform(post("/applications")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long appId = com.fasterxml.jackson.databind.ObjectMapper.class.getDeclaredConstructor().newInstance()
                .readTree(responseStr).get("data").get("id").asLong();

        // Candidate B attempts to read Candidate A's application -> 404 Not Found
        mockMvc.perform(get("/applications/" + appId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // Candidate B attempts to update Candidate A's status -> 404 Not Found
        mockMvc.perform(patch("/applications/" + appId + "/status")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPLIED\"}"))
                .andExpect(status().isNotFound());

        // Candidate B attempts to delete Candidate A's application -> 404 Not Found
        mockMvc.perform(delete("/applications/" + appId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }
}
