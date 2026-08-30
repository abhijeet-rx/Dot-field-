package com.dotfield.tailoring;

import com.dotfield.dto.*;
import com.dotfield.entity.Education;
import com.dotfield.entity.Profile;
import com.dotfield.entity.Project;
import com.dotfield.entity.Skill;
import com.dotfield.matching.DegreeLevel;
import com.dotfield.matching.JobRequirements;
import com.dotfield.matching.SkillNormalizationUtil;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ResumeSectionBuilder {

    public TailoredSkillsResponse buildSkills(Profile profile, ResumeKeywordSelector.KeywordResult keywordResult) {
        if (profile == null || profile.getSkills() == null || profile.getSkills().isEmpty()) {
            return TailoredSkillsResponse.builder()
                    .primary(Collections.emptyList())
                    .secondary(Collections.emptyList())
                    .build();
        }

        Set<String> matchedReq = keywordResult.requiredKeywords();
        Set<String> matchedPref = keywordResult.preferredKeywords();

        List<String> primaryReq = new ArrayList<>();
        List<String> primaryPref = new ArrayList<>();
        List<String> secondary = new ArrayList<>();

        Set<String> seenNormalizedPrimary = new HashSet<>();

        for (Skill skill : profile.getSkills()) {
            if (skill.getName() == null || skill.getName().isBlank()) {
                continue;
            }
            String rawName = skill.getName().trim();
            String normName = SkillNormalizationUtil.normalize(rawName);

            if (normName == null) continue;

            if (matchedReq.contains(normName)) {
                if (seenNormalizedPrimary.add(normName)) {
                    primaryReq.add(rawName);
                }
            } else if (matchedPref.contains(normName)) {
                if (seenNormalizedPrimary.add(normName)) {
                    primaryPref.add(rawName);
                }
            } else {
                secondary.add(rawName);
            }
        }

        List<String> primary = new ArrayList<>();
        primary.addAll(primaryReq);
        primary.addAll(primaryPref);

        return TailoredSkillsResponse.builder()
                .primary(Collections.unmodifiableList(primary))
                .secondary(Collections.unmodifiableList(secondary))
                .build();
    }

    public List<TailoredProjectResponse> buildProjects(Profile profile, ResumeKeywordSelector.KeywordResult keywordResult) {
        if (profile == null || profile.getProjects() == null || profile.getProjects().isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> reqSkills = keywordResult.requiredKeywords();
        Set<String> prefSkills = keywordResult.preferredKeywords();
        Set<String> allMatched = keywordResult.matchedKeywords();

        List<ProjectRank> ranks = new ArrayList<>();
        List<Project> projects = profile.getProjects();

        for (int i = 0; i < projects.size(); i++) {
            Project project = projects.get(i);
            int matchedReqCount = 0;
            int matchedPrefCount = 0;
            int otherMatchedCount = 0;

            Set<String> projTechNorm = new HashSet<>();
            if (project.getTechnologies() != null) {
                for (String tech : project.getTechnologies()) {
                    String norm = SkillNormalizationUtil.normalize(tech);
                    if (norm != null) {
                        projTechNorm.add(norm);
                    }
                }
            }

            Set<String> matchedProjKeywords = new LinkedHashSet<>();

            for (String kw : allMatched) {
                boolean isMatch = projTechNorm.contains(kw) ||
                        SkillNormalizationUtil.containsKeyword(project.getDescription(), kw) ||
                        SkillNormalizationUtil.containsKeyword(project.getName(), kw);

                if (isMatch) {
                    matchedProjKeywords.add(kw);
                    if (reqSkills.contains(kw)) {
                        matchedReqCount++;
                    } else if (prefSkills.contains(kw)) {
                        matchedPrefCount++;
                    } else {
                        otherMatchedCount++;
                    }
                }
            }

            int score = (matchedReqCount * 3) + (matchedPrefCount * 2) + (otherMatchedCount * 1);

            ranks.add(new ProjectRank(i, project, score, new ArrayList<>(matchedProjKeywords)));
        }

        // Primary sort: score descending, Secondary sort: original index ascending (stable)
        ranks.sort((r1, r2) -> {
            int cmp = Integer.compare(r2.score, r1.score);
            if (cmp != 0) return cmp;
            return Integer.compare(r1.originalIndex, r2.originalIndex);
        });

        List<TailoredProjectResponse> result = new ArrayList<>();
        for (ProjectRank rank : ranks) {
            Project p = rank.project;
            result.add(TailoredProjectResponse.builder()
                    .id(p.getId())
                    .name(p.getName())
                    .description(p.getDescription())
                    .githubUrl(p.getGithubUrl())
                    .liveUrl(p.getLiveUrl())
                    .technologies(p.getTechnologies() != null ? new ArrayList<>(p.getTechnologies()) : Collections.emptyList())
                    .projectScore(rank.score)
                    .emphasized(rank.score > 0)
                    .matchingKeywords(rank.matchingKeywords)
                    .build());
        }

        return Collections.unmodifiableList(result);
    }

    public List<TailoredEducationResponse> buildEducation(Profile profile, JobRequirements requirements) {
        if (profile == null || profile.getEducation() == null || profile.getEducation().isEmpty()) {
            return Collections.emptyList();
        }

        DegreeLevel reqLevel = requirements != null ? requirements.getRequiredEducationLevel() : null;
        String reqField = requirements != null && requirements.getRequiredEducationField() != null
                ? requirements.getRequiredEducationField().toLowerCase(Locale.ROOT)
                : null;

        List<TailoredEducationResponse> result = new ArrayList<>();

        for (Education edu : profile.getEducation()) {
            boolean emphasized = false;

            if (reqField != null && edu.getFieldOfStudy() != null) {
                if (edu.getFieldOfStudy().toLowerCase(Locale.ROOT).contains(reqField)) {
                    emphasized = true;
                }
            }

            if (reqLevel != null && edu.getDegree() != null) {
                DegreeLevel candLevel = parseDegreeLevel(edu.getDegree() + " " + (edu.getFieldOfStudy() != null ? edu.getFieldOfStudy() : ""));
                if (candLevel.getLevel() >= reqLevel.getLevel()) {
                    emphasized = true;
                }
            }

            result.add(TailoredEducationResponse.builder()
                    .id(edu.getId())
                    .institution(edu.getInstitution())
                    .degree(edu.getDegree())
                    .fieldOfStudy(edu.getFieldOfStudy())
                    .startDate(edu.getStartDate())
                    .endDate(edu.getEndDate())
                    .grade(edu.getGrade())
                    .emphasized(emphasized)
                    .build());
        }

        return Collections.unmodifiableList(result);
    }

    public List<TailoredLinkResponse> buildLinks(Profile profile) {
        if (profile == null) {
            return Collections.emptyList();
        }

        List<TailoredLinkResponse> links = new ArrayList<>();

        if (profile.getLinkedinUrl() != null && !profile.getLinkedinUrl().isBlank()) {
            links.add(TailoredLinkResponse.builder()
                    .type("LinkedIn")
                    .url(profile.getLinkedinUrl().trim())
                    .build());
        }
        if (profile.getGithubUrl() != null && !profile.getGithubUrl().isBlank()) {
            links.add(TailoredLinkResponse.builder()
                    .type("GitHub")
                    .url(profile.getGithubUrl().trim())
                    .build());
        }
        if (profile.getPortfolioUrl() != null && !profile.getPortfolioUrl().isBlank()) {
            links.add(TailoredLinkResponse.builder()
                    .type("Portfolio")
                    .url(profile.getPortfolioUrl().trim())
                    .build());
        }

        return Collections.unmodifiableList(links);
    }

    private DegreeLevel parseDegreeLevel(String text) {
        if (text == null) return DegreeLevel.UNKNOWN;
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

    private record ProjectRank(int originalIndex, Project project, int score, List<String> matchingKeywords) {}

}
