package com.dotfield.controller;

import com.dotfield.dto.UpdateProfileRequest;
import com.dotfield.entity.Profile;
import com.dotfield.entity.Role;
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

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProfileControllerTest {

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

    @BeforeEach
    void setUp() {
        profileRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .email("testuser@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();
        testUser = userRepository.save(testUser);
        authToken = jwtService.generateToken(testUser.getId(), testUser.getEmail(), testUser.getRole());
    }

    @Test
    void getProfile_whenEmpty_returnsNotFound() throws Exception {
        mockMvc.perform(get("/profile")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Candidate profile not found for current user"));
    }

    @Test
    void putProfile_createsOrUpdatesProfile() throws Exception {
        // Create initial profile linked to user
        Profile profile = Profile.builder().user(testUser).name("Initial").email(testUser.getEmail()).build();
        profileRepository.save(profile);

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .phone("+1234567890")
                .location("San Francisco, CA")
                .linkedinUrl("https://linkedin.com/in/johndoe")
                .githubUrl("https://github.com/johndoe")
                .portfolioUrl("https://johndoe.dev")
                .build();

        mockMvc.perform(put("/profile")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("John Doe"))
                .andExpect(jsonPath("$.data.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.data.skills", is(empty())))
                .andExpect(jsonPath("$.data.education", is(empty())))
                .andExpect(jsonPath("$.data.projects", is(empty())))
                .andExpect(jsonPath("$.data.experience", is(empty())))
                .andExpect(jsonPath("$.message").value("Profile updated successfully"));

        mockMvc.perform(get("/profile")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("John Doe"))
                .andExpect(jsonPath("$.data.email").value("john.doe@example.com"));
    }

    @Test
    void putProfile_withMissingRequiredName_returnsBadRequest() throws Exception {
        Profile profile = Profile.builder().user(testUser).name("Initial").email(testUser.getEmail()).build();
        profileRepository.save(profile);

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("")
                .email("john@example.com")
                .build();

        mockMvc.perform(put("/profile")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.name").value("Name is required"));
    }

    @Test
    void putProfile_withInvalidEmail_returnsBadRequest() throws Exception {
        Profile profile = Profile.builder().user(testUser).name("Initial").email(testUser.getEmail()).build();
        profileRepository.save(profile);

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("John Doe")
                .email("invalid-email")
                .build();

        mockMvc.perform(put("/profile")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.email").value("Email must be valid"));
    }

}
