package com.dotfield.tailoring;

import com.dotfield.dto.*;
import com.dotfield.entity.Job;
import com.dotfield.entity.Profile;
import com.dotfield.matching.JobRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ResumeTailoringEngine {

    private final ResumeKeywordSelector keywordSelector;
    private final ResumeExperiencePrioritizer experiencePrioritizer;
    private final ResumeSectionBuilder sectionBuilder;
    private final ResumeSummaryGenerator summaryGenerator;

    public TailoredResumeResponse tailor(Profile profile, Job job, JobRequirements requirements) {
        ResumeKeywordSelector.KeywordResult keywordResult = keywordSelector.selectKeywords(profile, requirements);

        TailoredSkillsResponse skills = sectionBuilder.buildSkills(profile, keywordResult);
        List<TailoredExperienceResponse> experience = experiencePrioritizer.prioritizeExperience(
                profile != null ? profile.getExperience() : null,
                keywordResult.matchedKeywords()
        );
        List<TailoredEducationResponse> education = sectionBuilder.buildEducation(profile, requirements);
        List<TailoredProjectResponse> projects = sectionBuilder.buildProjects(profile, keywordResult);
        List<TailoredLinkResponse> links = sectionBuilder.buildLinks(profile);

        String summary = summaryGenerator.generateSummary(profile, skills.getPrimary());

        List<String> emphasizedSkills = new ArrayList<>(skills.getPrimary());
        List<String> emphasizedExperiences = experience.stream()
                .filter(TailoredExperienceResponse::isEmphasized)
                .map(e -> e.getRole() + (e.getCompany() != null ? " at " + e.getCompany() : ""))
                .toList();

        List<String> emphasizedProjects = projects.stream()
                .filter(TailoredProjectResponse::isEmphasized)
                .map(TailoredProjectResponse::getName)
                .toList();

        String notes = String.format("Emphasized %d primary skills, %d experience entries, and %d projects.",
                emphasizedSkills.size(), emphasizedExperiences.size(), emphasizedProjects.size());

        TailoringAnalysisResponse analysis = TailoringAnalysisResponse.builder()
                .emphasizedSkills(emphasizedSkills)
                .emphasizedExperiences(emphasizedExperiences)
                .emphasizedProjects(emphasizedProjects)
                .matchedKeywords(new ArrayList<>(keywordResult.matchedKeywords()))
                .unusedJobKeywords(new ArrayList<>(keywordResult.unusedJobKeywords()))
                .tailoringNotes(notes)
                .build();

        return TailoredResumeResponse.builder()
                .jobId(job != null ? job.getId() : null)
                .profileId(profile != null ? profile.getId() : null)
                .summary(summary)
                .skills(skills)
                .experience(experience)
                .education(education)
                .projects(projects)
                .links(links)
                .tailoringAnalysis(analysis)
                .build();
    }

}
