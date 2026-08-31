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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FreshDatabaseSchemaValidationTest {

    @Test
    @DisplayName("Real PostgreSQL: Flyway migrations (V1 → V2 → V3 → V4) execute in order and full schema is validated")
    void validateRealPostgresFlywaySchema() {
        PostgreSQLContainer<?> postgresContainer = null;

        // Issue 7: Only use Testcontainers — no hardcoded local PostgreSQL fallback credentials
        try {
            postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("dot_field_fresh_pg")
                    .withUsername("test")
                    .withPassword("test");
            postgresContainer.start();
        } catch (Throwable t) {
            Assumptions.assumeTrue(false,
                    "Docker/Testcontainers environment not available — skipping PostgreSQL integration test: " + t.getMessage());
        }

        try {
            DataSource dataSource = DataSourceBuilder.create()
                    .url(postgresContainer.getJdbcUrl())
                    .username(postgresContainer.getUsername())
                    .password(postgresContainer.getPassword())
                    .driverClassName("org.postgresql.Driver")
                    .build();

            // Execute all Flyway migrations (V1 → V2 → V3 → V4) from scratch
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .locations("classpath:db/migration")
                    .load();

            flyway.migrate();

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            // --- Verify all expected tables exist ---
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
            );

            Set<String> tableNames = tables.stream()
                    .map(row -> row.get("table_name").toString().toLowerCase())
                    .collect(Collectors.toSet());

            // V1 tables
            assertTrue(tableNames.contains("profiles"), "Table 'profiles' should exist (V1)");
            assertTrue(tableNames.contains("skills"), "Table 'skills' should exist (V1)");
            assertTrue(tableNames.contains("experiences"), "Table 'experiences' should exist (V1)");
            assertTrue(tableNames.contains("educations"), "Table 'educations' should exist (V1)");
            assertTrue(tableNames.contains("projects"), "Table 'projects' should exist (V1)");
            assertTrue(tableNames.contains("project_technologies"), "Table 'project_technologies' should exist (V1)");
            assertTrue(tableNames.contains("jobs"), "Table 'jobs' should exist (V1)");

            // V2 tables
            assertTrue(tableNames.contains("users"), "Table 'users' should exist (V2)");

            // V3 tables
            assertTrue(tableNames.contains("applications"), "Table 'applications' should exist (V3)");

            // --- V2: Verify profiles.user_id exists ---
            Set<String> profileColumns = getColumnNames(jdbcTemplate, "profiles");
            assertTrue(profileColumns.contains("user_id"), "Column 'user_id' should exist on 'profiles' table (V2)");

            // --- V3: Verify applications table columns and relationships ---
            Set<String> appColumns = getColumnNames(jdbcTemplate, "applications");
            assertTrue(appColumns.contains("id"), "Column 'id' should exist on 'applications' (V3)");
            assertTrue(appColumns.contains("profile_id"), "Column 'profile_id' should exist on 'applications' (V3)");
            assertTrue(appColumns.contains("job_id"), "Column 'job_id' should exist on 'applications' (V3)");
            assertTrue(appColumns.contains("status"), "Column 'status' should exist on 'applications' (V3)");
            assertTrue(appColumns.contains("notes"), "Column 'notes' should exist on 'applications' (V3)");
            assertTrue(appColumns.contains("applied_at"), "Column 'applied_at' should exist on 'applications' (V3)");
            assertTrue(appColumns.contains("created_at"), "Column 'created_at' should exist on 'applications' (V3)");
            assertTrue(appColumns.contains("updated_at"), "Column 'updated_at' should exist on 'applications' (V3)");

            // --- V4: Verify fit_score and match_category columns exist on applications ---
            assertTrue(appColumns.contains("fit_score"), "Column 'fit_score' should exist on 'applications' (V4)");
            assertTrue(appColumns.contains("match_category"), "Column 'match_category' should exist on 'applications' (V4)");

            // --- Verify Flyway schema history records V1, V2, V3, V4 in order and all succeeded ---
            List<Map<String, Object>> flywayHistory = jdbcTemplate.queryForList(
                    "SELECT version, description, success FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank"
            );

            assertTrue(flywayHistory.size() >= 4, "Flyway should have recorded at least V1, V2, V3, V4 migration versions");
            assertEquals("1", flywayHistory.get(0).get("version").toString());
            assertEquals("2", flywayHistory.get(1).get("version").toString());
            assertEquals("3", flywayHistory.get(2).get("version").toString());
            assertEquals("4", flywayHistory.get(3).get("version").toString());
            assertTrue((Boolean) flywayHistory.get(0).get("success"), "V1 migration should have succeeded");
            assertTrue((Boolean) flywayHistory.get(1).get("success"), "V2 migration should have succeeded");
            assertTrue((Boolean) flywayHistory.get(2).get("success"), "V3 migration should have succeeded");
            assertTrue((Boolean) flywayHistory.get(3).get("success"), "V4 migration should have succeeded");

        } finally {
            if (postgresContainer != null) {
                try {
                    postgresContainer.stop();
                } catch (Exception ignored) {}
            }
        }
    }

    private Set<String> getColumnNames(JdbcTemplate jdbcTemplate, String tableName) {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = ?",
                tableName
        );
        return columns.stream()
                .map(row -> row.get("column_name").toString().toLowerCase())
                .collect(Collectors.toSet());
    }
}
