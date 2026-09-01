package com.dotfield.security;

import com.dotfield.entity.Role;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating, parsing, and validating JWT tokens.
 * Supports RS256 / JWKS verification with fallback to HMAC-SHA256.
 */
@Slf4j
@Service
public class JwtService {

    private final SecretKey hmacKey;
    private final PublicKey rsaPublicKey;
    private final String jwksUrl;
    private final String algorithm;
    private final long expirationMs;

    private JWKSet cachedJwkSet;
    private long jwkSetCacheExpiryMs = 0;
    private static final long JWK_CACHE_TTL_MS = 15 * 60 * 1000; // 15 minutes TTL

    public JwtService(String secret, long expirationMs) {
        this(secret, expirationMs, null, null, "HS256");
    }

    @org.springframework.beans.factory.annotation.Autowired
    public JwtService(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.expiration:86400000}") long expirationMs,
            @Value("${jwt.jwks-url:}") String jwksUrl,
            @Value("${jwt.public-key-path:}") String publicKeyPath,
            @Value("${jwt.algorithm:HS256}") String algorithm) {

        this.expirationMs = expirationMs;
        this.jwksUrl = (jwksUrl != null && !jwksUrl.isBlank()) ? jwksUrl.trim() : null;
        this.algorithm = (algorithm != null && !algorithm.isBlank()) ? algorithm.trim().toUpperCase() : "HS256";

        if ("HS256".equalsIgnoreCase(this.algorithm) && (jwksUrl == null || jwksUrl.isBlank()) && (publicKeyPath == null || publicKeyPath.isBlank())) {
            if (secret == null || secret.isBlank()) {
                throw new IllegalArgumentException("JWT secret must not be null or blank");
            }
            if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalArgumentException("JWT secret key must be at least 32 bytes (256 bits) for HS256 algorithm");
            }
        }

        PublicKey parsedRsaKey = null;
        if (publicKeyPath != null && !publicKeyPath.isBlank()) {
            try {
                parsedRsaKey = loadPublicKeyFromString(publicKeyPath);
            } catch (Exception e) {
                log.warn("Failed to load RSA public key from config: {}", e.getMessage());
            }
        }
        this.rsaPublicKey = parsedRsaKey;

        if (secret != null && !secret.isBlank()) {
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length >= 32) {
                this.hmacKey = Keys.hmacShaKeyFor(keyBytes);
            } else {
                log.warn("jwt.secret length is under 32 bytes — HMAC signing/verification will require valid secret for HS256");
                this.hmacKey = null;
            }
        } else {
            this.hmacKey = null;
        }

        if (this.hmacKey == null && this.rsaPublicKey == null && this.jwksUrl == null) {
            log.warn("No JWT verification key configured (jwt.secret, jwt.jwks-url, or jwt.public-key-path)");
        }
    }

    public String generateToken(Long userId, String email, Role role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("role", role.name());

        if (hmacKey == null) {
            throw new IllegalStateException("HMAC key (jwt.secret) must be configured to generate tokens");
        }

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

            if (jwksUrl != null) {
                return validateWithJwks(token);
            }

            if ("RS256".equals(algorithm) && rsaPublicKey != null) {
                Jwts.parser().verifyWith(rsaPublicKey).build().parseSignedClaims(token);
                return true;
            }

            if (hmacKey != null) {
                Jwts.parser().verifyWith(hmacKey).build().parseSignedClaims(token);
                return true;
            }

            log.error("JWT validation failed: No valid verification method available");
            return false;
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
        if (jwksUrl != null) {
            try {
                SignedJWT signedJWT = SignedJWT.parse(token);
                String keyId = signedJWT.getHeader().getKeyID();
                JWKSet jwkSet = fetchJwkSetWithCaching();
                JWK jwk = (keyId != null) ? jwkSet.getKeyByKeyId(keyId) : jwkSet.getKeys().stream().findFirst().orElse(null);
                if (jwk instanceof RSAKey rsaKey) {
                    PublicKey pubKey = rsaKey.toRSAPublicKey();
                    return Jwts.parser().verifyWith(pubKey).build().parseSignedClaims(token).getPayload();
                }
            } catch (Exception e) {
                throw new JwtException("Failed to verify JWKS signature for token: " + e.getMessage(), e);
            }
        }

        if ("RS256".equals(algorithm) && rsaPublicKey != null) {
            return Jwts.parser().verifyWith(rsaPublicKey).build().parseSignedClaims(token).getPayload();
        }

        if (hmacKey != null) {
            return Jwts.parser().verifyWith(hmacKey).build().parseSignedClaims(token).getPayload();
        }

        throw new JwtException("No valid JWT verification key configured");
    }

    private boolean validateWithJwks(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String keyId = signedJWT.getHeader().getKeyID();
            JWKSet jwkSet = fetchJwkSetWithCaching();

            JWK jwk = (keyId != null) ? jwkSet.getKeyByKeyId(keyId) : jwkSet.getKeys().stream().findFirst().orElse(null);
            if (jwk instanceof RSAKey rsaKey) {
                PublicKey pubKey = rsaKey.toRSAPublicKey();
                Jwts.parser().verifyWith(pubKey).build().parseSignedClaims(token);
                return true;
            }
            log.warn("JWKS verification failed: No matching RSA key found for kid={}", keyId);
            return false;
        } catch (Exception e) {
            log.warn("JWKS JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    private synchronized JWKSet fetchJwkSetWithCaching() throws Exception {
        long now = System.currentTimeMillis();
        if (cachedJwkSet == null || now > jwkSetCacheExpiryMs) {
            log.info("Fetching JWKS from remote URL: {}", jwksUrl);
            cachedJwkSet = JWKSet.load(URI.create(jwksUrl).toURL(), 5000, 5000, 100000);
            jwkSetCacheExpiryMs = now + JWK_CACHE_TTL_MS;
        }
        return cachedJwkSet;
    }

    private PublicKey loadPublicKeyFromString(String keyPem) throws Exception {
        String clean = keyPem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(clean);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }
}
