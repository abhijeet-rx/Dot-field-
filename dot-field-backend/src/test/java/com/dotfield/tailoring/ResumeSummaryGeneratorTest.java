package com.dotfield.tailoring;

import com.dotfield.entity.Education;
import com.dotfield.entity.Experience;
import com.dotfield.entity.Profile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResumeSummaryGeneratorTest {

    private ResumeSummaryGenerator summaryGenerator;

    @BeforeEach
    void setUp() {
        summaryGenerator = new ResumeSummaryGenerator();
    }

    @Test
    void generateSummary_usesVerifiedProfileFacts() {
        Profile profile = Profile.builder()
                .name("John Doe")
                .experience(List.of(
                        Experience.builder()
                                .role("Backend Engineer")
                                .company("Acme Inc")
                                .startDate(LocalDate.of(2021, 1, 1))
                                .build()
                ))
                .education(List.of(
                        Education.builder()
                                .fieldOfStudy("Computer Science")
                                .build()
                ))
                .build();

        List<String> primarySkills = List.of("Java", "Spring Boot", "PostgreSQL");

        String summary = summaryGenerator.generateSummary(profile, primarySkills);

        assertNotNull(summary);
        assertTrue(summary.contains("Backend Engineer"));
        assertTrue(summary.contains("Java, Spring Boot, PostgreSQL"));
        assertTrue(summary.contains("Acme Inc"));

        // Anti-fabrication check: no claims of "40% performance improvement" or "seniority"
        assertFalse(summary.contains("40%"));
        assertFalse(summary.contains("senior"));
    }

    @Test
    void generateSummary_minimalProfile_returnsSafeSummary() {
        Profile profile = Profile.builder().build();

        String summary = summaryGenerator.generateSummary(profile, List.of());

        assertEquals("Software developer.", summary);
    }

}
