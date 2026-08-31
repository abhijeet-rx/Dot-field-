package com.dotfield.controller;

import com.dotfield.entity.Role;
import com.dotfield.entity.User;
import com.dotfield.repository.UserRepository;
import com.dotfield.security.JwtService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StandardizedApiErrorTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String userToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = User.builder()
                .email("user@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();
        user = userRepository.save(user);
        userToken = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
    }

    @Test
    @DisplayName("400 Bad Request — Returns standardized ApiError without stack traces or sensitive data")
    void badRequestErrorFormatTest() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalid-email\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    @DisplayName("401 Unauthorized — Returns standardized ApiError when token is missing")
    void unauthorizedErrorFormatTest() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("Unauthorized")))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("403 Forbidden — Returns standardized ApiError when USER accesses ADMIN endpoint")
    void forbiddenErrorFormatTest() throws Exception {
        mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Forbidden Job\",\"company\":\"Co\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("Access denied")))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("404 Not Found — Returns standardized ApiError when resource doesn't exist")
    void notFoundErrorFormatTest() throws Exception {
        mockMvc.perform(get("/jobs/99999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Job not found")))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("405 Method Not Allowed — Returns standardized ApiError for unsupported HTTP method")
    void methodNotAllowedErrorFormatTest() throws Exception {
        mockMvc.perform(patch("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status", is(405)))
                .andExpect(jsonPath("$.message", containsString("not supported")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }
}
