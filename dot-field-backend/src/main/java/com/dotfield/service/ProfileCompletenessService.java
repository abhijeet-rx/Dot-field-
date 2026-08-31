package com.dotfield.service;

import com.dotfield.dto.ProfileCompletenessResponse;
import com.dotfield.entity.Profile;
import com.dotfield.exception.ResourceNotFoundException;
import com.dotfield.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProfileCompletenessService {

    private final ProfileRepository profileRepository;

    public ProfileCompletenessResponse calculateCompleteness(Long userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user ID: " + userId));

        Map<String, Integer> sections = new LinkedHashMap<>();
        List<String> recommendations = new ArrayList<>();

        // 1. Contact (20%)
        int contactScore = 0;
        if (profile.getName() != null && !profile.getName().isBlank()) {
            contactScore += 30;
        } else {
            recommendations.add("Add your full name");
        }

        if (profile.getEmail() != null && !profile.getEmail().isBlank()) {
            contactScore += 30;
        } else {
            recommendations.add("Add your contact email address");
        }

        if (profile.getPhone() != null && !profile.getPhone().isBlank()) {
            contactScore += 20;
        } else {
            recommendations.add("Add your phone number");
        }

        if (profile.getLocation() != null && !profile.getLocation().isBlank()) {
            contactScore += 20;
        } else {
            recommendations.add("Add your location");
        }
        sections.put("contact", contactScore);

        // 2. Skills (25%)
        int skillCount = (profile.getSkills() != null) ? profile.getSkills().size() : 0;
        int skillScore = Math.min(100, skillCount * 20);
        sections.put("skills", skillScore);
        if (skillScore < 100) {
            recommendations.add("Add at least 5 key technical or soft skills to your profile");
        }

        // 3. Experience (25%)
        int expCount = (profile.getExperience() != null) ? profile.getExperience().size() : 0;
        int expScore = Math.min(100, expCount * 50);
        sections.put("experience", expScore);
        if (expScore < 100) {
            recommendations.add("Add past work experience entries");
        }

        // 4. Education (15%)
        int eduCount = (profile.getEducation() != null) ? profile.getEducation().size() : 0;
        int eduScore = (eduCount > 0) ? 100 : 0;
        sections.put("education", eduScore);
        if (eduScore < 100) {
            recommendations.add("Add educational degrees or certifications");
        }

        // 5. Projects (15%)
        int projCount = (profile.getProjects() != null) ? profile.getProjects().size() : 0;
        int projScore = (projCount > 0) ? 100 : 0;
        sections.put("projects", projScore);
        if (projScore < 100) {
            recommendations.add("Add personal or open-source project highlights");
        }

        // Overall weighted average
        double totalWeighted = (contactScore * 0.20) + (skillScore * 0.25) + (expScore * 0.25) + (eduScore * 0.15) + (projScore * 0.15);
        int overallScore = (int) Math.round(totalWeighted);

        return ProfileCompletenessResponse.builder()
                .score(overallScore)
                .sections(sections)
                .missingRecommendations(recommendations)
                .build();
    }
}
