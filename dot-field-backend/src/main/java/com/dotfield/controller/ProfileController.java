package com.dotfield.controller;

import com.dotfield.dto.ApiResponse;
import com.dotfield.dto.ProfileCompletenessResponse;
import com.dotfield.dto.ProfileResponse;
import com.dotfield.dto.UpdateProfileRequest;
import com.dotfield.security.CurrentUserService;
import com.dotfield.service.ProfileCompletenessService;
import com.dotfield.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final ProfileCompletenessService completenessService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile() {
        ProfileResponse profile = profileService.getProfile();
        return ResponseEntity.ok(ApiResponse.success(profile, "Profile retrieved successfully"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        ProfileResponse profile = profileService.updateProfile(request);
        return ResponseEntity.ok(ApiResponse.success(profile, "Profile updated successfully"));
    }

    @GetMapping("/completeness")
    public ResponseEntity<ApiResponse<ProfileCompletenessResponse>> getCompleteness() {
        Long userId = currentUserService.getCurrentUserId();
        ProfileCompletenessResponse completeness = completenessService.calculateCompleteness(userId);
        return ResponseEntity.ok(ApiResponse.success(completeness));
    }

}
