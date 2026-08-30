package com.dotfield.matching;

import com.dotfield.entity.Job;
import com.dotfield.entity.RemoteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JobRequirementExtractorTest {

    private JobRequirementExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new JobRequirementExtractor();
    }

    @Test
    void extract_extractsRequiredAndPreferredSkillsAndTechExperienceAndEducation() {
        Job job = Job.builder()
                .title("Senior Java Engineer")
                .location("Bangalore, India")
                .remoteType(RemoteType.HYBRID)
                .description("""
                        Required Qualifications:
                        - Must have 3+ years of Java experience.
                        - Strong experience with Spring Boot and PostgreSQL.
                        - Bachelor degree in Computer Science required.
                        
                        Preferred Qualifications:
                        - Nice to have experience with Kubernetes and AWS.
                        """)
                .build();

        JobRequirements reqs = extractor.extract(job);

        assertNotNull(reqs);
        assertTrue(reqs.getRequiredSkills().contains("java"));
        assertTrue(reqs.getRequiredSkills().contains("spring boot"));
        assertTrue(reqs.getRequiredSkills().contains("postgresql"));
        assertTrue(reqs.getPreferredSkills().contains("kubernetes"));
        assertTrue(reqs.getPreferredSkills().contains("aws"));
        assertEquals(3, reqs.getMinimumExperienceYears());
        assertEquals("java", reqs.getExperienceTechnology());
        assertEquals(DegreeLevel.BACHELOR, reqs.getRequiredEducationLevel());
        assertEquals("computer science", reqs.getRequiredEducationField());
        assertEquals("Bangalore, India", reqs.getLocation());
        assertEquals(RemoteType.HYBRID, reqs.getRemoteType());
    }

    @Test
    void extract_generalExperienceRequirement_technologyIsNull() {
        Job job = Job.builder()
                .title("Software Engineer")
                .description("Requires 5+ years of professional experience.")
                .build();

        JobRequirements reqs = extractor.extract(job);

        assertEquals(5, reqs.getMinimumExperienceYears());
        assertNull(reqs.getExperienceTechnology());
    }

    @Test
    void extract_handlesTitleAndDescriptionSeparatelyWithoutOffsetCorruption() {
        Job job = Job.builder()
                .title("Lead React Engineer")
                .description("Preferred: AWS, Docker")
                .build();

        JobRequirements reqs = extractor.extract(job);

        assertTrue(reqs.getRequiredSkills().contains("react"));
        assertTrue(reqs.getPreferredSkills().contains("aws"));
        assertTrue(reqs.getPreferredSkills().contains("docker"));
        assertFalse(reqs.getPreferredSkills().contains("react"));
    }

    @Test
    void extract_handlesEmptyDescription() {
        Job job = Job.builder()
                .title("Software Engineer")
                .description("")
                .build();

        JobRequirements reqs = extractor.extract(job);

        assertNotNull(reqs);
        assertTrue(reqs.getRequiredSkills().isEmpty());
        assertTrue(reqs.getPreferredSkills().isEmpty());
        assertNull(reqs.getMinimumExperienceYears());
        assertNull(reqs.getRequiredEducationLevel());
    }
}
