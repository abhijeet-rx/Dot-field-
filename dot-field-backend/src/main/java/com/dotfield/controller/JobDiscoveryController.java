package com.dotfield.controller;

import com.dotfield.discovery.JobDiscoveryService;
import com.dotfield.dto.ApiResponse;
import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.JobDiscoveryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dotfield.dto.IngestionStatusResponse;
import com.dotfield.dto.JobIngestionRunResponse;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobDiscoveryController {

    private final JobDiscoveryService discoveryService;

    @PostMapping("/discover")
    public ResponseEntity<ApiResponse<JobDiscoveryResponse>> discoverJobs(@Valid @RequestBody JobDiscoveryRequest request) {
        JobDiscoveryResponse response = discoveryService.discoverJobs(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Job discovery completed successfully"));
    }

    @PostMapping("/ingestion/run")
    public ResponseEntity<ApiResponse<JobIngestionRunResponse>> runManualIngestion(@RequestBody(required = false) JobDiscoveryRequest request) {
        JobIngestionRunResponse response = discoveryService.runManualIngestion(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Job ingestion completed successfully"));
    }

    @GetMapping("/ingestion/status")
    public ResponseEntity<ApiResponse<IngestionStatusResponse>> getIngestionStatus() {
        IngestionStatusResponse status = discoveryService.getIngestionStatus();
        return ResponseEntity.ok(ApiResponse.success(status, "Ingestion status retrieved successfully"));
    }

}
