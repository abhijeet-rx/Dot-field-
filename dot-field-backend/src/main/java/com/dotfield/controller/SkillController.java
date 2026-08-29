package com.dotfield.controller;

import com.dotfield.dto.ApiResponse;
import com.dotfield.dto.SkillRequest;
import com.dotfield.dto.SkillResponse;
import com.dotfield.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/profile/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillResponse>>> getSkills() {
        List<SkillResponse> skills = skillService.getSkills();
        return ResponseEntity.ok(ApiResponse.success(skills, "Skills retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SkillResponse>> addSkill(@Valid @RequestBody SkillRequest request) {
        SkillResponse skill = skillService.addSkill(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(skill, "Skill added successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSkill(@PathVariable Long id) {
        skillService.deleteSkill(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Skill deleted successfully"));
    }

}
