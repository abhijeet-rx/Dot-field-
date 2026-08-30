package com.dotfield.tailoring;

import com.dotfield.entity.Experience;
import com.dotfield.entity.Profile;
import com.dotfield.entity.Skill;
import com.dotfield.matching.JobRequirements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ResumeKeywordSelectorTest {

    private ResumeKeywordSelector selector;

    @BeforeEach
    void setUp() {
        selector = new ResumeKeywordSelector();
    }

    @Test
    void selectKeywords_matchesNormalizedSkills() {
        Profile profile = Profile.builder()
                .skills(List.of(
                        Skill.builder().name("Java").build(),
                        Skill.builder().name("Spring Boot").build(),
                        Skill.builder().name("Postgres").build()
                ))
                .build();

        JobRequirements reqs = JobRequirements.builder()
                .requiredSkills(Set.of("java", "spring boot"))
                .preferredSkills(Set.of("postgresql", "aws"))
                .build();

        ResumeKeywordSelector.KeywordResult result = selector.selectKeywords(profile, reqs);

        assertTrue(result.matchedKeywords().contains("java"));
        assertTrue(result.matchedKeywords().contains("spring boot"));
        assertTrue(result.matchedKeywords().contains("postgresql"));
        assertTrue(result.unusedJobKeywords().contains("aws"));
    }

    @Test
    void selectKeywords_strictTechnologyMatching_javaNotJavaScript() {
        Profile profile = Profile.builder()
                .experience(List.of(
                        Experience.builder()
                                .role("Developer")
                                .description("Worked extensively with JavaScript.")
                                .build()
                ))
                .build();

        JobRequirements reqs = JobRequirements.builder()
                .requiredSkills(Set.of("java"))
                .build();

        ResumeKeywordSelector.KeywordResult result = selector.selectKeywords(profile, reqs);

        assertFalse(result.matchedKeywords().contains("java"));
        assertTrue(result.unusedJobKeywords().contains("java"));
    }

    @Test
    void selectKeywords_strictTechnologyMatching_reactNotReactNative() {
        Profile profile = Profile.builder()
                .experience(List.of(
                        Experience.builder()
                                .role("React Native Developer")
                                .description("Built cross-platform mobile apps using React Native.")
                                .build()
                ))
                .build();

        JobRequirements reqs = JobRequirements.builder()
                .requiredSkills(Set.of("react"))
                .build();

        ResumeKeywordSelector.KeywordResult result = selector.selectKeywords(profile, reqs);

        assertFalse(result.matchedKeywords().contains("react"));
        assertTrue(result.unusedJobKeywords().contains("react"));
    }

    @Test
    void selectKeywords_matchesExperienceTextSafely() {
        Profile profile = Profile.builder()
                .experience(List.of(
                        Experience.builder()
                                .role("Backend Developer")
                                .description("Worked extensively with Docker and Kubernetes")
                                .build()
                ))
                .build();

        JobRequirements reqs = JobRequirements.builder()
                .requiredSkills(Set.of("docker", "kubernetes", "python"))
                .build();

        ResumeKeywordSelector.KeywordResult result = selector.selectKeywords(profile, reqs);

        assertTrue(result.matchedKeywords().contains("docker"));
        assertTrue(result.matchedKeywords().contains("kubernetes"));
        assertTrue(result.unusedJobKeywords().contains("python"));
    }

}
