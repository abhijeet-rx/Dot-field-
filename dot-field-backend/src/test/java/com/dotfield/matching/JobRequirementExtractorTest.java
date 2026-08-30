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
    void extract_extractsRequiredAndPreferredSkillsAndExperienceAndEducation() {
        Job job = Job.builder()
                .title("Senior Java Developer")
                .location("Bangalore, India")
                .remoteType(RemoteType.HYBRID)
                .description("""
                        Required Qualifications:
                        - Must have 5+ years of experience in Java and Spring Boot.
                        - Strong experience with PostgreSQL and Docker.
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
        assertTrue(reqs.getRequiredSkills().contains("docker"));
        assertTrue(reqs.getPreferredSkills().contains("kubernetes"));
        assertTrue(reqs.getPreferredSkills().contains("aws"));
        assertEquals(5, reqs.getMinimumExperienceYears());
        assertEquals("Bachelor", reqs.getRequiredEducation());
        assertEquals("Bangalore, India", reqs.getLocation());
        assertEquals(RemoteType.HYBRID, reqs.getRemoteType());
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
        assertNull(reqs.getRequiredEducation());
    }
}
