package com.dotfield.controller;

import com.dotfield.dto.SkillRequest;
import com.dotfield.entity.Profile;
import com.dotfield.entity.Role;
import com.dotfield.entity.SkillCategory;
import com.dotfield.entity.User;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String authToken;
    private User testUser;
    private Profile testProfile;

    @BeforeEach
    void setUp() {
        profileRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .email("jane@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();
        testUser = userRepository.save(testUser);

        testProfile = Profile.builder()
                .user(testUser)
                .name("Jane Doe")
                .email("jane@example.com")
                .build();
        testProfile = profileRepository.save(testProfile);

        authToken = jwtService.generateToken(testUser.getId(), testUser.getEmail(), testUser.getRole());
    }

    @Test
    void addAndGetSkills_success() throws Exception {
        SkillRequest javaSkill = SkillRequest.builder()
                .name("Java")
                .category(SkillCategory.LANGUAGE)
                .build();

        mockMvc.perform(post("/profile/skills")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(javaSkill)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Java"))
                .andExpect(jsonPath("$.data.category").value("LANGUAGE"));

        mockMvc.perform(get("/profile/skills")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Java"));
    }

    @Test
    void addSkill_duplicate_returnsBadRequest() throws Exception {
        SkillRequest javaSkill = SkillRequest.builder()
                .name("Java")
                .category(SkillCategory.LANGUAGE)
                .build();

        mockMvc.perform(post("/profile/skills")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(javaSkill)))
                .andExpect(status().isCreated());

        SkillRequest duplicateJava = SkillRequest.builder()
                .name("java")
                .category(SkillCategory.LANGUAGE)
                .build();

        mockMvc.perform(post("/profile/skills")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateJava)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Skill 'java' already exists"));
    }

    @Test
    void addSkill_missingName_returnsBadRequest() throws Exception {
        SkillRequest invalidSkill = SkillRequest.builder()
                .name("")
                .category(SkillCategory.BACKEND)
                .build();

        mockMvc.perform(post("/profile/skills")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidSkill)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Skill name is required"));
    }

    @Test
    void deleteSkill_success() throws Exception {
        SkillRequest skill = SkillRequest.builder()
                .name("PostgreSQL")
                .category(SkillCategory.DATABASE)
                .build();

        String response = mockMvc.perform(post("/profile/skills")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(skill)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long skillId = objectMapper.readTree(response).get("data").get("id").asLong();

        mockMvc.perform(delete("/profile/skills/" + skillId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Skill deleted successfully"));

        mockMvc.perform(get("/profile/skills")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void deleteSkill_nonExistent_returnsNotFound() throws Exception {
        mockMvc.perform(delete("/profile/skills/999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

}
