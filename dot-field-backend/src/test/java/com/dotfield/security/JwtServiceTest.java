package com.dotfield.security;

import com.dotfield.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String TEST_SECRET = "404D635166546A576E5A7234753778214125442A472D4B6150645267556B5870";
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, 3600000L);
    }

    @Test
    @DisplayName("Should generate valid JWT token with subject userId and claims")
    void shouldGenerateValidToken() {
        String token = jwtService.generateToken(100L, "user@example.com", Role.USER);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals(100L, jwtService.getUserIdFromToken(token));
        assertEquals("user@example.com", jwtService.getEmailFromToken(token));
        assertEquals(Role.USER, jwtService.getRoleFromToken(token));
    }

    @Test
    @DisplayName("Should reject malformed or tampered JWT token")
    void shouldRejectInvalidToken() {
        String token = jwtService.generateToken(100L, "user@example.com", Role.USER);
        String invalidToken = token + "tampered";

        assertFalse(jwtService.validateToken(invalidToken));
    }

    @Test
    @DisplayName("Should reject expired JWT token")
    void shouldRejectExpiredToken() {
        JwtService shortLivedJwtService = new JwtService(TEST_SECRET, -1000L);
        String expiredToken = shortLivedJwtService.generateToken(100L, "user@example.com", Role.USER);

        assertFalse(shortLivedJwtService.validateToken(expiredToken));
    }

    @Test
    @DisplayName("Should fail fast on empty secret string")
    void shouldFailFastOnEmptySecret() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService("   ", 3600000L));
    }
}
