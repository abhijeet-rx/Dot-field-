package com.dotfield.matching;

import com.dotfield.entity.Experience;
import com.dotfield.entity.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Component
public class ExperienceMatcher {

    public record ExperienceResult(
            Integer score, // Null if UNKNOWN
            String status, // NOT_SPECIFIED, UNKNOWN, MEETS_REQUIREMENT, BELOW_REQUIREMENT
            String analysis
    ) {}

    public ExperienceResult match(Profile profile, JobRequirements requirements) {
        Integer minReqYears = requirements.getMinimumExperienceYears();

        if (minReqYears == null) {
            return new ExperienceResult(
                    null,
                    "NOT_SPECIFIED",
                    "Job specifies no minimum experience requirement."
            );
        }

        List<Experience> expList = (profile != null && profile.getExperience() != null)
                ? profile.getExperience()
                : List.of();

        if (expList.isEmpty()) {
            return new ExperienceResult(
                    null,
                    "UNKNOWN",
                    "Candidate profile contains no work experience records to evaluate required " + minReqYears + " years."
            );
        }

        double totalDays = 0;
        double techDays = 0;
        boolean hasTechEntries = false;

        String reqTech = requirements.getExperienceTechnology();
        String reqTechLower = reqTech != null ? reqTech.toLowerCase(Locale.ROOT) : null;

        for (Experience exp : expList) {
            LocalDate start = exp.getStartDate();
            if (start != null) {
                LocalDate end = exp.getEndDate() != null ? exp.getEndDate() : LocalDate.now();
                if (!end.isBefore(start)) {
                    long duration = ChronoUnit.DAYS.between(start, end);
                    totalDays += duration;

                    if (reqTechLower != null) {
                        String role = exp.getRole() != null ? exp.getRole().toLowerCase(Locale.ROOT) : "";
                        String desc = exp.getDescription() != null ? exp.getDescription().toLowerCase(Locale.ROOT) : "";
                        if (role.contains(reqTechLower) || desc.contains(reqTechLower)) {
                            techDays += duration;
                            hasTechEntries = true;
                        }
                    }
                }
            }
        }

        double totalYears = totalDays / 365.25;

        // Handling Technology-Specific Experience
        if (reqTechLower != null) {
            if (hasTechEntries) {
                double techYears = techDays / 365.25;
                if (techYears >= minReqYears) {
                    return new ExperienceResult(
                            100,
                            "MEETS_REQUIREMENT",
                            String.format("Candidate has %.1f years of %s experience, meeting the required %d years.", techYears, capitalize(reqTech), minReqYears)
                    );
                } else {
                    int score = (int) Math.round((techYears / minReqYears) * 100.0);
                    score = Math.min(100, Math.max(0, score));
                    return new ExperienceResult(
                            score,
                            "BELOW_REQUIREMENT",
                            String.format("Candidate has %.1f years of %s experience, below the required %d years.", techYears, capitalize(reqTech), minReqYears)
                    );
                }
            } else {
                // Technology-specific experience duration cannot be confirmed from structured data
                return new ExperienceResult(
                        null,
                        "UNKNOWN",
                        String.format("Candidate profile has %.1f years of total experience, but technology-specific experience duration for '%s' cannot be verified from structured experience entries.", totalYears, capitalize(reqTech))
                );
            }
        }

        // General Experience Requirement
        if (totalYears >= minReqYears) {
            return new ExperienceResult(
                    100,
                    "MEETS_REQUIREMENT",
                    String.format("Candidate has %.1f years of total experience, meeting the required %d years.", totalYears, minReqYears)
            );
        } else {
            int score = (int) Math.round((totalYears / minReqYears) * 100.0);
            score = Math.min(100, Math.max(0, score));
            return new ExperienceResult(
                    score,
                    "BELOW_REQUIREMENT",
                    String.format("Candidate has %.1f years of total experience, below the required %d years.", totalYears, minReqYears)
            );
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

}
