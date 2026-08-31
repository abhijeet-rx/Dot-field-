package com.dotfield.config;

import com.dotfield.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductionConfigurationFailureTest {

    @Test
    @DisplayName("JwtService initialization fails fast if secret is null, empty, or under 32 bytes (256 bits)")
    void jwtServiceFailsFastOnWeakOrMissingSecret() {
        // Null secret
        assertThrows(IllegalArgumentException.class, () -> new JwtService(null, 86400000L));

        // Empty secret
        assertThrows(IllegalArgumentException.class, () -> new JwtService("   ", 86400000L));

        // Short secret (< 32 bytes)
        assertThrows(IllegalArgumentException.class, () -> new JwtService("short_secret_under_32_bytes", 86400000L));

        // Valid 32-byte secret succeeds
        String validSecret = "404D635166546A576E5A7234753778214125442A472D4B6150645267556B5870";
        assertDoesNotThrow(() -> new JwtService(validSecret, 86400000L));
    }

    @Test
    @DisplayName(".gitignore files in root and backend contain .env to prevent secret leakage")
    void gitignoreIncludesEnvFiles() throws Exception {
        Path rootGitignore = Path.of("../.gitignore");
        if (Files.exists(rootGitignore)) {
            List<String> lines = Files.readAllLines(rootGitignore);
            boolean mentionsEnv = lines.stream().anyMatch(l -> l.trim().contains(".env"));
            assertTrue(mentionsEnv, "Root .gitignore must include .env pattern");
        }

        Path backendGitignore = Path.of(".gitignore");
        if (Files.exists(backendGitignore)) {
            List<String> lines = Files.readAllLines(backendGitignore);
            boolean mentionsEnv = lines.stream().anyMatch(l -> l.trim().contains(".env"));
            assertTrue(mentionsEnv, "Backend .gitignore must include .env pattern");
        }
    }
}
