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

    private static final Pattern EXP_TECH_PATTERN = Pattern.compile(
            "(\\d+)\\+?\\s*(?:years|yrs)(?:\\s+of\\s+(?:experience\\s+in\\s+)?([a-zA-Z0-9#+.-]+))?",
            Pattern.CASE_INSENSITIVE
    );

    private static final List<String> NON_TECH_EXP_WORDS = List.of(
            "experience", "professional", "work", "software", "engineering", "industry", "relevant", "total"
    );

    public JobRequirements extract(Job job) {
        if (job == null) {
            return new JobRequirements();
        }

        String description = job.getDescription() != null ? job.getDescription() : "";
        String title = job.getTitle() != null ? job.getTitle() : "";

        Set<String> requiredSkills = new HashSet<>();
        Set<String> preferredSkills = new HashSet<>();

        // Extract skills from title into required
        extractSkillsFromText(title.toLowerCase(Locale.ROOT), requiredSkills);

        // Section splitting within description strictly using description offsets
        String descLower = description.toLowerCase(Locale.ROOT);
        int prefIndex = indexOfAny(descLower, "preferred", "nice to have", "bonus", "plus", "desired", "preferred qualifications");
        int reqIndex = indexOfAny(descLower, "required", "must have", "mandatory", "essential", "minimum qualifications", "requirements");

        String reqDescText;
        String prefDescText;

        if (prefIndex != -1) {
            if (reqIndex != -1 && reqIndex < prefIndex) {
                reqDescText = descLower.substring(reqIndex, prefIndex);
                prefDescText = descLower.substring(prefIndex);
            } else {
                reqDescText = descLower.substring(0, prefIndex);
                prefDescText = descLower.substring(prefIndex);
            }
        } else {
            reqDescText = descLower;
            prefDescText = "";
        }

        extractSkillsFromText(reqDescText, requiredSkills);
        extractSkillsFromText(prefDescText, preferredSkills);
        preferredSkills.removeAll(requiredSkills);

        // Experience Extraction
        String combinedText = (title + " " + description).toLowerCase(Locale.ROOT);
        ParsedExperience parsedExp = extractExperienceDetails(combinedText);

        // Education Extraction
        ParsedEducation parsedEdu = extractEducationDetails(combinedText);

        return JobRequirements.builder()
                .requiredSkills(SkillNormalizationUtil.normalizeSet(requiredSkills))
                .preferredSkills(SkillNormalizationUtil.normalizeSet(preferredSkills))
                .minimumExperienceYears(parsedExp.minYears())
                .experienceTechnology(parsedExp.technology())
                .requiredEducationLevel(parsedEdu.level())
                .requiredEducationField(parsedEdu.field())
                .requiredEducation(parsedEdu.summary())
                .location(job.getLocation())
                .remoteType(job.getRemoteType())
                .build();
    }

    private void extractSkillsFromText(String text, Set<String> targetSet) {
        if (text == null || text.isBlank()) return;
        for (String vocabToken : KNOWN_VOCABULARY) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(vocabToken) + "\\b", Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(text).find()) {
                targetSet.add(vocabToken);
            }
        }
    }

    private record ParsedExperience(Integer minYears, String technology) {}

    private ParsedExperience extractExperienceDetails(String text) {
        Matcher matcher = EXP_TECH_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                int years = Integer.parseInt(matcher.group(1));
                String rawTech = matcher.group(2);
                String normalizedTech = null;

                if (rawTech != null && !rawTech.isBlank()) {
                    String cleanTech = rawTech.trim().toLowerCase(Locale.ROOT);
                    if (!NON_TECH_EXP_WORDS.contains(cleanTech)) {
                        normalizedTech = SkillNormalizationUtil.normalize(cleanTech);
                    }
                }

                return new ParsedExperience(years, normalizedTech);
            } catch (NumberFormatException ignored) {
            }
        }
        return new ParsedExperience(null, null);
    }

    private record ParsedEducation(DegreeLevel level, String field, String summary) {}

    private ParsedEducation extractEducationDetails(String text) {
        DegreeLevel level = null;
        String summary = null;

        if (text.contains("phd") || text.contains("doctorate")) {
            level = DegreeLevel.DOCTORATE;
            summary = "PhD";
        } else if (text.contains("master") || text.contains("m.s.") || text.contains("m.tech")) {
            level = DegreeLevel.MASTER;
            summary = "Master";
        } else if (text.contains("bachelor") || text.contains("b.s.") || text.contains("b.tech") || text.contains("b.e.") || text.contains("degree")) {
            level = DegreeLevel.BACHELOR;
            summary = "Bachelor";
        } else if (text.contains("associate")) {
            level = DegreeLevel.ASSOCIATE;
            summary = "Associate";
        } else if (text.contains("high school") || text.contains("diploma")) {
            level = DegreeLevel.HIGH_SCHOOL;
            summary = "High School";
        }

        String field = null;
        if (text.contains("computer science") || text.contains("cs")) {
            field = "computer science";
        } else if (text.contains("information technology") || text.contains("it")) {
            field = "information technology";
        } else if (text.contains("software engineering")) {
            field = "software engineering";
        } else if (text.contains("electrical engineering")) {
            field = "electrical engineering";
        } else if (text.contains("mechanical engineering")) {
            field = "mechanical engineering";
        } else if (text.contains("engineering")) {
            field = "engineering";
        }

        if (level != null && field != null && summary != null) {
            summary = summary + " in " + capitalize(field);
        }

        return new ParsedEducation(level, field, summary);
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

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

}
