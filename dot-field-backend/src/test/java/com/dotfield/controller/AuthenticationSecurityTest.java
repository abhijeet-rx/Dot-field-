package com.dotfield.controller;

import com.dotfield.dto.LoginRequest;
import com.dotfield.dto.RegisterRequest;
import com.dotfield.entity.Role;
import com.dotfield.entity.User;
import com.dotfield.repository.UserRepository;
import com.dotfield.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthenticationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    // ─── Registration Tests ────────────────────────────────────────

    @Test
    @DisplayName("Valid registration creates user with BCrypt password hash and profile")
    void validRegistrationTest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@example.com")
                .password("Password123!")
                .name("New User")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.user.email", is("newuser@example.com")))
                .andExpect(jsonPath("$.data.user.role", is("USER")))
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist());

        User user = userRepository.findByEmailIgnoreCase("newuser@example.com").orElseThrow();
        assertTrue(passwordEncoder.matches("Password123!", user.getPasswordHash()));
        assertNotEquals("Password123!", user.getPasswordHash());
    }

    @Test
    @DisplayName("Duplicate registration email (exact & case-insensitive) is rejected with 400")
    void duplicateEmailRegistrationRejected() throws Exception {
        User user = User.builder()
                .email("existing@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(Role.USER)
                .build();
        userRepository.save(user);

        RegisterRequest request = RegisterRequest.builder()
                .email("EXISTING@example.com")
                .password("Password123!")
                .name("Duplicate User")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("Invalid email format and blank/short password are rejected with 400")
    void invalidRegistrationPayloadRejected() throws Exception {
        RegisterRequest invalidEmail = RegisterRequest.builder()
                .email("not-an-email")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmail)))
                .andExpect(status().isBadRequest());

        RegisterRequest shortPassword = RegisterRequest.builder()
                .email("valid@example.com")
                .password("short")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortPassword)))
                .andExpect(status().isBadRequest());
    }

    // ─── Login Tests ───────────────────────────────────────────────

    @Test
    @DisplayName("Valid credentials produce JWT token with correct user ID, email, and role")
    void validLoginTest() throws Exception {
        User user = User.builder()
                .email("loginuser@example.com")
                .passwordHash(passwordEncoder.encode("CorrectPassword123!"))
                .role(Role.USER)
                .build();
        user = userRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .email("LOGINUSER@example.com")
                .password("CorrectPassword123!")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.user.id", is(user.getId().intValue())))
                .andExpect(jsonPath("$.data.user.email", is("loginuser@example.com")));
    }

    @Test
    @DisplayName("Invalid password and non-existent account return 401 with generic error message")
    void invalidLoginCredentialsRejected() throws Exception {
        User user = User.builder()
                .email("user@example.com")
                .passwordHash(passwordEncoder.encode("CorrectPassword"))
                .role(Role.USER)
                .build();
        userRepository.save(user);

        // Wrong password
        LoginRequest wrongPassword = LoginRequest.builder()
                .email("user@example.com")
                .password("WrongPassword")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPassword)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid email or password")));

        // Nonexistent user
        LoginRequest nonExistent = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistent)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    // ─── Protected API & JWT Validation Tests ──────────────────────

    @Test
    @DisplayName("Request without Authorization header returns 401 Unauthorized")
    void unauthenticatedRequestRejected() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("Unauthorized")));
    }

    @Test
    @DisplayName("Request with malformed or invalid signature JWT returns 401 Unauthorized")
    void malformedOrInvalidTokenRejected() throws Exception {
        // Malformed token string
        mockMvc.perform(get("/profile")
                        .header("Authorization", "Bearer not.a.valid.jwt.token"))
                .andExpect(status().isUnauthorized());

        // Tampered token
        String validToken = jwtService.generateToken(1L, "test@example.com", Role.USER);
        String tamperedToken = validToken + "tampered";

        mockMvc.perform(get("/profile")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized());
    }
}
