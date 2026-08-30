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

}
