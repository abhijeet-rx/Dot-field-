package com.dotfield.controller;

import com.dotfield.dto.EducationRequest;
import com.dotfield.dto.ExperienceRequest;
import com.dotfield.dto.ProjectRequest;
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

import java.time.LocalDate;
import java.util.List;

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
    private ExperienceRepository experienceRepository;

    @Autowired
    private EducationRepository educationRepository;

    @Autowired
    private ProjectRepository projectRepository;

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
    private Experience experienceB;
    private Education educationB;
    private Project projectB;
    private Job testJob;

    @BeforeEach
    void setUp() {
        skillRepository.deleteAll();
        experienceRepository.deleteAll();
        educationRepository.deleteAll();
        projectRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();
        jobRepository.deleteAll();

        // Create User A + Profile A
        userA = User.builder().email("usera@example.com").passwordHash("hash").role(Role.USER).build();
        userA = userRepository.save(userA);
        profileA = Profile.builder().user(userA).name("User A").email("usera@example.com").build();
        profileA.addSkill(Skill.builder().name("Java").category(SkillCategory.LANGUAGE).build());
        profileA.addExperience(Experience.builder().company("Company A").role("Dev A").build());
        profileA.addEducation(Education.builder().institution("Uni A").degree("BSc A").build());
        profileA.addProject(Project.builder().name("Project A").build());
        profileA = profileRepository.save(profileA);
        tokenA = jwtService.generateToken(userA.getId(), userA.getEmail(), userA.getRole());

        // Create User B + Profile B
        userB = User.builder().email("userb@example.com").passwordHash("hash").role(Role.USER).build();
        userB = userRepository.save(userB);
        profileB = Profile.builder().user(userB).name("User B").email("userb@example.com").build();
        skillB = Skill.builder().name("Python").category(SkillCategory.LANGUAGE).build();
        profileB.addSkill(skillB);
        experienceB = Experience.builder().company("Company B").role("Dev B").build();
        profileB.addExperience(experienceB);
        educationB = Education.builder().institution("Uni B").degree("MSc B").build();
        profileB.addEducation(educationB);
        projectB = Project.builder().name("Project B").build();
        profileB.addProject(projectB);
        profileB = profileRepository.save(profileB);
        tokenB = jwtService.generateToken(userB.getId(), userB.getEmail(), userB.getRole());

        // Create global job
        testJob = Job.builder().title("Java Developer").company("Tech Corp").status(JobStatus.SAVED).source("MANUAL").build();
        testJob = jobRepository.save(testJob);
    }

    // ─── Profile Isolation ────────────────────────────────────────

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

    // ─── Skill Isolation ──────────────────────────────────────────

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
    @DisplayName("DELETE /api/profile/skills/{id} — User A cannot delete User B's skill (returns 404)")
    void cannotMutateOtherUserSkill() throws Exception {
        Long userBSkillId = profileB.getSkills().get(0).getId();

        mockMvc.perform(delete("/profile/skills/" + userBSkillId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        // Verify Skill B still exists in database
        assertTrue(skillRepository.findById(userBSkillId).isPresent());
    }

    // ─── Experience Isolation ─────────────────────────────────────

    @Test
    @DisplayName("GET /api/profile/experience — User A only receives User A's experience")
    void experienceReadIsolationTest() throws Exception {
        mockMvc.perform(get("/profile/experience")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].company", is("Company A")));
    }

    @Test
    @DisplayName("PUT /api/profile/experience/{id} — User A cannot modify User B's experience (returns 404)")
    void cannotModifyOtherUserExperience() throws Exception {
        Long userBExpId = profileB.getExperience().get(0).getId();

        ExperienceRequest updateReq = ExperienceRequest.builder()
                .company("Hacked Company").role("Hacked Role").build();

        mockMvc.perform(put("/profile/experience/" + userBExpId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // Verify Experience B is unchanged
        Experience expB = experienceRepository.findById(userBExpId).orElseThrow();
        assertEquals("Company B", expB.getCompany());
    }

    @Test
    @DisplayName("DELETE /api/profile/experience/{id} — User A cannot delete User B's experience (returns 404)")
    void cannotDeleteOtherUserExperience() throws Exception {
        Long userBExpId = profileB.getExperience().get(0).getId();

        mockMvc.perform(delete("/profile/experience/" + userBExpId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        assertTrue(experienceRepository.findById(userBExpId).isPresent());
    }

    // ─── Education Isolation ──────────────────────────────────────

    @Test
    @DisplayName("GET /api/profile/education — User A only receives User A's education")
    void educationReadIsolationTest() throws Exception {
        mockMvc.perform(get("/profile/education")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].institution", is("Uni A")));
    }

    @Test
    @DisplayName("PUT /api/profile/education/{id} — User A cannot modify User B's education (returns 404)")
    void cannotModifyOtherUserEducation() throws Exception {
        Long userBEduId = profileB.getEducation().get(0).getId();

        EducationRequest updateReq = EducationRequest.builder()
                .institution("Hacked Uni").degree("Hacked Degree").build();

        mockMvc.perform(put("/profile/education/" + userBEduId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        Education eduB = educationRepository.findById(userBEduId).orElseThrow();
        assertEquals("Uni B", eduB.getInstitution());
    }

    @Test
    @DisplayName("DELETE /api/profile/education/{id} — User A cannot delete User B's education (returns 404)")
    void cannotDeleteOtherUserEducation() throws Exception {
        Long userBEduId = profileB.getEducation().get(0).getId();

        mockMvc.perform(delete("/profile/education/" + userBEduId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        assertTrue(educationRepository.findById(userBEduId).isPresent());
    }

    // ─── Project Isolation ────────────────────────────────────────

    @Test
    @DisplayName("GET /api/profile/projects — User A only receives User A's projects")
    void projectReadIsolationTest() throws Exception {
        mockMvc.perform(get("/profile/projects")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("Project A")));
    }

    @Test
    @DisplayName("PUT /api/profile/projects/{id} — User A cannot modify User B's project (returns 404)")
    void cannotModifyOtherUserProject() throws Exception {
        Long userBProjId = profileB.getProjects().get(0).getId();

        ProjectRequest updateReq = ProjectRequest.builder()
                .name("Hacked Project").build();

        mockMvc.perform(put("/profile/projects/" + userBProjId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        Project projB = projectRepository.findById(userBProjId).orElseThrow();
        assertEquals("Project B", projB.getName());
    }

    @Test
    @DisplayName("DELETE /api/profile/projects/{id} — User A cannot delete User B's project (returns 404)")
    void cannotDeleteOtherUserProject() throws Exception {
        Long userBProjId = profileB.getProjects().get(0).getId();

        mockMvc.perform(delete("/profile/projects/" + userBProjId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        assertTrue(projectRepository.findById(userBProjId).isPresent());
    }

    // ─── Job Match & Tailoring Isolation ──────────────────────────

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
