package com.dotfield.tailoring;

import com.dotfield.dto.*;
import com.dotfield.entity.Education;
import com.dotfield.entity.Profile;
import com.dotfield.entity.Project;
import com.dotfield.entity.Skill;
import com.dotfield.matching.DegreeLevel;
import com.dotfield.matching.JobRequirements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ResumeSectionBuilderTest {

    private ResumeSectionBuilder sectionBuilder;

    @BeforeEach
    void setUp() {
        sectionBuilder = new ResumeSectionBuilder();
    }

    @Test
    void buildSkills_prioritizesRequiredThenPreferredThenOther() {
        Profile profile = Profile.builder()
                .skills(List.of(
                        Skill.builder().name("Git").build(),
                        Skill.builder().name("Java").build(),
                        Skill.builder().name("Spring Boot").build(),
                        Skill.builder().name("PostgreSQL").build()
                ))
                .build();

        ResumeKeywordSelector.KeywordResult kwResult = new ResumeKeywordSelector.KeywordResult(
                Set.of("java", "spring boot", "postgresql"),
                Set.of("aws"),
                Set.of("java"),
                Set.of("spring boot"),
                Set.of("git", "java", "spring boot", "postgresql")
        );

        TailoredSkillsResponse skills = sectionBuilder.buildSkills(profile, kwResult);

        assertEquals(List.of("Java", "Spring Boot", "PostgreSQL"), skills.getPrimary());
        assertEquals(List.of("Git"), skills.getSecondary());
        // Verify missing job skill AWS is not added
        assertFalse(skills.getPrimary().contains("AWS"));
        assertFalse(skills.getSecondary().contains("AWS"));
    }

    @Test
    void buildProjects_scoresAndOrdersByRelevanceFormula() {
        Project proj1 = Project.builder()
                .id(1L)
                .name("General Web App")
                .technologies(List.of("HTML", "CSS"))
                .build();

        Project proj2 = Project.builder()
                .id(2L)
                .name("Backend Service")
                .technologies(List.of("Java", "Spring Boot"))
                .build();

        Profile profile = Profile.builder()
                .projects(List.of(proj1, proj2))
                .build();

        ResumeKeywordSelector.KeywordResult kwResult = new ResumeKeywordSelector.KeywordResult(
                Set.of("java", "spring boot"),
                Set.of(),
                Set.of("java"), // req (3 pts)
                Set.of("spring boot"), // pref (2 pts)
                Set.of("html", "css", "java", "spring boot")
        );

        List<TailoredProjectResponse> projects = sectionBuilder.buildProjects(profile, kwResult);

        assertEquals(2, projects.size());
        assertEquals(2L, projects.get(0).getId()); // Score 5 comes before Score 0
        assertEquals(5, projects.get(0).getProjectScore());
        assertTrue(projects.get(0).isEmphasized());

        assertEquals(1L, projects.get(1).getId());
        assertEquals(0, projects.get(1).getProjectScore());
        assertFalse(projects.get(1).isEmphasized());
    }

    @Test
    void buildEducation_emphasizesMatchingDegreeOrField() {
        Education edu = Education.builder()
                .id(1L)
                .institution("State University")
                .degree("Bachelor of Science")
                .fieldOfStudy("Computer Science")
                .build();

        Profile profile = Profile.builder()
                .education(List.of(edu))
                .build();

        JobRequirements reqs = JobRequirements.builder()
                .requiredEducationLevel(DegreeLevel.BACHELOR)
                .requiredEducationField("computer science")
                .build();

        List<TailoredEducationResponse> education = sectionBuilder.buildEducation(profile, reqs);

        assertEquals(1, education.size());
        assertTrue(education.get(0).isEmphasized());
        assertEquals("Bachelor of Science", education.get(0).getDegree());
    }

    @Test
    void buildLinks_extractsProfileLinksWithoutModification() {
        Profile profile = Profile.builder()
                .linkedinUrl("https://linkedin.com/in/test")
                .githubUrl("https://github.com/test")
                .build();

        List<TailoredLinkResponse> links = sectionBuilder.buildLinks(profile);

        assertEquals(2, links.size());
        assertEquals("LinkedIn", links.get(0).getType());
        assertEquals("https://linkedin.com/in/test", links.get(0).getUrl());
        assertEquals("GitHub", links.get(1).getType());
        assertEquals("https://github.com/test", links.get(1).getUrl());
    }

}
