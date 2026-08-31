package com.dotfield.config;

import com.dotfield.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ProductionConfigurationFailureTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(JwtService.class);

    @Test
    @DisplayName("Spring ApplicationContext startup fails fast when jwt.secret is missing, empty, or under 32 bytes")
    void contextStartupFailsFastOnMissingOrWeakJwtSecret() {
        // 1. Missing jwt.secret property -> Context creation fails fast
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
        });

        // 2. Empty jwt.secret property -> Context creation fails fast
        contextRunner.withPropertyValues("jwt.secret=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
        });

        // 3. Short jwt.secret property (< 32 bytes) -> Context creation fails fast
        contextRunner.withPropertyValues("jwt.secret=too_short_secret").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
        });

        // 4. Valid 32-byte jwt.secret property -> Context creation succeeds
        contextRunner.withPropertyValues("jwt.secret=404D635166546A576E5A7234753778214125442A472D4B6150645267556B5870").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(JwtService.class);
        });
    }

    @Test
    @DisplayName("JwtService unit initialization fails fast if secret is null, empty, or under 32 bytes")
    void jwtServiceFailsFastOnWeakOrMissingSecret() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService(null, 86400000L));
        assertThrows(IllegalArgumentException.class, () -> new JwtService("   ", 86400000L));
        assertThrows(IllegalArgumentException.class, () -> new JwtService("short_secret_under_32_bytes", 86400000L));

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
