package com.dotfield.matching;

import com.dotfield.entity.Education;
import com.dotfield.entity.Profile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EducationMatcherTest {

    private EducationMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new EducationMatcher();
    }

    @Test
    void match_exactDegreeMatch_returns100() {
        Profile profile = Profile.builder()
                .education(List.of(
                        Education.builder()
                                .degree("Bachelor of Science")
                                .fieldOfStudy("Computer Science")
                                .build()
                ))
                .build();

        JobRequirements reqs = JobRequirements.builder()
                .requiredEducation("Bachelor")
                .build();

        EducationMatcher.EducationResult result = matcher.match(profile, reqs);

        assertEquals(100, result.score());
        assertEquals("MATCH", result.status());
    }

    @Test
    void match_higherDegreeMatch_returns75() {
        Profile profile = Profile.builder()
                .education(List.of(
                        Education.builder()
                                .degree("Master of Science")
                                .fieldOfStudy("Software Engineering")
                                .build()
                ))
                .build();

        JobRequirements reqs = JobRequirements.builder()
                .requiredEducation("Bachelor")
                .build();

        EducationMatcher.EducationResult result = matcher.match(profile, reqs);

        assertEquals(75, result.score());
        assertEquals("PARTIAL_MATCH", result.status());
    }

    @Test
    void match_noEducationRequirement_returnsNullScore() {
        Profile profile = Profile.builder().build();
        JobRequirements reqs = JobRequirements.builder().build();

        EducationMatcher.EducationResult result = matcher.match(profile, reqs);

        assertNull(result.score());
        assertEquals("NOT_REQUIRED", result.status());
    }

    @Test
    void match_emptyCandidateEducation_returnsNullScore() {
        Profile profile = Profile.builder().education(List.of()).build();
        JobRequirements reqs = JobRequirements.builder().requiredEducation("Bachelor").build();

        EducationMatcher.EducationResult result = matcher.match(profile, reqs);

        assertNull(result.score());
        assertEquals("UNKNOWN", result.status());
    }
}
