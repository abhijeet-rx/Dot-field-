package com.dotfield.controller;

import com.dotfield.dto.CreateJobRequest;
import com.dotfield.dto.ExperienceRequest;
import com.dotfield.dto.SkillRequest;
import com.dotfield.dto.UpdateProfileRequest;
import com.dotfield.entity.Profile;
import com.dotfield.entity.Role;
import com.dotfield.entity.SkillCategory;
import com.dotfield.entity.User;
import com.dotfield.repository.JobRepository;
import com.dotfield.repository.ProfileRepository;
import com.dotfield.repository.UserRepository;
import com.dotfield.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ResumeTailoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private User testUser;
    private Profile testProfile;
    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        jobRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .email("alex@example.com")
                .passwordHash("hash")
                .role(Role.ADMIN) // Admin so POST /jobs works
                .build();
        testUser = userRepository.save(testUser);

        testProfile = Profile.builder()
                .user(testUser)
                .name("Alex Smith")
                .email("alex@example.com")
                .linkedinUrl("https://linkedin.com/in/alexsmith")
                .build();
        testProfile = profileRepository.save(testProfile);

        authToken = jwtService.generateToken(testUser.getId(), testUser.getEmail(), testUser.getRole());

        SkillRequest skill = SkillRequest.builder()
                .name("Java")
                .category(SkillCategory.LANGUAGE)
                .build();

        mockMvc.perform(post("/profile/skills")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(skill)));

        ExperienceRequest exp = ExperienceRequest.builder()
                .company("Acme Inc")
                .role("Software Engineer")
                .description("Developed Java applications.")
                .build();

        mockMvc.perform(post("/profile/experience")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(exp)));
    }

    @Test
    void tailorResume_success() throws Exception {
        CreateJobRequest createJobRequest = CreateJobRequest.builder()
                .title("Java Developer")
                .company("Acme Corp")
                .description("Requires Java and Spring Boot experience")
                .build();

        String jobResponseJson = mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createJobRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long jobId = objectMapper.readTree(jobResponseJson).get("data").get("id").asLong();

        mockMvc.perform(get("/jobs/" + jobId + "/resume/tailor")
                        .header("Authorization", "Bearer " + authToken))
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
        mockMvc.perform(get("/jobs/999/resume/tailor")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Job not found with id: 999"));
    }

}
