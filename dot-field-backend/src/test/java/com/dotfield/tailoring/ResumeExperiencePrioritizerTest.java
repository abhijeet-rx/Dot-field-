package com.dotfield.tailoring;

import com.dotfield.dto.TailoredExperienceResponse;
import com.dotfield.entity.Experience;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ResumeExperiencePrioritizerTest {

    private ResumeExperiencePrioritizer prioritizer;

    @BeforeEach
    void setUp() {
        prioritizer = new ResumeExperiencePrioritizer();
    }

    @Test
    void prioritizeExperience_preservesReverseChronologicalOrder() {
        Experience exp1 = Experience.builder()
                .id(1L)
                .role("Junior Dev")
                .company("Company A")
                .startDate(LocalDate.of(2018, 1, 1))
                .endDate(LocalDate.of(2020, 1, 1))
                .description("Java development")
                .build();

        Experience exp2 = Experience.builder()
                .id(2L)
                .role("Senior Dev")
                .company("Company B")
                .startDate(LocalDate.of(2020, 2, 1))
                .endDate(null) // Current job
                .description("Spring Boot development")
                .build();

        List<TailoredExperienceResponse> result = prioritizer.prioritizeExperience(
                List.of(exp1, exp2),
                Set.of("java")
        );

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getId()); // Latest role first
        assertEquals(1L, result.get(1).getId());
    }

    @Test
    void prioritizeExperience_prioritizesMatchingBullets_withoutTextAlteration() {
        String desc = "Implemented CI/CD pipelines.\nDeveloped backend APIs using Spring Boot and PostgreSQL.\nCreated UI components in HTML.";

        Experience exp = Experience.builder()
                .id(1L)
                .role("Software Engineer")
                .company("Tech Corp")
                .description(desc)
                .build();

        List<TailoredExperienceResponse> result = prioritizer.prioritizeExperience(
                List.of(exp),
                Set.of("spring boot", "postgresql")
        );

        assertEquals(1, result.size());
        TailoredExperienceResponse tailoredExp = result.get(0);
        assertTrue(tailoredExp.isEmphasized());

        String[] lines = tailoredExp.getDescription().split("\n");
        assertEquals("Developed backend APIs using Spring Boot and PostgreSQL.", lines[0]);
    }

    @Test
    void prioritizeExperience_noMatches_notEmphasized() {
        Experience exp = Experience.builder()
                .id(1L)
                .role("Designer")
                .company("Studio")
                .description("Created UI mockups.")
                .build();

        List<TailoredExperienceResponse> result = prioritizer.prioritizeExperience(
                List.of(exp),
                Set.of("java", "spring boot")
        );

        assertFalse(result.get(0).isEmphasized());
        assertTrue(result.get(0).getMatchingKeywords().isEmpty());
    }

}
