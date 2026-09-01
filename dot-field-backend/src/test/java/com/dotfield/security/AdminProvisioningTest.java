package com.dotfield.security;

import com.dotfield.config.AdminBootstrapInitializer;
import com.dotfield.dto.AuthResponse;
import com.dotfield.dto.LoginRequest;
import com.dotfield.dto.RegisterRequest;
import com.dotfield.entity.Role;
import com.dotfield.entity.User;
import com.dotfield.repository.ProfileRepository;
import com.dotfield.repository.UserRepository;
import com.dotfield.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "initial.admin.email=sysadmin@example.com"
})
class AdminProvisioningTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AdminBootstrapInitializer adminBootstrapInitializer;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        profileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Test 1: Configured admin email receives ADMIN role upon initial registration")
    void test1_configuredAdminEmailReceivesAdminRole() {
        RegisterRequest request = RegisterRequest.builder()
                .email("sysadmin@example.com")
                .password("AdminSecret@123")
                .name("Sys Admin")
                .build();

        AuthResponse response = authService.register(request);
        assertNotNull(response);
        assertEquals("ADMIN", response.getUser().getRole());

        User dbUser = userRepository.findByEmailIgnoreCase("sysadmin@example.com").orElseThrow();
        assertEquals(Role.ADMIN, dbUser.getRole());
    }

    @Test
    @DisplayName("Test 2: Configured admin logs in and receives ADMIN role in UserResponse & JWT authority")
    void test2_configuredAdminLoginGeneratesAdminJwt() {
        test1_configuredAdminEmailReceivesAdminRole();

        LoginRequest loginReq = LoginRequest.builder()
                .email("sysadmin@example.com")
                .password("AdminSecret@123")
                .build();

        AuthResponse loginResp = authService.login(loginReq);
        assertEquals("ADMIN", loginResp.getUser().getRole());

        Role roleFromToken = jwtService.getRoleFromToken(loginResp.getToken());
        assertEquals(Role.ADMIN, roleFromToken);
    }

    @Test
    @DisplayName("Test 3: Configured admin logging in repeatedly maintains ADMIN role without duplicate users/profiles")
    void test3_repeatedAdminLoginsMaintainRoleAndIndempotency() {
        test1_configuredAdminEmailReceivesAdminRole();
        long initialUserCount = userRepository.count();
        long initialProfileCount = profileRepository.count();

        LoginRequest loginReq = LoginRequest.builder()
                .email("sysadmin@example.com")
                .password("AdminSecret@123")
                .build();

        for (int i = 0; i < 5; i++) {
            AuthResponse resp = authService.login(loginReq);
            assertEquals("ADMIN", resp.getUser().getRole());
        }

        assertEquals(initialUserCount, userRepository.count());
        assertEquals(initialProfileCount, profileRepository.count());
    }

    @Test
    @DisplayName("Test 4: Normal user receives USER role upon registration")
    void test4_normalUserReceivesUserRole() {
        RegisterRequest request = RegisterRequest.builder()
                .email("candidate@example.com")
                .password("UserPass@123")
                .name("Candidate User")
                .build();

        AuthResponse response = authService.register(request);
        assertEquals("USER", response.getUser().getRole());

        User dbUser = userRepository.findByEmailIgnoreCase("candidate@example.com").orElseThrow();
        assertEquals(Role.USER, dbUser.getRole());
    }

    @Test
    @DisplayName("Test 5: Normal user gets 403 Forbidden on administrative endpoints (/jobs/discover & /jobs/ingestion/run)")
    void test5_normalUserForbiddenOnAdminEndpoints() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("candidate@example.com")
                .password("UserPass@123")
                .build();

        AuthResponse authResp = authService.register(request);
        String userToken = authResp.getToken();

        mockMvc.perform(post("/jobs/discover")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"INDIANAPI\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/jobs/ingestion/run")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test 6: Client cannot pass role in registration payload to elevate to ADMIN")
    void test6_roleEscalationInPayloadIsImpossible() {
        RegisterRequest request = RegisterRequest.builder()
                .email("malicious@example.com")
                .password("MaliciousPass@123")
                .build();

        AuthResponse response = authService.register(request);
        assertEquals("USER", response.getUser().getRole());
    }

    @Test
    @DisplayName("Test 7: AdminBootstrapInitializer idempotently ensures configured admin identity retains ADMIN role")
    void test7_adminBootstrapInitializerEnsuresAdminRole() {
        // Register candidate first
        RegisterRequest request = RegisterRequest.builder()
                .email("sysadmin@example.com")
                .password("AdminSecret@123")
                .build();
        authService.register(request);

        // Manually alter DB role to USER for testing
        User dbUser = userRepository.findByEmailIgnoreCase("sysadmin@example.com").orElseThrow();
        dbUser.setRole(Role.USER);
        userRepository.save(dbUser);

        // Trigger bootstrap listener
        adminBootstrapInitializer.onApplicationReady();

        User reloadedUser = userRepository.findByEmailIgnoreCase("sysadmin@example.com").orElseThrow();
        assertEquals(Role.ADMIN, reloadedUser.getRole());
    }
}
