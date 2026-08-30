package com.dotfield.tailoring;

import com.dotfield.dto.TailoredResumeResponse;
import com.dotfield.entity.*;
import com.dotfield.matching.JobRequirements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ResumeTailoringEngineTest {

    private ResumeTailoringEngine engine;

    @BeforeEach
    void setUp() {
        ResumeKeywordSelector keywordSelector = new ResumeKeywordSelector();
        ResumeExperiencePrioritizer experiencePrioritizer = new ResumeExperiencePrioritizer();
        ResumeSectionBuilder sectionBuilder = new ResumeSectionBuilder();
        ResumeSummaryGenerator summaryGenerator = new ResumeSummaryGenerator();

        engine = new ResumeTailoringEngine(keywordSelector, experiencePrioritizer, sectionBuilder, summaryGenerator);
    }

    // ──────────────────────────────────────────────────────────────
    // Adversarial Anti-Fabrication Tests
    // ──────────────────────────────────────────────────────────────

    @Test
    void test1_missingSkills_neverInsertedIntoTailoredSkills() {
        Profile profile = Profile.builder()
                .skills(List.of(
                        Skill.builder().name("Java").build(),
                        Skill.builder().name("Spring Boot").build()
                ))
                .build();

        Job job = Job.builder().id(1L).title("Backend Engineer").build();
        JobRequirements reqs = JobRequirements.builder()
                .requiredSkills(Set.of("java", "spring boot", "kubernetes", "aws"))
                .build();

        TailoredResumeResponse response = engine.tailor(profile, job, reqs);

        assertFalse(response.getSkills().getPrimary().contains("Kubernetes"));
        assertFalse(response.getSkills().getPrimary().contains("kubernetes"));
        assertFalse(response.getSkills().getPrimary().contains("AWS"));
        assertFalse(response.getSkills().getPrimary().contains("aws"));
        assertFalse(response.getSkills().getSecondary().contains("Kubernetes"));
        assertFalse(response.getSkills().getSecondary().contains("AWS"));

        assertTrue(response.getTailoringAnalysis().getUnusedJobKeywords().contains("kubernetes"));
        assertTrue(response.getTailoringAnalysis().getUnusedJobKeywords().contains("aws"));
    }

    @Test
    void test2_jobTitleInflation_candidateRoleUnchanged() {
        Profile profile = Profile.builder()
                .experience(List.of(
                        Experience.builder()
                                .role("Software Engineer")
                                .company("Tech Corp")
                                .build()
                ))
                .build();

        Job job = Job.builder().id(1L).title("Senior Backend Engineer").build();
        JobRequirements reqs = JobRequirements.builder().requiredSkills(Set.of("java")).build();

        TailoredResumeResponse response = engine.tailor(profile, job, reqs);

        assertEquals("Software Engineer", response.getExperience().get(0).getRole());
        assertNotEquals("Senior Backend Engineer", response.getExperience().get(0).getRole());
    }

    @Test
    void test3_unsupportedTechnology_notAddedToExperienceDescription() {
        Profile profile = Profile.builder()
                .experience(List.of(
                        Experience.builder()
                                .role("Developer")
                                .description("Worked on APIs.")
                                .build()
                ))
                .build();

        Job job = Job.builder().id(1L).title("Java Engineer").build();
        JobRequirements reqs = JobRequirements.builder().requiredSkills(Set.of("spring boot")).build();

        TailoredResumeResponse response = engine.tailor(profile, job, reqs);

        assertEquals("Worked on APIs.", response.getExperience().get(0).getDescription());
        assertFalse(response.getExperience().get(0).getDescription().contains("Spring Boot"));
    }

    @Test
    void test4_fakeMetrics_neverGeneratedInOutput() {
        Profile profile = Profile.builder()
                .experience(List.of(
                        Experience.builder()
                                .role("Developer")
                                .description("Developed backend applications.")
                                .build()
                ))
                .build();

        Job job = Job.builder().id(1L).title("Developer").build();
        JobRequirements reqs = JobRequirements.builder().requiredSkills(Set.of("java")).build();

        TailoredResumeResponse response = engine.tailor(profile, job, reqs);

        if (response.getSummary() != null) {
            assertFalse(response.getSummary().contains("40%"));
            assertFalse(response.getSummary().contains("performance"));
        }
        assertFalse(response.getExperience().get(0).getDescription().contains("40%"));
        assertFalse(response.getExperience().get(0).getDescription().contains("improved"));
    }

    @Test
    void test5_fakeAchievements_neverGeneratedInOutput() {
        Profile profile = Profile.builder()
                .experience(List.of(
                        Experience.builder()
                                .role("Developer")
                                .description("Worked on a web application.")
                                .build()
                ))
                .build();

        Job job = Job.builder().id(1L).title("Tech Lead").build();
        JobRequirements reqs = JobRequirements.builder().requiredSkills(Set.of("java")).build();

        TailoredResumeResponse response = engine.tailor(profile, job, reqs);

        assertFalse(response.getExperience().get(0).getDescription().contains("Led a team"));
        assertFalse(response.getExperience().get(0).getDescription().contains("5 engineers"));
    }

    @Test
    void test6_fakeProjects_neverAppearedInResponse() {
        Profile profile = Profile.builder()
                .projects(List.of(
                        Project.builder().id(10L).name("Web Dashboard").technologies(List.of("React")).build()
                ))
                .build();

        Job job = Job.builder().id(1L).title("Machine Learning Engineer").build();
        JobRequirements reqs = JobRequirements.builder().requiredSkills(Set.of("python", "tensorflow")).build();

        TailoredResumeResponse response = engine.tailor(profile, job, reqs);

        assertEquals(1, response.getProjects().size());
        assertEquals("Web Dashboard", response.getProjects().get(0).getName());
    }

    // ──────────────────────────────────────────────────────────────
    // Determinism & Edge Case Tests
    // ──────────────────────────────────────────────────────────────

    @Test
    void testDeterminism_sameInputProducesIdenticalOutput() {
        Profile profile = Profile.builder()
                .id(1L)
                .name("Alice")
                .skills(List.of(Skill.builder().name("Java").build()))
                .experience(List.of(Experience.builder().role("Developer").company("Acme").build()))
                .projects(List.of(Project.builder().name("Project A").technologies(List.of("Java")).build()))
                .build();

        Job job = Job.builder().id(100L).title("Java Engineer").build();
        JobRequirements reqs = JobRequirements.builder().requiredSkills(Set.of("java")).build();

        TailoredResumeResponse result1 = engine.tailor(profile, job, reqs);
        TailoredResumeResponse result2 = engine.tailor(profile, job, reqs);

        assertEquals(result1, result2);
    }

    @Test
    void testEmptyAndPartialProfile_doesNotCrash_summaryIsNull() {
        Profile emptyProfile = Profile.builder().id(1L).name("Bob").build();
        Job job = Job.builder().id(200L).title("Software Engineer").build();
        JobRequirements reqs = JobRequirements.builder().requiredSkills(Set.of("java")).build();

        TailoredResumeResponse response = engine.tailor(emptyProfile, job, reqs);

        assertNotNull(response);
        assertEquals(200L, response.getJobId());
        assertEquals(1L, response.getProfileId());
        assertNull(response.getSummary()); // No invented Software developer role!
        assertTrue(response.getSkills().getPrimary().isEmpty());
        assertTrue(response.getSkills().getSecondary().isEmpty());
        assertTrue(response.getExperience().isEmpty());
        assertTrue(response.getEducation().isEmpty());
        assertTrue(response.getProjects().isEmpty());
        assertTrue(response.getLinks().isEmpty());
    }

}
