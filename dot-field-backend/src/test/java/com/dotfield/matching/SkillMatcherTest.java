package com.dotfield.matching;

import com.dotfield.entity.Profile;
import com.dotfield.entity.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SkillMatcherTest {

    private SkillMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new SkillMatcher();
    }

    @Test
    void match_allRequiredAndPreferredMatched_returns100Score() {
        Profile profile = Profile.builder()
                .skills(List.of(
                        Skill.builder().name("Java").build(),
                        Skill.builder().name("Spring Boot").build(),
                        Skill.builder().name("Docker").build(),
                        Skill.builder().name("AWS").build()
                ))
                .build();

        JobRequirements reqs = JobRequirements.builder()
                .requiredSkills(Set.of("java", "spring boot"))
                .preferredSkills(Set.of("docker", "aws"))
                .build();

        SkillMatcher.SkillResult result = matcher.match(profile, reqs);

        assertEquals(100, result.score());
        assertEquals(Set.of("java", "spring boot"), result.matchedRequiredSkills());
        assertTrue(result.missingRequiredSkills().isEmpty());
        assertEquals(Set.of("docker", "aws"), result.matchedPreferredSkills());
        assertTrue(result.missingPreferredSkills().isEmpty());
    }

    @Test
    void match_partialRequiredMatch_returnsWeightedScore() {
        Profile profile = Profile.builder()
                .skills(List.of(
                        Skill.builder().name("Java").build()
                ))
                .build();

        JobRequirements reqs = JobRequirements.builder()
                .requiredSkills(Set.of("java", "spring boot"))
                .preferredSkills(Set.of("docker"))
                .build();

        SkillMatcher.SkillResult result = matcher.match(profile, reqs);

        // 50% required match * 0.70 + 0% preferred match * 0.30 = 35% -> 35
        assertEquals(35, result.score());
        assertEquals(Set.of("java"), result.matchedRequiredSkills());
        assertEquals(Set.of("spring boot"), result.missingRequiredSkills());
        assertEquals(Set.of("docker"), result.missingPreferredSkills());
    }

    @Test
    void match_noSkillsRequiredOrPreferred_returnsNullScore() {
        Profile profile = Profile.builder()
                .skills(List.of(Skill.builder().name("Photoshop").build()))
                .build();

        JobRequirements reqs = JobRequirements.builder().build();

        SkillMatcher.SkillResult result = matcher.match(profile, reqs);

        assertNull(result.score());
    }

    @Test
    void match_unrelatedCandidateSkills_doNotIncreaseScore() {
        Profile profile = Profile.builder()
                .skills(List.of(
                        Skill.builder().name("Photoshop").build(),
                        Skill.builder().name("Premiere Pro").build()
                ))
                .build();

        JobRequirements reqs = JobRequirements.builder()
                .requiredSkills(Set.of("java", "spring boot"))
                .build();

        SkillMatcher.SkillResult result = matcher.match(profile, reqs);

        assertEquals(0, result.score());
    }
}
