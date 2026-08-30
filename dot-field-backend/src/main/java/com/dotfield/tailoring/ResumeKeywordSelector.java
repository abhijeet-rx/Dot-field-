package com.dotfield.tailoring;

import com.dotfield.entity.Experience;
import com.dotfield.entity.Profile;
import com.dotfield.entity.Project;
import com.dotfield.entity.Skill;
import com.dotfield.matching.JobRequirements;
import com.dotfield.matching.SkillNormalizationUtil;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ResumeKeywordSelector {

    public record KeywordResult(
            Set<String> matchedKeywords,
            Set<String> unusedJobKeywords,
            Set<String> requiredKeywords,
            Set<String> preferredKeywords,
            Set<String> candidateNormalizedSkills
    ) {}

    public KeywordResult selectKeywords(Profile profile, JobRequirements requirements) {
        Set<String> candidateRawTerms = new HashSet<>();

        if (profile != null) {
            if (profile.getSkills() != null) {
                for (Skill skill : profile.getSkills()) {
                    if (skill.getName() != null && !skill.getName().isBlank()) {
                        candidateRawTerms.add(skill.getName());
                    }
                }
            }
            if (profile.getProjects() != null) {
                for (Project project : profile.getProjects()) {
                    if (project.getTechnologies() != null) {
                        for (String tech : project.getTechnologies()) {
                            if (tech != null && !tech.isBlank()) {
                                candidateRawTerms.add(tech);
                            }
                        }
                    }
                }
            }
            if (profile.getExperience() != null) {
                for (Experience exp : profile.getExperience()) {
                    if (exp.getRole() != null && !exp.getRole().isBlank()) {
                        candidateRawTerms.add(exp.getRole());
                    }
                }
            }
        }

        Set<String> candidateNormalizedSkills = SkillNormalizationUtil.normalizeSet(candidateRawTerms);

        Set<String> reqSkills = requirements != null && requirements.getRequiredSkills() != null
                ? SkillNormalizationUtil.normalizeSet(requirements.getRequiredSkills())
                : Collections.emptySet();

        Set<String> prefSkills = requirements != null && requirements.getPreferredSkills() != null
                ? SkillNormalizationUtil.normalizeSet(requirements.getPreferredSkills())
                : Collections.emptySet();

        Set<String> allJobKeywords = new LinkedHashSet<>();
        allJobKeywords.addAll(reqSkills);
        allJobKeywords.addAll(prefSkills);

        if (requirements != null) {
            if (requirements.getExperienceTechnology() != null && !requirements.getExperienceTechnology().isBlank()) {
                String normExpTech = SkillNormalizationUtil.normalize(requirements.getExperienceTechnology());
                if (normExpTech != null) {
                    allJobKeywords.add(normExpTech);
                }
            }
            if (requirements.getRequiredEducationField() != null && !requirements.getRequiredEducationField().isBlank()) {
                String normEduField = SkillNormalizationUtil.normalize(requirements.getRequiredEducationField());
                if (normEduField != null) {
                    allJobKeywords.add(normEduField);
                }
            }
        }

        Set<String> matchedKeywords = new LinkedHashSet<>();
        Set<String> unusedKeywords = new LinkedHashSet<>();

        for (String jobKeyword : allJobKeywords) {
            boolean matched = candidateNormalizedSkills.contains(jobKeyword);

            if (!matched && profile != null && profile.getExperience() != null) {
                String termLower = jobKeyword.toLowerCase(Locale.ROOT);
                for (Experience exp : profile.getExperience()) {
                    String role = exp.getRole() != null ? exp.getRole().toLowerCase(Locale.ROOT) : "";
                    String desc = exp.getDescription() != null ? exp.getDescription().toLowerCase(Locale.ROOT) : "";
                    if (role.contains(termLower) || desc.contains(termLower)) {
                        matched = true;
                        break;
                    }
                }
            }

            if (matched) {
                matchedKeywords.add(jobKeyword);
            } else {
                unusedKeywords.add(jobKeyword);
            }
        }

        return new KeywordResult(
                Collections.unmodifiableSet(matchedKeywords),
                Collections.unmodifiableSet(unusedKeywords),
                Collections.unmodifiableSet(reqSkills),
                Collections.unmodifiableSet(prefSkills),
                Collections.unmodifiableSet(candidateNormalizedSkills)
        );
    }

}
