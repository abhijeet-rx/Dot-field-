package com.dotfield.controller;

import com.dotfield.entity.*;
import com.dotfield.repository.JobRepository;
import com.dotfield.repository.ProfileRepository;
import com.dotfield.repository.UserRepository;
import com.dotfield.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JobMatchingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private User testUser;
    private String authToken;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .email("alex@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();
        testUser = userRepository.save(testUser);
        authToken = jwtService.generateToken(testUser.getId(), testUser.getEmail(), testUser.getRole());
    }

    @Test
    void getJobMatch_success() throws Exception {
        Profile profile = Profile.builder()
                .user(testUser)
                .name("Alex Developer")
                .email("alex@example.com")
                .location("Bangalore, India")
                .build();
        profileRepository.save(profile);

        Job job = Job.builder()
                .title("Senior Backend Engineer")
                .company("Amazon")
                .location("Bangalore, India")
                .remoteType(RemoteType.HYBRID)
                .status(JobStatus.SAVED)
                .source("COMPANY_WEBSITE")
                .description("Required skills: Java, Spring Boot, Docker. 4+ years of experience required.")
                .build();
        Job savedJob = jobRepository.save(job);

        mockMvc.perform(get("/jobs/{id}/match", savedJob.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobId").value(savedJob.getId()))
                .andExpect(jsonPath("$.data.profileId").value(profile.getId()))
                .andExpect(jsonPath("$.data.overallScore", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.matchCategory", notNullValue()))
                .andExpect(jsonPath("$.data.strengths", notNullValue()))
                .andExpect(jsonPath("$.data.gaps", notNullValue()));
    }

    @Test
    void getJobMatch_jobNotFound_returns404() throws Exception {
        Profile profile = Profile.builder()
                .user(testUser)
                .name("Alex Developer")
                .email("alex@example.com")
                .build();
        profileRepository.save(profile);

        mockMvc.perform(get("/jobs/{id}/match", 999L)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Job not found with id: 999"));
    }

    @Test
    void getJobMatch_profileNotFound_returns404() throws Exception {
        Job job = Job.builder()
                .title("Software Engineer")
                .company("Netflix")
                .source("MANUAL")
                .build();
        Job savedJob = jobRepository.save(job);

        mockMvc.perform(get("/jobs/{id}/match", savedJob.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Candidate profile not found for current user"));
    }
}
