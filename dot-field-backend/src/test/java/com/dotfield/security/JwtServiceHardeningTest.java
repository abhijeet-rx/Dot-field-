package com.dotfield.security;

import com.dotfield.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceHardeningTest {

    private static final String VALID_SECRET_KEY = "9DTNFPJ0xWC1o8EQa/KdcqkXyTFs66YZJsjuhj58G3o=";

    @Test
    @DisplayName("HMAC HS256 token generation and validation succeeds with valid secret key")
    void hmacTokenGenerationAndValidationSucceeds() {
        JwtService jwtService = new JwtService(VALID_SECRET_KEY, 86400000L);

        String token = jwtService.generateToken(101L, "user@example.com", Role.USER);
        assertThat(token).isNotBlank();

        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.getUserIdFromToken(token)).isEqualTo(101L);
        assertThat(jwtService.getEmailFromToken(token)).isEqualTo("user@example.com");
        assertThat(jwtService.getRoleFromToken(token)).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("Invalid or tampered token validation returns false gracefully")
    void invalidTokenValidationReturnsFalse() {
        JwtService jwtService = new JwtService(VALID_SECRET_KEY, 86400000L);
        assertThat(jwtService.validateToken("invalid.jwt.token")).isFalse();
        assertThat(jwtService.validateToken(null)).isFalse();
        assertThat(jwtService.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("Rejects short secret keys (< 32 bytes)")
    void rejectsShortSecretKeys() {
        assertThatThrownBy(() -> new JwtService("shortsecret", 86400000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    @DisplayName("Rejects unsupported non-HS256 algorithms")
    void rejectsUnsupportedAlgorithm() {
        assertThatThrownBy(() -> new JwtService(VALID_SECRET_KEY, 86400000L, null, null, "RS256"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported JWT algorithm");
    }
}
