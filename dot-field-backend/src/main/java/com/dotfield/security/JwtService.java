package com.dotfield.security;

import com.dotfield.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating, parsing, and validating JWT tokens.
 * Enforces HS256 (HMAC-SHA256) as the single authoritative algorithm for signing and verification.
 */
@Slf4j
@Service
public class JwtService {

    private final SecretKey hmacKey;
    private final String algorithm;
    private final long expirationMs;

    public JwtService(String secret, long expirationMs) {
        this(secret, expirationMs, "HS256");
    }

    public JwtService(String secret, long expirationMs, String algorithm) {
        this(secret, expirationMs, null, null, algorithm);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public JwtService(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.expiration:86400000}") long expirationMs,
            @Value("${jwt.jwks-url:}") String jwksUrl,
            @Value("${jwt.public-key-path:}") String publicKeyPath,
            @Value("${jwt.algorithm:HS256}") String algorithm) {

        this.expirationMs = expirationMs;
        this.algorithm = (algorithm != null && !algorithm.isBlank()) ? algorithm.trim().toUpperCase() : "HS256";

        if (!"HS256".equals(this.algorithm)) {
            throw new IllegalArgumentException("Unsupported JWT algorithm: '" + algorithm + "'. Only HS256 is supported.");
        }

        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret must not be null or blank");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 32 bytes (256 bits) for HS256 algorithm");
        }

        this.hmacKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long userId, String email, Role role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("role", role.name());

        return Jwts.builder()
                .subject(userId.toString())
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(hmacKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            if (token == null || token.isBlank()) {
                return false;
            }

            // Verify payload & signature with HS256 key
            Claims claims = Jwts.parser()
                    .verifyWith(hmacKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getExpiration() == null || claims.getExpiration().after(new Date());
        } catch (Exception e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    public String getEmailFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("email", String.class);
    }

    public Role getRoleFromToken(String token) {
        Claims claims = getClaims(token);
        String roleStr = claims.get("role", String.class);
        return roleStr != null ? Role.valueOf(roleStr) : Role.USER;
    }

    private Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(hmacKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new JwtException("JWT verification failed: " + e.getMessage(), e);
        }
    }

    public String getAlgorithm() {
        return algorithm;
    }
}
