package com.dotfield.matching;

import com.dotfield.entity.Experience;
import com.dotfield.entity.Profile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExperienceMatcherTest {

    private ExperienceMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new ExperienceMatcher();
    }

    @Test
    void match_generalExperienceRequirement_exceedsRequirement_returns100() {
        Profile profile = Profile.builder()
                .experience(List.of(
                        Experience.builder()
                                .role("Software Engineer")
                                .startDate(LocalDate.now().minusYears(4))
                                .endDate(LocalDate.now())
                                .build()
                ))
                .build();

        JobRequirements reqs = JobRequirements.builder()
                .minimumExperienceYears(3)
                .build();

        ExperienceMatcher.ExperienceResult result = matcher.match(profile, reqs);

        assertEquals(100, result.score());
        assertEquals("MEETS_REQUIREMENT", result.status());
    }

    @Test
    void match_technologySpecificRequirement_candidateHasMatchingTechExperience_returns100() {
        Profile profile = Profile.builder()
                .experience(List.of(
                        Experience.builder()
                                .role("Java Developer")
                                .description("Building backend Java services")
                                .startDate(LocalDate.now().minusYears(3))
                                .endDate(LocalDate.now())
                                .build()
                ))
                .build();

        JobRequirements reqs = JobRequirements.builder()
                .minimumExperienceYears(2)
                .experienceTechnology("java")
                .build();

        ExperienceMatcher.ExperienceResult result = matcher.match(profile, reqs);

        assertEquals(100, result.score());
        assertEquals("MEETS_REQUIREMENT", result.status());
        assertTrue(result.analysis().contains("Java experience"));
    }

    @Test
    void match_technologySpecificRequirement_unconfirmableFromStructuredData_returnsUnknown() {
        Profile profile = Profile.builder()
                .experience(List.of(
                        Experience.builder()
                                .role("General Engineer")
                                .description("Worked on legacy systems")
                                .startDate(LocalDate.now().minusYears(5))
                                .endDate(LocalDate.now())
                                .build()
                ))
                .build();

        JobRequirements reqs = JobRequirements.builder()
                .minimumExperienceYears(3)
                .experienceTechnology("java")
                .build();

        ExperienceMatcher.ExperienceResult result = matcher.match(profile, reqs);

        assertNull(result.score());
        assertEquals("UNKNOWN", result.status());
        assertTrue(result.analysis().contains("cannot be verified"));
    }

    @Test
    void match_generalExperienceRequirement_belowRequirement_returnsProportionalScore() {
        Profile profile = Profile.builder()
                .experience(List.of(
                        Experience.builder()
                                .role("Engineer")
                                .startDate(LocalDate.now().minusYears(2))
                                .endDate(LocalDate.now())
                                .build()
                ))
                .build();

        JobRequirements reqs = JobRequirements.builder()
                .minimumExperienceYears(4)
                .build();

        ExperienceMatcher.ExperienceResult result = matcher.match(profile, reqs);

        assertEquals(50, result.score());
        assertEquals("BELOW_REQUIREMENT", result.status());
    }

    @Test
    void match_noRequirementSpecified_returnsNullScore() {
        Profile profile = Profile.builder().build();
        JobRequirements reqs = JobRequirements.builder().build();

        ExperienceMatcher.ExperienceResult result = matcher.match(profile, reqs);

        assertNull(result.score());
        assertEquals("NOT_SPECIFIED", result.status());
    }

    @Test
    void match_emptyCandidateExperience_returnsNullScore() {
        Profile profile = Profile.builder().experience(List.of()).build();
        JobRequirements reqs = JobRequirements.builder().minimumExperienceYears(2).build();

        ExperienceMatcher.ExperienceResult result = matcher.match(profile, reqs);

        assertNull(result.score());
        assertEquals("UNKNOWN", result.status());
    }
}
