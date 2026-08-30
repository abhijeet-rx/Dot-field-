package com.dotfield.matching;

import com.dotfield.entity.Job;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JobRequirementExtractor {

    private static final Set<String> KNOWN_VOCABULARY = Set.of(
            "java", "python", "javascript", "typescript", "react", "angular", "vue",
            "spring", "spring boot", "docker", "kubernetes", "aws", "gcp", "azure",
            "postgresql", "mysql", "mongodb", "redis", "git", "ci/cd", "kafka",
            "rest", "graphql", "microservices", "c++", "c#", "go", "golang", "rust",
            "ruby", "rails", "node", "nodejs", "html", "css", "tailwind", "sql"
    );

    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile("(\\d+)\\+?\\s*(?:years|yrs)", Pattern.CASE_INSENSITIVE);

    public JobRequirements extract(Job job) {
        if (job == null) {
            return new JobRequirements();
        }

        String description = job.getDescription() != null ? job.getDescription() : "";
        String title = job.getTitle() != null ? job.getTitle() : "";
        String combinedText = (title + " " + description).toLowerCase(Locale.ROOT);

        Set<String> requiredSkills = new HashSet<>();
        Set<String> preferredSkills = new HashSet<>();

        // Section splitting for required vs preferred skills
        String descLower = description.toLowerCase(Locale.ROOT);
        int prefIndex = indexOfAny(descLower, "preferred", "nice to have", "bonus", "plus", "desired", "preferred qualifications");
        int reqIndex = indexOfAny(descLower, "required", "must have", "mandatory", "essential", "minimum qualifications", "requirements");

        if (prefIndex != -1 && reqIndex != -1 && prefIndex > reqIndex) {
            String reqText = combinedText.substring(0, title.length() + prefIndex + 1);
            String prefText = combinedText.substring(title.length() + prefIndex);

            extractSkillsFromText(reqText, requiredSkills);
            extractSkillsFromText(prefText, preferredSkills);
            preferredSkills.removeAll(requiredSkills);
        } else if (prefIndex != -1 && reqIndex == -1) {
            String reqText = combinedText.substring(0, title.length() + prefIndex + 1);
            String prefText = combinedText.substring(title.length() + prefIndex);

            extractSkillsFromText(reqText, requiredSkills);
            extractSkillsFromText(prefText, preferredSkills);
            preferredSkills.removeAll(requiredSkills);
        } else {
            extractSkillsFromText(combinedText, requiredSkills);
        }

        // Minimum Experience Extraction
        Integer minExp = extractMinimumExperience(combinedText);

        // Required Education Extraction
        String education = extractEducationRequirement(combinedText);

        return JobRequirements.builder()
                .requiredSkills(SkillNormalizationUtil.normalizeSet(requiredSkills))
                .preferredSkills(SkillNormalizationUtil.normalizeSet(preferredSkills))
                .minimumExperienceYears(minExp)
                .requiredEducation(education)
                .location(job.getLocation())
                .remoteType(job.getRemoteType())
                .build();
    }

    private void extractSkillsFromText(String text, Set<String> targetSet) {
        for (String vocabToken : KNOWN_VOCABULARY) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(vocabToken) + "\\b", Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(text).find()) {
                targetSet.add(vocabToken);
            }
        }
    }

    private Integer extractMinimumExperience(String text) {
        Matcher matcher = EXPERIENCE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private String extractEducationRequirement(String text) {
        if (text.contains("phd") || text.contains("doctorate")) {
            return "PhD";
        }
        if (text.contains("master") || text.contains("m.s.") || text.contains("m.tech")) {
            return "Master";
        }
        if (text.contains("bachelor") || text.contains("b.s.") || text.contains("b.tech") || text.contains("b.e.") || text.contains("degree")) {
            return "Bachelor";
        }
        return null;
    }

    private int indexOfAny(String text, String... keywords) {
        int minPos = -1;
        for (String kw : keywords) {
            int pos = text.indexOf(kw);
            if (pos != -1 && (minPos == -1 || pos < minPos)) {
                minPos = pos;
            }
        }
        return minPos;
    }

}
