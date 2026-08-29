package com.dotfield.controller;

import com.dotfield.dto.ExperienceRequest;
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

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExperienceControllerTest {

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
    void createUpdateDeleteExperience_flowSuccess() throws Exception {
        ExperienceRequest createReq = ExperienceRequest.builder()
                .company("Acme Corp")
                .role("Software Engineer")
                .description("Built scalable Java web applications")
                .startDate(LocalDate.of(2022, 6, 1))
                .endDate(LocalDate.of(2024, 8, 15))
                .build();

        String response = mockMvc.perform(post("/profile/experience")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.company").value("Acme Corp"))
                .andExpect(jsonPath("$.data.role").value("Software Engineer"))
                .andReturn().getResponse().getContentAsString();

        Long expId = objectMapper.readTree(response).get("data").get("id").asLong();

        ExperienceRequest updateReq = ExperienceRequest.builder()
                .company("Acme Corp")
                .role("Senior Software Engineer")
                .description("Led backend architecture")
                .startDate(LocalDate.of(2022, 6, 1))
                .endDate(LocalDate.of(2024, 8, 15))
                .build();

        mockMvc.perform(put("/profile/experience/" + expId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("Senior Software Engineer"));

        mockMvc.perform(get("/profile/experience"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].role").value("Senior Software Engineer"));

        mockMvc.perform(delete("/profile/experience/" + expId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/profile/experience"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void addExperience_invalidDateRange_returnsBadRequest() throws Exception {
        ExperienceRequest invalidReq = ExperienceRequest.builder()
                .company("Tech Corp")
                .role("Developer")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2023, 1, 1))
                .build();

        mockMvc.perform(post("/profile/experience")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Start date cannot be after end date"));
    }

    @Test
    void createExperience_missingCompany_returnsBadRequest() throws Exception {
        ExperienceRequest invalidReq = ExperienceRequest.builder()
                .company("")
                .role("Developer")
                .build();

        mockMvc.perform(post("/profile/experience")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.company").value("Company is required"));
    }

    @Test
    void deleteExperience_nonExistent_returnsNotFound() throws Exception {
        mockMvc.perform(delete("/profile/experience/999"))
                .andExpect(status().isNotFound());
    }

}
