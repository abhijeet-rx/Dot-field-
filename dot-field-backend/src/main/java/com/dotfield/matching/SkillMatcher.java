package com.dotfield.matching;

import com.dotfield.entity.Profile;
import com.dotfield.entity.Project;
import com.dotfield.entity.Skill;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class SkillMatcher {

    public record SkillResult(
            Integer score, // Null if UNKNOWN
            Set<String> matchedRequiredSkills,
            Set<String> missingRequiredSkills,
            Set<String> matchedPreferredSkills,
            Set<String> missingPreferredSkills
    ) {}

    public SkillResult match(Profile profile, JobRequirements requirements) {
        Set<String> candidateRawSkills = new HashSet<>();

        if (profile != null) {
            if (profile.getSkills() != null) {
                for (Skill skill : profile.getSkills()) {
                    if (skill.getName() != null) {
                        candidateRawSkills.add(skill.getName());
                    }
                }
            }
            if (profile.getProjects() != null) {
                for (Project project : profile.getProjects()) {
                    if (project.getTechnologies() != null) {
                        for (String tech : project.getTechnologies()) {
                            if (tech != null && !tech.isBlank()) {
                                candidateRawSkills.add(tech);
                            }
                        }
                    }
                }
            }
        }

        Set<String> candidateSkills = SkillNormalizationUtil.normalizeSet(candidateRawSkills);
        Set<String> reqSkills = SkillNormalizationUtil.normalizeSet(requirements.getRequiredSkills());
        Set<String> prefSkills = SkillNormalizationUtil.normalizeSet(requirements.getPreferredSkills());

        Set<String> matchedRequired = new HashSet<>(reqSkills);
        matchedRequired.retainAll(candidateSkills);

        Set<String> missingRequired = new HashSet<>(reqSkills);
        missingRequired.removeAll(candidateSkills);

        Set<String> matchedPreferred = new HashSet<>(prefSkills);
        matchedPreferred.retainAll(candidateSkills);

        Set<String> missingPreferred = new HashSet<>(prefSkills);
        missingPreferred.removeAll(candidateSkills);

        boolean hasReq = !reqSkills.isEmpty();
        boolean hasPref = !prefSkills.isEmpty();

        Integer score = null;

        if (hasReq && hasPref) {
            double reqScore = ((double) matchedRequired.size() / reqSkills.size()) * 100.0;
            double prefScore = ((double) matchedPreferred.size() / prefSkills.size()) * 100.0;
            score = (int) Math.round((reqScore * 0.70) + (prefScore * 0.30));
        } else if (hasReq) {
            score = (int) Math.round(((double) matchedRequired.size() / reqSkills.size()) * 100.0);
        } else if (hasPref) {
            score = (int) Math.round(((double) matchedPreferred.size() / prefSkills.size()) * 100.0);
        }

        return new SkillResult(
                score,
                Collections.unmodifiableSet(matchedRequired),
                Collections.unmodifiableSet(missingRequired),
                Collections.unmodifiableSet(matchedPreferred),
                Collections.unmodifiableSet(missingPreferred)
        );
    }

}
