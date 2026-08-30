package com.dotfield.service;

import com.dotfield.dto.JobMatchResponse;
import com.dotfield.entity.Job;
import com.dotfield.entity.Profile;
import com.dotfield.exception.ResourceNotFoundException;
import com.dotfield.matching.*;
import com.dotfield.repository.JobRepository;
import com.dotfield.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobMatchingService {

    private final CurrentUserService currentUserService;
    private final JobRepository jobRepository;

    private final JobRequirementExtractor requirementExtractor;
    private final SkillMatcher skillMatcher;
    private final ExperienceMatcher experienceMatcher;
    private final EducationMatcher educationMatcher;
    private final LocationMatcher locationMatcher;

    private final MatchScoreCalculator scoreCalculator;
    private final MatchExplanationBuilder explanationBuilder;

    @Transactional(readOnly = true)
    public JobMatchResponse analyzeJob(Long jobId) {
        Profile profile = currentUserService.getCurrentUserProfile();

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        JobRequirements requirements = requirementExtractor.extract(job);

        SkillMatcher.SkillResult skillResult = skillMatcher.match(profile, requirements);
        ExperienceMatcher.ExperienceResult expResult = experienceMatcher.match(profile, requirements);
        EducationMatcher.EducationResult eduResult = educationMatcher.match(profile, requirements);
        LocationMatcher.LocationResult locResult = locationMatcher.match(profile, requirements);

        MatchScoreCalculator.ScoreResult scoreResult = scoreCalculator.calculate(
                skillResult.score(),
                expResult.score(),
                eduResult.score(),
                locResult.score()
        );

        MatchExplanationBuilder.ExplanationResult explanations = explanationBuilder.buildExplanations(
                skillResult,
                expResult,
                eduResult,
                locResult
        );

        log.info("Analyzed match for Job ID: {} and Profile ID: {} -> Score: {}, Category: {}",
                jobId, profile.getId(), scoreResult.overallScore(), scoreResult.matchCategory());

        return JobMatchResponse.builder()
                .jobId(job.getId())
                .profileId(profile.getId())
                .overallScore(scoreResult.overallScore())
                .matchCategory(scoreResult.matchCategory())
                .skillScore(skillResult.score())
                .experienceScore(expResult.score())
                .educationScore(eduResult.score())
                .locationScore(locResult.score())
                .matchedRequiredSkills(skillResult.matchedRequiredSkills())
                .missingRequiredSkills(skillResult.missingRequiredSkills())
                .matchedPreferredSkills(skillResult.matchedPreferredSkills())
                .missingPreferredSkills(skillResult.missingPreferredSkills())
                .experienceAnalysis(expResult.analysis())
                .educationAnalysis(eduResult.analysis())
                .locationAnalysis(locResult.analysis())
                .strengths(explanations.strengths())
                .gaps(explanations.gaps())
                .build();
    }

}
