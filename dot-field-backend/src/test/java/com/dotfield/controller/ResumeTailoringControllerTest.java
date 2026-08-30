package com.dotfield.controller;

import com.dotfield.dto.CreateJobRequest;
import com.dotfield.dto.SkillRequest;
import com.dotfield.dto.UpdateProfileRequest;
import com.dotfield.entity.Job;
import com.dotfield.entity.SkillCategory;
import com.dotfield.repository.JobRepository;
import com.dotfield.repository.ProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResumeTailoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @BeforeEach
    void setUp() throws Exception {
        jobRepository.deleteAll();
        profileRepository.deleteAll();

        // Setup candidate profile
        UpdateProfileRequest profileRequest = UpdateProfileRequest.builder()
                .name("Alex Smith")
                .email("alex@example.com")
                .linkedinUrl("https://linkedin.com/in/alexsmith")
                .build();

        mockMvc.perform(put("/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profileRequest)));

        SkillRequest skill = SkillRequest.builder()
                .name("Java")
                .category(SkillCategory.LANGUAGE)
                .build();

        mockMvc.perform(post("/profile/skills")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(skill)));
    }

    @Test
    void tailorResume_success() throws Exception {
        CreateJobRequest createJobRequest = CreateJobRequest.builder()
                .title("Java Developer")
                .company("Acme Corp")
                .description("Requires Java and Spring Boot experience")
                .build();

        String jobResponseJson = mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createJobRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long jobId = objectMapper.readTree(jobResponseJson).get("data").get("id").asLong();

        mockMvc.perform(get("/jobs/" + jobId + "/resume/tailor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Resume tailored successfully"))
                .andExpect(jsonPath("$.data.jobId").value(jobId))
                .andExpect(jsonPath("$.data.summary", notNullValue()))
                .andExpect(jsonPath("$.data.skills.primary[0]").value("Java"))
                .andExpect(jsonPath("$.data.links[0].type").value("LinkedIn"))
                .andExpect(jsonPath("$.data.tailoringAnalysis", notNullValue()));
    }

    @Test
    void tailorResume_jobNotFound_returnsNotFound() throws Exception {
        mockMvc.perform(get("/jobs/999/resume/tailor"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Job not found with id: 999"));
    }

}
