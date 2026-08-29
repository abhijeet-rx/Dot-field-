package com.dotfield.mapper;

import com.dotfield.dto.*;
import com.dotfield.entity.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProfileMapper {

    public ProfileResponse toProfileResponse(Profile profile) {
        if (profile == null) {
            return null;
        }

        return ProfileResponse.builder()
                .id(profile.getId())
                .name(profile.getName())
                .email(profile.getEmail())
                .phone(profile.getPhone())
                .location(profile.getLocation())
                .linkedinUrl(profile.getLinkedinUrl())
                .githubUrl(profile.getGithubUrl())
                .portfolioUrl(profile.getPortfolioUrl())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .skills(profile.getSkills() != null
                        ? profile.getSkills().stream().map(this::toSkillResponse).collect(Collectors.toList())
                        : Collections.emptyList())
                .education(profile.getEducation() != null
                        ? profile.getEducation().stream().map(this::toEducationResponse).collect(Collectors.toList())
                        : Collections.emptyList())
                .projects(profile.getProjects() != null
                        ? profile.getProjects().stream().map(this::toProjectResponse).collect(Collectors.toList())
                        : Collections.emptyList())
                .experience(profile.getExperience() != null
                        ? profile.getExperience().stream().map(this::toExperienceResponse).collect(Collectors.toList())
                        : Collections.emptyList())
                .build();
    }

    public SkillResponse toSkillResponse(Skill skill) {
        if (skill == null) {
            return null;
        }
        return SkillResponse.builder()
                .id(skill.getId())
                .name(skill.getName())
                .category(skill.getCategory())
                .build();
    }

    public Skill toSkillEntity(SkillRequest request) {
        if (request == null) {
            return null;
        }
        return Skill.builder()
                .name(request.getName().trim())
                .category(request.getCategory())
                .build();
    }

    public EducationResponse toEducationResponse(Education education) {
        if (education == null) {
            return null;
        }
        return EducationResponse.builder()
                .id(education.getId())
                .institution(education.getInstitution())
                .degree(education.getDegree())
                .fieldOfStudy(education.getFieldOfStudy())
                .startDate(education.getStartDate())
                .endDate(education.getEndDate())
                .grade(education.getGrade())
                .build();
    }

    public Education toEducationEntity(EducationRequest request) {
        if (request == null) {
            return null;
        }
        return Education.builder()
                .institution(request.getInstitution())
                .degree(request.getDegree())
                .fieldOfStudy(request.getFieldOfStudy())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .grade(request.getGrade())
                .build();
    }

    public ProjectResponse toProjectResponse(Project project) {
        if (project == null) {
            return null;
        }
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .githubUrl(project.getGithubUrl())
                .liveUrl(project.getLiveUrl())
                .technologies(project.getTechnologies() != null
                        ? new ArrayList<>(project.getTechnologies())
                        : Collections.emptyList())
                .build();
    }

    public Project toProjectEntity(ProjectRequest request) {
        if (request == null) {
            return null;
        }
        return Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .githubUrl(request.getGithubUrl())
                .liveUrl(request.getLiveUrl())
                .technologies(request.getTechnologies() != null
                        ? new ArrayList<>(request.getTechnologies())
                        : new ArrayList<>())
                .build();
    }

    public ExperienceResponse toExperienceResponse(Experience experience) {
        if (experience == null) {
            return null;
        }
        return ExperienceResponse.builder()
                .id(experience.getId())
                .company(experience.getCompany())
                .role(experience.getRole())
                .description(experience.getDescription())
                .startDate(experience.getStartDate())
                .endDate(experience.getEndDate())
                .build();
    }

    public Experience toExperienceEntity(ExperienceRequest request) {
        if (request == null) {
            return null;
        }
        return Experience.builder()
                .company(request.getCompany())
                .role(request.getRole())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();
    }

}
