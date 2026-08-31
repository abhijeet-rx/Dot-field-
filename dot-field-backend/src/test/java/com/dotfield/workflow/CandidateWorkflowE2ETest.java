package com.dotfield.workflow;

import com.dotfield.entity.Job;
import com.dotfield.entity.JobStatus;
import com.dotfield.entity.Role;
import com.dotfield.entity.User;
import com.dotfield.repository.JobRepository;
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
class CandidateWorkflowE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JwtService jwtService;

    private Job sampleJob;

    @BeforeEach
    void setUp() {
        sampleJob = jobRepository.save(Job.builder()
                .title("Full Stack Engineer")
                .company("Acme Corp")
                .location("Remote")
                .source("LINKEDIN")
                .status(JobStatus.SAVED)
                .build());
    }

    @Test
    @DisplayName("Full Candidate Workflow: Register -> Login -> Onboarding -> Fit Analysis -> Tailor Resume -> Application Tracking -> Analytics")
    void fullCandidateWorkflowE2E() throws Exception {
        // 1. Candidate Registration
        String regBody = "{\"email\":\"workflow.candidate@example.com\",\"password\":\"Pass123!\",\"name\":\"Workflow Candidate\"}";
        String regRes = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(regBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        String token = com.fasterxml.jackson.databind.ObjectMapper.class.getDeclaredConstructor().newInstance()
                .readTree(regRes).get("data").get("token").asText();

        // 2. Candidate Onboarding — Check Profile Completeness
        mockMvc.perform(get("/profile/completeness")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score", greaterThanOrEqualTo(0)));

        // 3. Discover Jobs
        mockMvc.perform(get("/jobs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", not(empty())));

        // 4. Fit Match Analysis
        mockMvc.perform(get("/jobs/" + sampleJob.getId() + "/match")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overallScore", notNullValue()));

        // 5. Resume Tailoring
        mockMvc.perform(get("/jobs/" + sampleJob.getId() + "/resume/tailor")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()));

        // 6. Track Application
        String appBody = "{\"jobId\":" + sampleJob.getId() + ",\"status\":\"SAVED\",\"notes\":\"Preparing application\"}";
        String appRes = mockMvc.perform(post("/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.status", is("SAVED")))
                .andReturn().getResponse().getContentAsString();

        Long appId = com.fasterxml.jackson.databind.ObjectMapper.class.getDeclaredConstructor().newInstance()
                .readTree(appRes).get("data").get("id").asLong();

        // 7. Update Status to APPLIED
        mockMvc.perform(patch("/applications/" + appId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPLIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("APPLIED")))
                .andExpect(jsonPath("$.data.appliedAt", notNullValue()));

        // 8. Update Application Notes
        mockMvc.perform(put("/applications/" + appId + "/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Submitted via portal\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notes", is("Submitted via portal")));

        // 9. Fetch Application Analytics
        mockMvc.perform(get("/applications/analytics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalApplications", is(1)))
                .andExpect(jsonPath("$.data.statusCounts.APPLIED", is(1)));
    }
}
