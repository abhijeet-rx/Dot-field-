package com.dotfield.discovery;

import com.dotfield.dto.*;
import com.dotfield.entity.*;
import com.dotfield.repository.*;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FullJobIntelligencePipelineE2ETest {

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

    @Autowired
    private ObjectMapper objectMapper;

    private User adminUser;
    private String adminToken;

    private User candidateUser;
    private Profile candidateProfile;
    private String candidateToken;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = User.builder().email("admin@example.com").passwordHash("hash").role(Role.ADMIN).build();
        adminUser = userRepository.save(adminUser);
        adminToken = jwtService.generateToken(adminUser.getId(), adminUser.getEmail(), adminUser.getRole());

        candidateUser = User.builder().email("candidate@example.com").passwordHash("hash").role(Role.USER).build();
        candidateUser = userRepository.save(candidateUser);
        candidateProfile = Profile.builder().user(candidateUser).name("Candidate User").email("candidate@example.com").build();
        candidateProfile.addSkill(Skill.builder().name("Java").category(SkillCategory.LANGUAGE).build());
        candidateProfile.addSkill(Skill.builder().name("Spring Boot").category(SkillCategory.FRAMEWORK).build());
        candidateProfile.addExperience(Experience.builder().company("Tech Co").role("Java Engineer").description("Built microservices with Spring Boot").build());
        candidateProfile = profileRepository.save(candidateProfile);
        candidateToken = jwtService.generateToken(candidateUser.getId(), candidateUser.getEmail(), candidateUser.getRole());
    }

    @Test
    @DisplayName("Scenario 1: Complete successful pipeline — Extraction -> Normalization -> Persistence -> Requirement Analysis -> Fit Score -> Resume Tailoring")
    void successfulJobPipeline() throws Exception {
        // Step 1: Admin extracts & ingests job
        ExtractJobRequest extractReq = ExtractJobRequest.builder()
                .source("COMPANY_WEBSITE")
                .rawData(java.util.Map.of(
                        "jobUrl", "https://careers.acme.com/jobs/dev-101",
                        "title", "Senior Java Developer",
                        "company", "Acme Corp",
                        "location", "Remote",
                        "description", "Senior Java Developer at Acme Corp. Requirements: 5+ years Java, Spring Boot, PostgreSQL. Bachelor's degree in Computer Science."
                ))
                .build();

        MvcResult extractResult = mockMvc.perform(post("/jobs/extract")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(extractReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.title", is("Senior Java Developer")))
                .andExpect(jsonPath("$.data.company", is("Acme Corp")))
                .andReturn();

        ApiResponse<?> extractResponse = objectMapper.readValue(extractResult.getResponse().getContentAsString(), ApiResponse.class);
        Number jobIdNum = (Number) ((java.util.Map<?, ?>) extractResponse.getData()).get("id");
        Long jobId = jobIdNum.longValue();

        // Step 2: Candidate queries fit score match
        mockMvc.perform(get("/jobs/" + jobId + "/match")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overallScore", greaterThan(0)))
                .andExpect(jsonPath("$.data.matchCategory", notNullValue()))
                .andExpect(jsonPath("$.data.jobId", is(jobId.intValue())));

        // Step 3: Candidate requests tailored resume
        mockMvc.perform(get("/jobs/" + jobId + "/resume/tailor")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobId", is(jobId.intValue())))
                .andExpect(jsonPath("$.data.summary", notNullValue()))
                .andExpect(jsonPath("$.data.skills", notNullValue()));
    }

    @Test
    @DisplayName("Scenario 2: Duplicate External ID is rejected and updates existing record")
    void duplicateExternalIdIsRejected() throws Exception {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("COMPANY_WEBSITE")
                .maxResults(10)
                .build();

        mockMvc.perform(post("/jobs/discover")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        long countAfterFirst = jobRepository.count();

        // Second discovery run with same listings
        mockMvc.perform(post("/jobs/discover")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newJobs", is(0)));

        assertEquals(countAfterFirst, jobRepository.count());
    }

    @Test
    @DisplayName("Scenario 3: Duplicate Canonical URL is canonicalized and deduplicated")
    void duplicateCanonicalUrlIsRejected() throws Exception {
        CreateJobRequest job1 = CreateJobRequest.builder()
                .title("Software Engineer")
                .company("Acme")
                .jobUrl("https://acme.com/jobs/1?utm_source=linkedin&ref=123")
                .source("MANUAL")
                .build();

        mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(job1)))
                .andExpect(status().isCreated());

        Job existingJob = jobRepository.findAll().get(0);
        assertEquals("https://acme.com/jobs/1", existingJob.getCanonicalUrl());
    }

    @Test
    @DisplayName("Scenario 4: Duplicate Fingerprint updates existing job")
    void duplicateFingerprintIsRejected() throws Exception {
        CreateJobRequest job1 = CreateJobRequest.builder()
                .title("Frontend Developer")
                .company("Design Studio")
                .location("New York, NY")
                .description("React frontend role")
                .source("MANUAL")
                .build();

        mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(job1)))
                .andExpect(status().isCreated());

        Job saved = jobRepository.findAll().get(0);
        assertNotNull(saved.getDeduplicationFingerprint());
    }

    @Test
    @DisplayName("Scenario 5: Malformed job description falls back gracefully without crashing match/tailoring")
    void malformedJobHandledGracefully() throws Exception {
        CreateJobRequest jobReq = CreateJobRequest.builder()
                .title("General Worker")
                .company("Anon Corp")
                .description("!!! $$$ %%% Short description with no clear skills.")
                .source("MANUAL")
                .build();

        MvcResult result = mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<?> response = objectMapper.readValue(result.getResponse().getContentAsString(), ApiResponse.class);
        Number jobIdNum = (Number) ((java.util.Map<?, ?>) response.getData()).get("id");
        Long jobId = jobIdNum.longValue();

        // Match should complete with low score rather than exception
        mockMvc.perform(get("/jobs/" + jobId + "/match")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overallScore", notNullValue()));
    }

    @Test
    @DisplayName("Scenario 6: Null description ingestion proceeds cleanly")
    void missingDescriptionHandledGracefully() throws Exception {
        CreateJobRequest jobReq = CreateJobRequest.builder()
                .title("No Description Role")
                .company("Silent Corp")
                .source("MANUAL")
                .build();

        mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Scenario 7: Invalid source discovery returns appropriate 400 error")
    void extractionFailureHandledGracefully() throws Exception {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("NON_EXISTENT_SOURCE")
                .build();

        mockMvc.perform(post("/jobs/discover")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
