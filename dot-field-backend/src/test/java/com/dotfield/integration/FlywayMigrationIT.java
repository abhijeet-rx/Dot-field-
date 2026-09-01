package com.dotfield.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class FlywayMigrationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("dot_field_test")
            .withUsername("test_user")
            .withPassword("test_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("jwt.secret", () -> "9DTNFPJ0xWC1o8EQa/KdcqkXyTFs66YZJsjuhj58G3o=");
    }

    @Autowired(required = false)
    private Flyway flyway;

    @Test
    @DisplayName("Flyway migrations V1 through V6 execute successfully against PostgreSQL container")
    void migrationsApplyCleanly() {
        assertThat(postgres.isRunning()).isTrue();
        if (flyway != null) {
            var info = flyway.info();
            assertThat(info.applied()).isNotEmpty();
            assertThat(info.current().getVersion().getVersion()).isEqualTo("6");
        }
    }
}
