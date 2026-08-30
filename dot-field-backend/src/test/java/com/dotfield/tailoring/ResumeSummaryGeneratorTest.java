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
        assertFalse(summary.contains("Senior"));
    }

    @Test
    void generateSummary_emptyProfile_returnsNullWithoutFabricatedRole() {
        Profile profile = Profile.builder().build();

        String summary = summaryGenerator.generateSummary(profile, List.of());

        assertNull(summary);
    }

    @Test
    void generateSummary_noRoleInExperience_returnsNullWithoutFabricatedRole() {
        Profile profile = Profile.builder()
                .experience(List.of(
                        Experience.builder().company("Acme Inc").build()
                ))
                .build();

        String summary = summaryGenerator.generateSummary(profile, List.of("Java"));

        assertNull(summary);
    }

    @Test
    void generateSummary_doesNotInflateRoleToTargetJobTitle() {
        Profile profile = Profile.builder()
                .experience(List.of(
                        Experience.builder()
                                .role("Software Engineer")
                                .company("Tech Corp")
                                .build()
                ))
                .build();

        String summary = summaryGenerator.generateSummary(profile, List.of("Java"));

        assertTrue(summary.contains("Software Engineer"));
        assertFalse(summary.contains("Senior Backend Engineer"));
    }

}
