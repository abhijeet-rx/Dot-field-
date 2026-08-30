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
        DegreeLevel reqLevel = requirements.getRequiredEducationLevel();
        String reqField = requirements.getRequiredEducationField();
        String reqSummary = requirements.getRequiredEducation();

        if (reqLevel == null && (reqSummary == null || reqSummary.isBlank())) {
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
                    "Candidate profile contains no education records to verify required " +
                            (reqSummary != null ? reqSummary : "degree") + "."
            );
        }

        if (reqLevel == null) {
            reqLevel = DegreeLevel.BACHELOR; // Default fallback if summary string existed without level
        }

        DegreeLevel bestCandidateLevel = DegreeLevel.UNKNOWN;
        String bestDegreeTitle = "";
        String bestFieldTitle = "";

        boolean exactFieldMatch = false;
        boolean relatedFieldMatch = false;

        for (Education edu : eduList) {
            String degreeStr = edu.getDegree() != null ? edu.getDegree().trim() : "";
            String fieldStr = edu.getFieldOfStudy() != null ? edu.getFieldOfStudy().trim() : "";

            DegreeLevel candidateLevel = parseDegreeLevel(degreeStr + " " + fieldStr);
            if (candidateLevel.getLevel() > bestCandidateLevel.getLevel()) {
                bestCandidateLevel = candidateLevel;
                bestDegreeTitle = degreeStr;
                bestFieldTitle = fieldStr;
            }

            if (reqField != null && !reqField.isBlank()) {
                String fieldLower = fieldStr.toLowerCase(Locale.ROOT);
                String reqFieldLower = reqField.toLowerCase(Locale.ROOT);

                if (fieldLower.contains(reqFieldLower) || reqFieldLower.contains(fieldLower)) {
                    exactFieldMatch = true;
                } else if (isRelatedField(fieldLower, reqFieldLower)) {
                    relatedFieldMatch = true;
                }
            } else {
                exactFieldMatch = true;
            }
        }

        boolean levelMeetsOrExceeds = bestCandidateLevel.getLevel() >= reqLevel.getLevel();
        boolean levelExceeds = bestCandidateLevel.getLevel() > reqLevel.getLevel();
        boolean levelBelow = bestCandidateLevel.getLevel() < reqLevel.getLevel();

        String displayReq = reqSummary != null ? reqSummary : reqLevel.name();

        if (levelMeetsOrExceeds && exactFieldMatch) {
            return new EducationResult(
                    100,
                    "MATCH",
                    String.format("Candidate holds a %s degree in %s, matching the required %s.",
                            bestDegreeTitle, bestFieldTitle, displayReq)
            );
        } else if (levelExceeds || (levelMeetsOrExceeds && relatedFieldMatch)) {
            return new EducationResult(
                    75,
                    "PARTIAL_MATCH",
                    String.format("Candidate holds a %s in %s, providing an advanced degree level or related field qualification.",
                            bestDegreeTitle, bestFieldTitle)
            );
        } else {
            return new EducationResult(
                    0,
                    "MISMATCH",
                    String.format("Candidate education (%s in %s) does not meet the required %s level and field.",
                            bestDegreeTitle, bestFieldTitle, displayReq)
            );
        }
    }

    private DegreeLevel parseDegreeLevel(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("phd") || lower.contains("doctorate") || lower.contains("ph.d.")) {
            return DegreeLevel.DOCTORATE;
        }
        if (lower.contains("master") || lower.contains("m.s.") || lower.contains("m.tech") || lower.contains("m.b.a.") || lower.contains("mba")) {
            return DegreeLevel.MASTER;
        }
        if (lower.contains("bachelor") || lower.contains("b.s.") || lower.contains("b.tech") || lower.contains("b.e.") || lower.contains("degree")) {
            return DegreeLevel.BACHELOR;
        }
        if (lower.contains("associate") || lower.contains("a.s.") || lower.contains("a.a.")) {
            return DegreeLevel.ASSOCIATE;
        }
        if (lower.contains("high school") || lower.contains("diploma")) {
            return DegreeLevel.HIGH_SCHOOL;
        }
        return DegreeLevel.UNKNOWN;
    }

    private boolean isRelatedField(String candidateField, String requiredField) {
        if (requiredField.contains("computer science") || requiredField.contains("cs")) {
            return candidateField.contains("information technology") ||
                    candidateField.contains("software engineering") ||
                    candidateField.contains("computer engineering") ||
                    candidateField.contains("it");
        }
        if (requiredField.contains("engineering")) {
            return candidateField.contains("computer science") || candidateField.contains("technology");
        }
        return false;
    }

}
