package com.dotfield.controller;

import com.dotfield.dto.ApiResponse;
import com.dotfield.dto.TailoredResumeResponse;
import com.dotfield.tailoring.ResumeTailoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class ResumeTailoringController {

    private final ResumeTailoringService tailoringService;

    @GetMapping("/{id}/resume/tailor")
    public ResponseEntity<ApiResponse<TailoredResumeResponse>> tailorResume(@PathVariable Long id) {
        TailoredResumeResponse response = tailoringService.tailorResume(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Resume tailored successfully"));
    }

}
