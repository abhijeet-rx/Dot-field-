package com.dotfield.matching;

import com.dotfield.entity.Education;
import com.dotfield.entity.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class EducationMatcher {

    public record EducationResult(
            Integer score, // Null if UNKNOWN
            String status, // NOT_REQUIRED, UNKNOWN, MATCH, PARTIAL_MATCH, MISMATCH
            String analysis
    ) {}

    public EducationResult match(Profile profile, JobRequirements requirements) {
        String reqEdu = requirements.getRequiredEducation();

        if (reqEdu == null || reqEdu.isBlank()) {
            return new EducationResult(
                    null,
                    "NOT_REQUIRED",
                    "Job specifies no education requirement."
            );
        }

        List<Education> eduList = (profile != null && profile.getEducation() != null)
                ? profile.getEducation()
                : List.of();

        if (eduList.isEmpty()) {
            return new EducationResult(
                    null,
                    "UNKNOWN",
                    "Candidate profile contains no education records to verify required " + reqEdu + " degree."
            );
        }

        String reqEduLower = reqEdu.toLowerCase(Locale.ROOT);
        boolean exactMatch = false;
        boolean partialMatch = false;

        for (Education edu : eduList) {
            String degree = edu.getDegree() != null ? edu.getDegree().toLowerCase(Locale.ROOT) : "";
            String field = edu.getFieldOfStudy() != null ? edu.getFieldOfStudy().toLowerCase(Locale.ROOT) : "";
            String combined = degree + " " + field;

            if (reqEduLower.contains("phd")) {
                if (combined.contains("phd") || combined.contains("doctorate")) {
                    exactMatch = true;
                }
            } else if (reqEduLower.contains("master")) {
                if (combined.contains("phd") || combined.contains("doctorate")) {
                    partialMatch = true; // Higher level
                } else if (combined.contains("master") || combined.contains("m.s.") || combined.contains("m.tech")) {
                    exactMatch = true;
                }
            } else if (reqEduLower.contains("bachelor") || reqEduLower.contains("degree")) {
                if (combined.contains("phd") || combined.contains("master") || combined.contains("m.s.")) {
                    partialMatch = true; // Higher level
                } else if (combined.contains("bachelor") || combined.contains("b.s.") || combined.contains("b.tech") || combined.contains("b.e.") || combined.contains("degree")) {
                    exactMatch = true;
                }
            }
        }

        if (exactMatch) {
            return new EducationResult(
                    100,
                    "MATCH",
                    "Candidate holds a degree matching the required " + reqEdu + " level."
            );
        } else if (partialMatch) {
            return new EducationResult(
                    75,
                    "PARTIAL_MATCH",
                    "Candidate holds an advanced degree or related field qualification."
            );
        } else {
            return new EducationResult(
                    0,
                    "MISMATCH",
                    "Candidate education does not match the required " + reqEdu + " level."
            );
        }
    }

}
