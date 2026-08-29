package com.dotfield.controller;

import com.dotfield.dto.ProjectRequest;
import com.dotfield.dto.UpdateProfileRequest;
import com.dotfield.repository.ProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProfileRepository profileRepository;

    @BeforeEach
    void setUp() throws Exception {
        profileRepository.deleteAll();

        UpdateProfileRequest profileRequest = UpdateProfileRequest.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .build();

        mockMvc.perform(put("/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profileRequest)));
    }

    @Test
    void createUpdateDeleteProject_flowSuccess() throws Exception {
        ProjectRequest createReq = ProjectRequest.builder()
                .name("DOT Field Backend")
                .description("Job discovery and resume tailoring backend")
                .githubUrl("https://github.com/example/dot-field")
                .liveUrl("https://dotfield.dev")
                .technologies(List.of("Java 21", "Spring Boot", "PostgreSQL"))
                .build();

        String response = mockMvc.perform(post("/profile/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("DOT Field Backend"))
                .andExpect(jsonPath("$.data.technologies", hasSize(3)))
                .andReturn().getResponse().getContentAsString();

        Long projectId = objectMapper.readTree(response).get("data").get("id").asLong();

        ProjectRequest updateReq = ProjectRequest.builder()
                .name("DOT Field Platform")
                .description("Updated description")
                .githubUrl("https://github.com/example/dot-field")
                .liveUrl("https://dotfield.dev")
                .technologies(List.of("Java 21", "Spring Boot", "PostgreSQL", "Docker"))
                .build();

        mockMvc.perform(put("/profile/projects/" + projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("DOT Field Platform"))
                .andExpect(jsonPath("$.data.technologies", hasSize(4)));

        mockMvc.perform(get("/profile/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        mockMvc.perform(delete("/profile/projects/" + projectId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/profile/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void createProject_missingName_returnsBadRequest() throws Exception {
        ProjectRequest invalidReq = ProjectRequest.builder()
                .name("")
                .description("No name project")
                .build();

        mockMvc.perform(post("/profile/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Project name is required"));
    }

    @Test
    void updateProject_nonExistent_returnsNotFound() throws Exception {
        ProjectRequest updateReq = ProjectRequest.builder()
                .name("Ghost Project")
                .build();

        mockMvc.perform(put("/profile/projects/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());
    }

}
