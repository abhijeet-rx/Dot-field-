package com.dotfield.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String location;
    private String linkedinUrl;
    private String githubUrl;
    private String portfolioUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<SkillResponse> skills = new ArrayList<>();

    @Builder.Default
    private List<EducationResponse> education = new ArrayList<>();

    @Builder.Default
    private List<ProjectResponse> projects = new ArrayList<>();

    @Builder.Default
    private List<ExperienceResponse> experience = new ArrayList<>();

}
