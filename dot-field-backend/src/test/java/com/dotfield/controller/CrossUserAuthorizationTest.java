package com.dotfield.controller;

import com.dotfield.dto.SkillRequest;
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
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CrossUserAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User userA;
    private Profile profileA;
    private String tokenA;

    private User userB;
    private Profile profileB;
    private String tokenB;

    private Skill skillB;
    private Job testJob;

    @BeforeEach
    void setUp() {
        skillRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();
        jobRepository.deleteAll();

        // Create User A + Profile A
        userA = User.builder().email("usera@example.com").passwordHash("hash").role(Role.USER).build();
        userA = userRepository.save(userA);
        profileA = Profile.builder().user(userA).name("User A").email("usera@example.com").build();
        profileA.addSkill(Skill.builder().name("Java").category(SkillCategory.LANGUAGE).build());
        profileA = profileRepository.save(profileA);
        tokenA = jwtService.generateToken(userA.getId(), userA.getEmail(), userA.getRole());

        // Create User B + Profile B
        userB = User.builder().email("userb@example.com").passwordHash("hash").role(Role.USER).build();
        userB = userRepository.save(userB);
        profileB = Profile.builder().user(userB).name("User B").email("userb@example.com").build();
        skillB = Skill.builder().name("Python").category(SkillCategory.LANGUAGE).build();
        profileB.addSkill(skillB);
        profileB = profileRepository.save(profileB);
        tokenB = jwtService.generateToken(userB.getId(), userB.getEmail(), userB.getRole());

        // Create global job
        testJob = Job.builder().title("Java Developer").company("Tech Corp").status(JobStatus.SAVED).source("MANUAL").build();
        testJob = jobRepository.save(testJob);
    }

    @Test
    @DisplayName("GET /api/profile — User A receives Profile A, User B receives Profile B")
    void profileIsolationTest() throws Exception {
        mockMvc.perform(get("/profile")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("User A")))
                .andExpect(jsonPath("$.data.email", is("usera@example.com")));

        mockMvc.perform(get("/profile")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("User B")))
                .andExpect(jsonPath("$.data.email", is("userb@example.com")));
    }

    @Test
    @DisplayName("GET /api/profile/skills — User A only receives User A's skills, not User B's skills")
    void skillIsolationTest() throws Exception {
        mockMvc.perform(get("/profile/skills")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("Java")));
    }

    @Test
    @DisplayName("DELETE /api/profile/skills/{id} — User A cannot delete User B's skill (returns 404/403)")
    void cannotMutateOtherUserSkill() throws Exception {
        Long userBSkillId = profileB.getSkills().get(0).getId();

        mockMvc.perform(delete("/profile/skills/" + userBSkillId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        // Verify Skill B still exists in database
        assertTrue(skillRepository.findById(userBSkillId).isPresent());
    }

    @Test
    @DisplayName("GET /api/jobs/{id}/match — User A match analysis is calculated strictly against Profile A")
    void matchIsolationTest() throws Exception {
        mockMvc.perform(get("/jobs/" + testJob.getId() + "/match")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileId", is(profileA.getId().intValue())));
    }

    @Test
    @DisplayName("GET /api/jobs/{id}/resume/tailor — User A resume tailoring uses Profile A, never Profile B")
    void tailoringIsolationTest() throws Exception {
        mockMvc.perform(get("/jobs/" + testJob.getId() + "/resume/tailor")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileId", is(profileA.getId().intValue())));
    }
}
