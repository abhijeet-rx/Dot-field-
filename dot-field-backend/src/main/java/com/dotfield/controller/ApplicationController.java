package com.dotfield.controller;

import com.dotfield.dto.*;
import com.dotfield.entity.ApplicationStatus;
import com.dotfield.exception.BadRequestException;
import com.dotfield.security.CurrentUserService;
import com.dotfield.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt", "appliedAt", "status");

    private final ApplicationService applicationService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationResponse>> createApplication(
            @Valid @RequestBody CreateApplicationRequest body) {
        Long userId = currentUserService.getCurrentUserId();
        ApplicationResponse response = applicationService.createApplication(userId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ApplicationResponse>>> getApplications(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("Invalid sort field: '" + sortBy + "'. Allowed sort fields are: " + ALLOWED_SORT_FIELDS);
        }

        Long userId = currentUserService.getCurrentUserId();
        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        PagedResponse<ApplicationResponse> response = applicationService.getApplications(userId, status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<ApplicationResponse>> checkJobTrackedStatus(
            @RequestParam Long jobId) {
        Long userId = currentUserService.getCurrentUserId();
        return applicationService.getApplicationByJobId(userId, jobId)
                .map(app -> ResponseEntity.ok(ApiResponse.success(app)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success(null)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicationResponse>> getApplicationById(
            @PathVariable Long id) {
        Long userId = currentUserService.getCurrentUserId();
        ApplicationResponse response = applicationService.getApplicationById(userId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateApplicationStatusRequest body) {
        Long userId = currentUserService.getCurrentUserId();
        ApplicationResponse response = applicationService.updateStatus(userId, id, body.getStatus());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/notes")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateNotes(
            @PathVariable Long id,
            @Valid @RequestBody UpdateApplicationNotesRequest body) {
        Long userId = currentUserService.getCurrentUserId();
        String notes = body != null ? body.getNotes() : null;
        ApplicationResponse response = applicationService.updateNotes(userId, id, notes);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(
            @PathVariable Long id) {
        Long userId = currentUserService.getCurrentUserId();
        applicationService.deleteApplication(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<ApplicationAnalyticsResponse>> getAnalytics() {
        Long userId = currentUserService.getCurrentUserId();
        ApplicationAnalyticsResponse response = applicationService.getAnalytics(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
