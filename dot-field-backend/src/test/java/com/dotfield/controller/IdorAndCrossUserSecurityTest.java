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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IdorAndCrossUserSecurityTest {

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

    private Skill skillA;
    private Experience experienceA;
    private Education educationA;
    private Project projectA;
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
        skillA = Skill.builder().name("Java").category(SkillCategory.LANGUAGE).build();
        profileA.addSkill(skillA);
        experienceA = Experience.builder().company("Company A").role("Dev A").build();
        profileA.addExperience(experienceA);
        educationA = Education.builder().institution("Uni A").degree("BSc A").build();
        profileA.addEducation(educationA);
        projectA = Project.builder().name("Project A").build();
        profileA.addProject(projectA);
        profileA = profileRepository.save(profileA);
        tokenA = jwtService.generateToken(userA.getId(), userA.getEmail(), userA.getRole());

        // Create User B + Profile B
        userB = User.builder().email("userb@example.com").passwordHash("hash").role(Role.USER).build();
        userB = userRepository.save(userB);
        profileB = Profile.builder().user(userB).name("User B").email("userb@example.com").build();
        Skill skillB = Skill.builder().name("Python").category(SkillCategory.LANGUAGE).build();
        profileB.addSkill(skillB);
        profileB = profileRepository.save(profileB);
        tokenB = jwtService.generateToken(userB.getId(), userB.getEmail(), userB.getRole());

        // Create global job
        testJob = Job.builder().title("Software Architect").company("Tech Corp").status(JobStatus.SAVED).source("MANUAL").build();
        testJob = jobRepository.save(testJob);
    }

    @Test
    @DisplayName("GET /profile — Authenticated user receives only their own candidate profile")
    void profileReadIsolation() throws Exception {
        mockMvc.perform(get("/profile")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("User B")))
                .andExpect(jsonPath("$.data.email", is("userb@example.com")));
    }

    @Test
    @DisplayName("DELETE /profile/skills/{id} — User B cannot delete User A's skill (IDOR attempt returns 404)")
    void idorSkillDeletePrevented() throws Exception {
        Long userASkillId = profileA.getSkills().get(0).getId();

        mockMvc.perform(delete("/profile/skills/" + userASkillId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        assertTrue(skillRepository.findById(userASkillId).isPresent());
    }

    @Test
    @DisplayName("PUT /profile/experience/{id} — User B cannot modify User A's experience (IDOR attempt returns 404)")
    void idorExperienceUpdatePrevented() throws Exception {
        Long userAExpId = profileA.getExperience().get(0).getId();

        ExperienceRequest updateReq = ExperienceRequest.builder()
                .company("Hacked Corp")
                .role("Hacked Role")
                .build();

        mockMvc.perform(put("/profile/experience/" + userAExpId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        Experience expA = experienceRepository.findById(userAExpId).orElseThrow();
        assertEquals("Company A", expA.getCompany());
    }

    @Test
    @DisplayName("DELETE /profile/experience/{id} — User B cannot delete User A's experience (IDOR attempt returns 404)")
    void idorExperienceDeletePrevented() throws Exception {
        Long userAExpId = profileA.getExperience().get(0).getId();

        mockMvc.perform(delete("/profile/experience/" + userAExpId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        assertTrue(experienceRepository.findById(userAExpId).isPresent());
    }

    @Test
    @DisplayName("PUT /profile/education/{id} — User B cannot modify User A's education (IDOR attempt returns 404)")
    void idorEducationUpdatePrevented() throws Exception {
        Long userAEduId = profileA.getEducation().get(0).getId();

        EducationRequest updateReq = EducationRequest.builder()
                .institution("Hacked Uni")
                .degree("Hacked Degree")
                .build();

        mockMvc.perform(put("/profile/education/" + userAEduId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        Education eduA = educationRepository.findById(userAEduId).orElseThrow();
        assertEquals("Uni A", eduA.getInstitution());
    }

    @Test
    @DisplayName("DELETE /profile/education/{id} — User B cannot delete User A's education (IDOR attempt returns 404)")
    void idorEducationDeletePrevented() throws Exception {
        Long userAEduId = profileA.getEducation().get(0).getId();

        mockMvc.perform(delete("/profile/education/" + userAEduId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        assertTrue(educationRepository.findById(userAEduId).isPresent());
    }

    @Test
    @DisplayName("PUT /profile/projects/{id} — User B cannot modify User A's project (IDOR attempt returns 404)")
    void idorProjectUpdatePrevented() throws Exception {
        Long userAProjId = profileA.getProjects().get(0).getId();

        ProjectRequest updateReq = ProjectRequest.builder()
                .name("Hacked Project")
                .build();

        mockMvc.perform(put("/profile/projects/" + userAProjId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        Project projA = projectRepository.findById(userAProjId).orElseThrow();
        assertEquals("Project A", projA.getName());
    }

    @Test
    @DisplayName("DELETE /profile/projects/{id} — User B cannot delete User A's project (IDOR attempt returns 404)")
    void idorProjectDeletePrevented() throws Exception {
        Long userAProjId = profileA.getProjects().get(0).getId();

        mockMvc.perform(delete("/profile/projects/" + userAProjId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        assertTrue(projectRepository.findById(userAProjId).isPresent());
    }

    @Test
    @DisplayName("Random non-existent ID manipulation (e.g., 99999) returns 404 Not Found")
    void nonExistentIdManipulationReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/profile/skills/99999")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/profile/experience/99999")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/profile/education/99999")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/profile/projects/99999")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /jobs/{id}/match and GET /jobs/{id}/resume/tailor use authenticated user profile strictly")
    void matchAndTailoringIsolation() throws Exception {
        mockMvc.perform(get("/jobs/" + testJob.getId() + "/match")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileId", is(profileB.getId().intValue())));

        mockMvc.perform(get("/jobs/" + testJob.getId() + "/resume/tailor")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileId", is(profileB.getId().intValue())));
    }
}
