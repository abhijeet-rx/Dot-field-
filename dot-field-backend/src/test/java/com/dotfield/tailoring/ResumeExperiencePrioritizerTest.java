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
        String line1 = "Worked with documentation.";
        String line2 = "Built Java backend services.";
        String line3 = "Attended meetings.";

        Experience exp = Experience.builder()
                .id(1L)
                .role("Software Engineer")
                .company("Tech Corp")
                .description(line1 + "\n" + line2 + "\n" + line3)
                .build();

        List<TailoredExperienceResponse> result = prioritizer.prioritizeExperience(
                List.of(exp),
                Set.of("java")
        );

        assertEquals(1, result.size());
        TailoredExperienceResponse tailoredExp = result.get(0);
        assertTrue(tailoredExp.isEmphasized());

        String[] lines = tailoredExp.getDescription().split("\n");
        assertEquals(3, lines.length);
        assertEquals("Built Java backend services.", lines[0]);
        assertEquals("Worked with documentation.", lines[1]);
        assertEquals("Attended meetings.", lines[2]);
    }

    @Test
    void prioritizeExperience_javaNotMatchedInJavaScriptBullet() {
        String line1 = "Built frontend components in JavaScript.";

        Experience exp = Experience.builder()
                .id(1L)
                .role("Frontend Dev")
                .company("Web Co")
                .description(line1)
                .build();

        List<TailoredExperienceResponse> result = prioritizer.prioritizeExperience(
                List.of(exp),
                Set.of("java")
        );

        assertFalse(result.get(0).isEmphasized());
        assertTrue(result.get(0).getMatchingKeywords().isEmpty());
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
