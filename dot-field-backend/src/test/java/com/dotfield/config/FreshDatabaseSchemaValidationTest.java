package com.dotfield.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.url=jdbc:h2:mem:dot_field_fresh_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
class FreshDatabaseSchemaValidationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Flyway migrations execute in order and Hibernate schema validation succeeds")
    void validateFlywayMigrationsAndHibernateSchema() {
        // 1. Verify expected database tables exist
        List<Map<String, Object>> tables = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE LOWER(TABLE_SCHEMA) = 'public'"
        );

        Set<String> tableNames = tables.stream()
                .flatMap(row -> row.values().stream())
                .map(val -> val.toString().toLowerCase())
                .collect(Collectors.toSet());

        assertTrue(tableNames.contains("users"), "Table 'users' should exist");
        assertTrue(tableNames.contains("profiles"), "Table 'profiles' should exist");
        assertTrue(tableNames.contains("skills"), "Table 'skills' should exist");
        assertTrue(tableNames.contains("experiences"), "Table 'experiences' should exist");
        assertTrue(tableNames.contains("educations"), "Table 'educations' should exist");
        assertTrue(tableNames.contains("projects"), "Table 'projects' should exist");
        assertTrue(tableNames.contains("project_technologies"), "Table 'project_technologies' should exist");
        assertTrue(tableNames.contains("jobs"), "Table 'jobs' should exist");

        // 2. Verify V2 authentication link: profiles table contains user_id column
        List<Map<String, Object>> profileColumns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME) = 'PROFILES'"
        );

        Set<String> columnNames = profileColumns.stream()
                .flatMap(row -> row.values().stream())
                .map(val -> val.toString().toLowerCase())
                .collect(Collectors.toSet());

        assertTrue(columnNames.contains("user_id"), "Column 'user_id' should exist on 'profiles' table from V2 migration");

        // 3. Verify jobs table deduplication columns
        List<Map<String, Object>> jobColumns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME) = 'JOBS'"
        );

        Set<String> jobColumnNames = jobColumns.stream()
                .flatMap(row -> row.values().stream())
                .map(val -> val.toString().toLowerCase())
                .collect(Collectors.toSet());

        assertTrue(jobColumnNames.contains("canonical_url"), "Column 'canonical_url' should exist on 'jobs' table");
        assertTrue(jobColumnNames.contains("deduplication_fingerprint"), "Column 'deduplication_fingerprint' should exist on 'jobs' table");

        // 4. Verify Flyway schema history table recorded migrations
        List<Map<String, Object>> flywayHistory = jdbcTemplate.queryForList(
                "SELECT success FROM flyway_schema_history"
        );

        assertTrue(flywayHistory.size() >= 2, "Flyway should have recorded at least 2 migration versions");
    }
}
