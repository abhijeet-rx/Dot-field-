package com.dotfield.controller;

import com.dotfield.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health-check controller.
 * Provides a simple endpoint to verify the backend is running.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        Map<String, String> data = Map.of("status", "UP");
        return ResponseEntity.ok(ApiResponse.success(data));
    }

}
