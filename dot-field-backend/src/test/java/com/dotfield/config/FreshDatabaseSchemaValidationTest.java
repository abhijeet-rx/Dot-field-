package com.dotfield.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FreshDatabaseSchemaValidationTest {

    @Test
    @DisplayName("Real PostgreSQL: Flyway migrations (V1 -> V2) execute in order and schema is validated against PostgreSQL")
    void validateRealPostgresFlywaySchema() {
        DataSource dataSource = null;
        PostgreSQLContainer<?> postgresContainer = null;

        // 1. Try Testcontainers PostgreSQL container if Docker is available
        try {
            postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("dot_field_fresh_pg")
                    .withUsername("test")
                    .withPassword("test");
            postgresContainer.start();
            dataSource = DataSourceBuilder.create()
                    .url(postgresContainer.getJdbcUrl())
                    .username(postgresContainer.getUsername())
                    .password(postgresContainer.getPassword())
                    .driverClassName("org.postgresql.Driver")
                    .build();
        } catch (Throwable t) {
            // Docker daemon not available — attempt local PostgreSQL fallback connection if reachable
            try {
                DataSource localDs = DataSourceBuilder.create()
                        .url("jdbc:postgresql://localhost:5432/postgres")
                        .username("postgres")
                        .password("postgres")
                        .driverClassName("org.postgresql.Driver")
                        .build();
                try (Connection conn = localDs.getConnection()) {
                    dataSource = localDs;
                }
            } catch (Throwable localEx) {
                // Neither Docker nor local PostgreSQL credentials reachable — assume test environment limitation
                Assumptions.assumeTrue(false, "Neither Docker/Testcontainers nor local PostgreSQL instance is reachable for fresh DB test: " + t.getMessage());
            }
        }

        try {
            assertNotNull(dataSource, "PostgreSQL DataSource must be initialized");

            // 2. Execute Flyway migrations V1 & V2 from scratch on fresh PostgreSQL database
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .locations("classpath:db/migration")
                    .load();

            flyway.migrate();

            // 3. Inspect resulting real PostgreSQL database schema via JdbcTemplate
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            List<Map<String, Object>> tables = jdbcTemplate.queryForList(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
            );

            Set<String> tableNames = tables.stream()
                    .map(row -> row.get("table_name").toString().toLowerCase())
                    .collect(Collectors.toSet());

            assertTrue(tableNames.contains("users"), "Table 'users' should exist in PostgreSQL");
            assertTrue(tableNames.contains("profiles"), "Table 'profiles' should exist in PostgreSQL");
            assertTrue(tableNames.contains("skills"), "Table 'skills' should exist in PostgreSQL");
            assertTrue(tableNames.contains("experiences"), "Table 'experiences' should exist in PostgreSQL");
            assertTrue(tableNames.contains("educations"), "Table 'educations' should exist in PostgreSQL");
            assertTrue(tableNames.contains("projects"), "Table 'projects' should exist in PostgreSQL");
            assertTrue(tableNames.contains("project_technologies"), "Table 'project_technologies' should exist in PostgreSQL");
            assertTrue(tableNames.contains("jobs"), "Table 'jobs' should exist in PostgreSQL");

            // 4. Verify V2 user_id foreign key on profiles table
            List<Map<String, Object>> profileColumns = jdbcTemplate.queryForList(
                    "SELECT column_name FROM information_schema.columns WHERE table_name = 'profiles'"
            );

            Set<String> columnNames = profileColumns.stream()
                    .map(row -> row.get("column_name").toString().toLowerCase())
            .collect(Collectors.toSet());

            assertTrue(columnNames.contains("user_id"), "Column 'user_id' should exist on 'profiles' table from V2 migration");

            // 5. Verify Flyway schema history recorded V1 and V2 in order
            List<Map<String, Object>> flywayHistory = jdbcTemplate.queryForList(
                    "SELECT version, description, success FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank"
            );

            assertTrue(flywayHistory.size() >= 2, "Flyway should have recorded at least V1 and V2 migration versions");
            assertEquals("1", flywayHistory.get(0).get("version").toString());
            assertEquals("2", flywayHistory.get(1).get("version").toString());
            assertTrue((Boolean) flywayHistory.get(0).get("success"));
            assertTrue((Boolean) flywayHistory.get(1).get("success"));

        } finally {
            if (postgresContainer != null) {
                try {
                    postgresContainer.stop();
                } catch (Exception ignored) {}
            }
        }
    }
}
