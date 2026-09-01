package com.dotfield.security;

import com.dotfield.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceHardeningTest {

    private static final String SECRET_KEY = "9DTNFPJ0xWC1o8EQa/KdcqkXyTFs66YZJsjuhj58G3o=";

    @Test
    @DisplayName("HMAC token generation and validation succeeds with standard secret key")
    void hmacTokenGenerationAndValidationSucceeds() {
        JwtService jwtService = new JwtService(SECRET_KEY, 86400000L);

        String token = jwtService.generateToken(101L, "user@example.com", Role.USER);
        assertThat(token).isNotBlank();

        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.getUserIdFromToken(token)).isEqualTo(101L);
        assertThat(jwtService.getEmailFromToken(token)).isEqualTo("user@example.com");
        assertThat(jwtService.getRoleFromToken(token)).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("Invalid token validation returns false gracefully")
    void invalidTokenValidationReturnsFalse() {
        JwtService jwtService = new JwtService(SECRET_KEY, 86400000L);
        assertThat(jwtService.validateToken("invalid.jwt.token")).isFalse();
        assertThat(jwtService.validateToken(null)).isFalse();
        assertThat(jwtService.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("RS256 algorithm with invalid JWKS URL fails closed on validation")
    void invalidJwksUrlFailsClosed() {
        JwtService jwtService = new JwtService(
                SECRET_KEY,
                86400000L,
                "https://invalid-jwks-domain-12345.com/.well-known/jwks.json",
                null,
                "RS256"
        );

        assertThat(jwtService.validateToken("header.payload.signature")).isFalse();
    }
}
