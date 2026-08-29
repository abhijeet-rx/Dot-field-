package com.dotfield.controller;

import com.dotfield.dto.ApiResponse;
import com.dotfield.dto.ExperienceRequest;
import com.dotfield.dto.ExperienceResponse;
import com.dotfield.service.ExperienceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/profile/experience")
@RequiredArgsConstructor
public class ExperienceController {

    private final ExperienceService experienceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExperienceResponse>>> getExperience() {
        List<ExperienceResponse> experience = experienceService.getExperience();
        return ResponseEntity.ok(ApiResponse.success(experience, "Experience retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExperienceResponse>> addExperience(@Valid @RequestBody ExperienceRequest request) {
        ExperienceResponse experience = experienceService.addExperience(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(experience, "Experience added successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExperienceResponse>> updateExperience(
            @PathVariable Long id,
            @Valid @RequestBody ExperienceRequest request) {
        ExperienceResponse experience = experienceService.updateExperience(id, request);
        return ResponseEntity.ok(ApiResponse.success(experience, "Experience updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExperience(@PathVariable Long id) {
        experienceService.deleteExperience(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Experience deleted successfully"));
    }

}
