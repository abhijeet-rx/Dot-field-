package com.dotfield.tailoring;

import com.dotfield.entity.Education;
import com.dotfield.entity.Experience;
import com.dotfield.entity.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class ResumeSummaryGenerator {

    public String generateSummary(Profile profile, List<String> primarySkills) {
        if (profile == null) {
            return null;
        }

        Experience latestExp = getLatestExperience(profile.getExperience());
        Education topEdu = getTopEducation(profile.getEducation());

        if (latestExp == null || latestExp.getRole() == null || latestExp.getRole().isBlank()) {
            return null;
        }

        String role = latestExp.getRole().trim();

        StringBuilder summaryBuilder = new StringBuilder();
        summaryBuilder.append(role);

        if (primarySkills != null && !primarySkills.isEmpty()) {
            List<String> topSkills = primarySkills.subList(0, Math.min(3, primarySkills.size()));
            summaryBuilder.append(" with experience in ").append(String.join(", ", topSkills));
        }

        summaryBuilder.append(".");

        if (latestExp.getCompany() != null && !latestExp.getCompany().isBlank()) {
            summaryBuilder.append(" Previously worked at ").append(latestExp.getCompany().trim()).append(".");
        } else if (topEdu != null && topEdu.getFieldOfStudy() != null && !topEdu.getFieldOfStudy().isBlank()) {
            summaryBuilder.append(" Education in ").append(topEdu.getFieldOfStudy().trim()).append(".");
        }

        return summaryBuilder.toString();
    }

    private Experience getLatestExperience(List<Experience> experiences) {
        if (experiences == null || experiences.isEmpty()) {
            return null;
        }

        List<Experience> copy = new ArrayList<>(experiences);
        copy.sort((e1, e2) -> {
            LocalDate end1 = e1.getEndDate();
            LocalDate end2 = e2.getEndDate();
            if (end1 == null && end2 != null) return -1;
            if (end1 != null && end2 == null) return 1;
            if (end1 != null && end2 != null) {
                int cmp = end2.compareTo(end1);
                if (cmp != 0) return cmp;
            }
            LocalDate start1 = e1.getStartDate() != null ? e1.getStartDate() : LocalDate.MIN;
            LocalDate start2 = e2.getStartDate() != null ? e2.getStartDate() : LocalDate.MIN;
            return start2.compareTo(start1);
        });
        return copy.get(0);
    }

    private Education getTopEducation(List<Education> educationList) {
        if (educationList == null || educationList.isEmpty()) {
            return null;
        }
        return educationList.get(0);
    }

}
