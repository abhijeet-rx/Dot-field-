package com.dotfield.smoke;

import com.dotfield.dto.LoginRequest;
import com.dotfield.dto.RegisterRequest;
import com.dotfield.dto.UpdateProfileRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductionSmokeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Production Smoke Test: Boot -> Health check -> Register -> Login -> JWT -> Profile -> Job Query")
    void fullProductionSmokeTest() throws Exception {
        // 1. Health check probe
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("UP")))
                .andExpect(jsonPath("$.data.database", is("UP")));

        // 2. Register candidate user
        String email = "smoketest_" + System.currentTimeMillis() + "@example.com";
        RegisterRequest registerReq = RegisterRequest.builder()
                .email(email)
                .password("SmokeTestPass123!")
                .name("Smoke Test User")
                .build();

        MvcResult regResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andReturn();

        // 3. Login
        LoginRequest loginReq = LoginRequest.builder()
                .email(email)
                .password("SmokeTestPass123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("data").get("token").asText();
        assertNotNull(token);

        // 4. Authenticated profile update
        UpdateProfileRequest profileReq = UpdateProfileRequest.builder()
                .name("Smoke Test User Updated")
                .email(email)
                .location("San Francisco, CA")
                .phone("+1-555-0199")
                .build();

        mockMvc.perform(put("/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Smoke Test User Updated")))
                .andExpect(jsonPath("$.data.location", is("San Francisco, CA")));

        // 5. Authenticated job listing query
        mockMvc.perform(get("/jobs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }
}
