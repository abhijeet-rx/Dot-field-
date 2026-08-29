package com.dotfield.controller;

import com.dotfield.dto.ApiResponse;
import com.dotfield.dto.ProfileResponse;
import com.dotfield.dto.UpdateProfileRequest;
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

}
