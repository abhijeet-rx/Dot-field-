package com.dotfield.tailoring;

import com.dotfield.dto.TailoredExperienceResponse;
import com.dotfield.entity.Experience;
import com.dotfield.matching.SkillNormalizationUtil;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ResumeExperiencePrioritizer {

    public List<TailoredExperienceResponse> prioritizeExperience(List<Experience> experiences, Set<String> matchedKeywords) {
        if (experiences == null || experiences.isEmpty()) {
            return Collections.emptyList();
        }

        // Sort experience primary order: reverse chronological order (latest end date/null first, then latest start date)
        List<Experience> sortedExperiences = new ArrayList<>(experiences);
        sortedExperiences.sort((e1, e2) -> {
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

        List<TailoredExperienceResponse> result = new ArrayList<>();

        for (Experience exp : sortedExperiences) {
            String originalDesc = exp.getDescription();
            String prioritizedDesc = prioritizeBullets(originalDesc, matchedKeywords);

            Set<String> expKeywords = findMatchingKeywordsInExperience(exp, matchedKeywords);
            boolean emphasized = !expKeywords.isEmpty();

            result.add(TailoredExperienceResponse.builder()
                    .id(exp.getId())
                    .company(exp.getCompany())
                    .role(exp.getRole())
                    .description(prioritizedDesc)
                    .startDate(exp.getStartDate())
                    .endDate(exp.getEndDate())
                    .emphasized(emphasized)
                    .matchingKeywords(new ArrayList<>(expKeywords))
                    .build());
        }

        return result;
    }

    private String prioritizeBullets(String description, Set<String> matchedKeywords) {
        if (description == null || description.isBlank() || matchedKeywords == null || matchedKeywords.isEmpty()) {
            return description;
        }

        String[] lines = description.split("\r?\n");
        if (lines.length <= 1) {
            return description;
        }

        // Split and rank lines based on keyword presence while preserving exact line content
        List<LineRank> ranks = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int matchCount = 0;
            String lineLower = line.toLowerCase(Locale.ROOT);
            for (String kw : matchedKeywords) {
                if (lineLower.contains(kw.toLowerCase(Locale.ROOT))) {
                    matchCount++;
                }
            }
            ranks.add(new LineRank(i, line, matchCount));
        }

        // Sort lines primarily by matchCount descending, secondarily by original index ascending (stable)
        ranks.sort((r1, r2) -> {
            int cmp = Integer.compare(r2.matchCount, r1.matchCount);
            if (cmp != 0) return cmp;
            return Integer.compare(r1.originalIndex, r2.originalIndex);
        });

        return ranks.stream()
                .map(r -> r.text)
                .collect(Collectors.joining("\n"));
    }

    private Set<String> findMatchingKeywordsInExperience(Experience exp, Set<String> matchedKeywords) {
        if (matchedKeywords == null || matchedKeywords.isEmpty() || exp == null) {
            return Collections.emptySet();
        }

        String roleLower = exp.getRole() != null ? exp.getRole().toLowerCase(Locale.ROOT) : "";
        String descLower = exp.getDescription() != null ? exp.getDescription().toLowerCase(Locale.ROOT) : "";

        Set<String> found = new LinkedHashSet<>();
        for (String kw : matchedKeywords) {
            String kwLower = kw.toLowerCase(Locale.ROOT);
            if (roleLower.contains(kwLower) || descLower.contains(kwLower)) {
                found.add(kw);
            }
        }
        return found;
    }

    private record LineRank(int originalIndex, String text, int matchCount) {}

}
