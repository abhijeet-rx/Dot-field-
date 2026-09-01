package com.dotfield.controller;

import com.dotfield.dto.*;
import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.JobStatus;
import com.dotfield.entity.RemoteType;
import com.dotfield.exception.BadRequestException;
import com.dotfield.service.JobExtractionService;
import com.dotfield.service.JobMatchingService;
import com.dotfield.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final JobExtractionService jobExtractionService;
    private final JobMatchingService jobMatchingService;

    @PostMapping
    public ResponseEntity<ApiResponse<JobResponse>> createJob(@Valid @RequestBody CreateJobRequest request) {
        JobResponse job = jobService.createJob(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(job, "Job opportunity created successfully"));
    }

    @PostMapping("/extract")
    public ResponseEntity<ApiResponse<JobResponse>> extractJob(@Valid @RequestBody ExtractJobRequest request) {
        JobResponse job = jobExtractionService.extractAndIngest(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(job, "Job opportunity extracted and ingested successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<JobResponse>>> getAllJobs(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) RemoteType remoteType,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (page < 0) {
            throw new BadRequestException("Page index must be >= 0. Received: " + page);
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("Page size must be between 1 and 100. Received: " + size);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PagedResponse<JobResponse> jobs = jobService.getAllJobs(status, company, source, remoteType, employmentType, pageable);
        return ResponseEntity.ok(ApiResponse.success(jobs, "Jobs retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> getJobById(@PathVariable Long id) {
        JobResponse job = jobService.getJobById(id);
        return ResponseEntity.ok(ApiResponse.success(job, "Job retrieved successfully"));
    }

    @GetMapping("/{id}/match")
    public ResponseEntity<ApiResponse<JobMatchResponse>> getJobMatch(@PathVariable Long id) {
        JobMatchResponse matchResponse = jobMatchingService.analyzeJob(id);
        return ResponseEntity.ok(ApiResponse.success(matchResponse, "Job match analysis completed successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> updateJob(
            @PathVariable Long id,
            @Valid @RequestBody UpdateJobRequest request) {
        JobResponse job = jobService.updateJob(id, request);
        return ResponseEntity.ok(ApiResponse.success(job, "Job updated successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<JobResponse>> updateJobStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateJobStatusRequest request) {
        JobResponse job = jobService.updateJobStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(job, "Job status updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Job deleted successfully"));
    }

}
