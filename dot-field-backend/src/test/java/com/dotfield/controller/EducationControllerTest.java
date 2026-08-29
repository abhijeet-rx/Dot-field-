package com.dotfield.controller;

import com.dotfield.dto.EducationRequest;
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
class EducationControllerTest {

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
    void createUpdateDeleteEducation_flowSuccess() throws Exception {
        EducationRequest createReq = EducationRequest.builder()
                .institution("MIT")
                .degree("Bachelor of Science")
                .fieldOfStudy("Computer Science")
                .startDate(LocalDate.of(2018, 9, 1))
                .endDate(LocalDate.of(2022, 5, 30))
                .grade("3.9 GPA")
                .build();

        String response = mockMvc.perform(post("/profile/education")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.institution").value("MIT"))
                .andExpect(jsonPath("$.data.degree").value("Bachelor of Science"))
                .andReturn().getResponse().getContentAsString();

        Long eduId = objectMapper.readTree(response).get("data").get("id").asLong();

        EducationRequest updateReq = EducationRequest.builder()
                .institution("MIT")
                .degree("Master of Science")
                .fieldOfStudy("Computer Science")
                .startDate(LocalDate.of(2022, 9, 1))
                .endDate(LocalDate.of(2024, 5, 30))
                .grade("4.0 GPA")
                .build();

        mockMvc.perform(put("/profile/education/" + eduId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.degree").value("Master of Science"));

        mockMvc.perform(get("/profile/education"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].degree").value("Master of Science"));

        mockMvc.perform(delete("/profile/education/" + eduId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/profile/education"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void addEducation_invalidDateRange_returnsBadRequest() throws Exception {
        EducationRequest invalidReq = EducationRequest.builder()
                .institution("Stanford")
                .degree("PhD")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2020, 1, 1))
                .build();

        mockMvc.perform(post("/profile/education")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Start date cannot be after end date"));
    }

    @Test
    void updateEducation_nonExistent_returnsNotFound() throws Exception {
        EducationRequest updateReq = EducationRequest.builder()
                .institution("Harvard")
                .degree("BS")
                .build();

        mockMvc.perform(put("/profile/education/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());
    }

}
