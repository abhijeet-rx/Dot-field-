package com.dotfield.matching;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SkillNormalizationUtilTest {

    @Test
    void normalize_trimsAndLowercasesAndMapsAliases() {
        assertEquals("javascript", SkillNormalizationUtil.normalize("  JS  "));
        assertEquals("typescript", SkillNormalizationUtil.normalize("TS"));
        assertEquals("react", SkillNormalizationUtil.normalize("ReactJS"));
        assertEquals("postgresql", SkillNormalizationUtil.normalize("Postgres"));
        assertEquals("kubernetes", SkillNormalizationUtil.normalize("k8s"));
        assertEquals("spring boot", SkillNormalizationUtil.normalize("springboot"));
        assertEquals("java", SkillNormalizationUtil.normalize("Java"));
        assertNull(SkillNormalizationUtil.normalize("  "));
        assertNull(SkillNormalizationUtil.normalize(null));
    }

    @Test
    void normalize_enforcesStrictNonEquivalence() {
        assertNotEquals(SkillNormalizationUtil.normalize("Java"), SkillNormalizationUtil.normalize("JavaScript"));
        assertNotEquals(SkillNormalizationUtil.normalize("React"), SkillNormalizationUtil.normalize("React Native"));
        assertNotEquals(SkillNormalizationUtil.normalize("Spring"), SkillNormalizationUtil.normalize("Spring Boot"));
    }

    @Test
    void normalizeSet_deduplicatesAndIgnoresNulls() {
        Set<String> result = SkillNormalizationUtil.normalizeSet(List.of("JS", "javascript", " ReactJS ", "", "Java"));
        assertEquals(Set.of("javascript", "react", "java"), result);
    }
}
