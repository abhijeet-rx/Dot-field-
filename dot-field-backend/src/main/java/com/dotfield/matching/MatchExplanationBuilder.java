package com.dotfield.matching;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MatchExplanationBuilder {

    public record ExplanationResult(
            List<String> strengths,
            List<String> gaps
    ) {}

    public ExplanationResult buildExplanations(
            SkillMatcher.SkillResult skillResult,
            ExperienceMatcher.ExperienceResult expResult,
            EducationMatcher.EducationResult eduResult,
            LocationMatcher.LocationResult locResult
    ) {
        List<String> strengths = new ArrayList<>();
        List<String> gaps = new ArrayList<>();

        // Skill Strengths & Gaps
        if (skillResult != null) {
            for (String skill : skillResult.matchedRequiredSkills()) {
                strengths.add("Matches required skill: " + capitalize(skill));
            }
            for (String skill : skillResult.matchedPreferredSkills()) {
                strengths.add("Matches preferred skill: " + capitalize(skill));
            }
            for (String skill : skillResult.missingRequiredSkills()) {
                gaps.add("Missing required skill: " + capitalize(skill));
            }
            for (String skill : skillResult.missingPreferredSkills()) {
                gaps.add("Missing preferred skill: " + capitalize(skill));
            }
        }

        // Experience Strengths & Gaps
        if (expResult != null) {
            if ("MEETS_REQUIREMENT".equals(expResult.status())) {
                strengths.add(expResult.analysis());
            } else if ("BELOW_REQUIREMENT".equals(expResult.status())) {
                gaps.add(expResult.analysis());
            }
        }

        // Education Strengths & Gaps
        if (eduResult != null) {
            if ("MATCH".equals(eduResult.status()) || "PARTIAL_MATCH".equals(eduResult.status())) {
                strengths.add(eduResult.analysis());
            } else if ("MISMATCH".equals(eduResult.status())) {
                gaps.add(eduResult.analysis());
            }
        }

        // Location Strengths & Gaps
        if (locResult != null) {
            if ("REMOTE_COMPATIBLE".equals(locResult.status()) || "LOCATION_MATCHED".equals(locResult.status())) {
                strengths.add(locResult.analysis());
            } else if ("LOCATION_MISMATCH".equals(locResult.status())) {
                gaps.add(locResult.analysis());
            }
        }

        return new ExplanationResult(strengths, gaps);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

}
