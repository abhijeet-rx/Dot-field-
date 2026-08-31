package com.dotfield.controller;

import com.dotfield.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health-check controller suitable for production deployments.
 * Verifies application running state and database connectivity.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("status", "UP");

        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            statusMap.put("database", "UP");
        } catch (Exception e) {
            log.error("Health check database probe failed: {}", e.getMessage());
            statusMap.put("database", "DOWN");
            return ResponseEntity.status(503).body(ApiResponse.success(statusMap, "Database is unreachable"));
        }

        return ResponseEntity.ok(ApiResponse.success(statusMap));
    }

}
