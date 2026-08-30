package com.dotfield.controller;

import com.dotfield.dto.LoginRequest;
import com.dotfield.dto.RegisterRequest;
import com.dotfield.entity.Role;
import com.dotfield.entity.User;
import com.dotfield.repository.ProfileRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        profileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/auth/register — Successful registration defaults to USER role and auto-creates profile")
    void registerSuccess() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("candidate@example.com")
                .password("Password123!")
                .name("Candidate Name")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.user.email", is("candidate@example.com")))
                .andExpect(jsonPath("$.data.user.role", is("USER")))
                .andExpect(jsonPath("$.data.user.profileId", notNullValue()));

        User user = userRepository.findByEmailIgnoreCase("candidate@example.com").orElseThrow();
        assertEquals(Role.USER, user.getRole());
        assertTrue(passwordEncoder.matches("Password123!", user.getPasswordHash()));
        assertTrue(profileRepository.findByUserId(user.getId()).isPresent());
    }

    @Test
    @DisplayName("POST /api/auth/register — Fails on duplicate email with 400")
    void registerDuplicateEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("duplicate@example.com")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("POST /api/auth/register — Fails on password shorter than 8 characters")
    void registerShortPassword() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("shortpass@example.com")
                .password("short")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register — Admin Bootstrap assigns ADMIN role when email matches initial.admin.email")
    void registerInitialAdmin() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("admin@example.com") // Matches initial.admin.email in test props
                .password("AdminPass123!")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.role", is("ADMIN")));

        User user = userRepository.findByEmailIgnoreCase("admin@example.com").orElseThrow();
        assertEquals(Role.ADMIN, user.getRole());
    }

    @Test
    @DisplayName("POST /api/auth/login — Successful login returns JWT token")
    void loginSuccess() throws Exception {
        User user = User.builder()
                .email("user@example.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .role(Role.USER)
                .build();
        userRepository.save(user);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("user@example.com")
                .password("Secret123!")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.user.email", is("user@example.com")));
    }

    @Test
    @DisplayName("POST /api/auth/login — Wrong password returns 401 generic error")
    void loginWrongPassword() throws Exception {
        User user = User.builder()
                .email("user@example.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .role(Role.USER)
                .build();
        userRepository.save(user);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("user@example.com")
                .password("WrongPassword")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("GET /api/auth/me — Returns authenticated user profile information")
    void getAuthMeSuccess() throws Exception {
        User user = User.builder()
                .email("me@example.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .role(Role.USER)
                .build();
        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email", is("me@example.com")))
                .andExpect(jsonPath("$.data.id", is(user.getId().intValue())));
    }
}
