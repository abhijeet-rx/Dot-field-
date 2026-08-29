package com.dotfield.controller;

import com.dotfield.dto.ApiResponse;
import com.dotfield.dto.EducationRequest;
import com.dotfield.dto.EducationResponse;
import com.dotfield.service.EducationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/profile/education")
@RequiredArgsConstructor
public class EducationController {

    private final EducationService educationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EducationResponse>>> getEducation() {
        List<EducationResponse> education = educationService.getEducation();
        return ResponseEntity.ok(ApiResponse.success(education, "Education retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EducationResponse>> addEducation(@Valid @RequestBody EducationRequest request) {
        EducationResponse education = educationService.addEducation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(education, "Education added successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EducationResponse>> updateEducation(
            @PathVariable Long id,
            @Valid @RequestBody EducationRequest request) {
        EducationResponse education = educationService.updateEducation(id, request);
        return ResponseEntity.ok(ApiResponse.success(education, "Education updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEducation(@PathVariable Long id) {
        educationService.deleteEducation(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Education deleted successfully"));
    }

}
