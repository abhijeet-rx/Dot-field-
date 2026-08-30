package com.dotfield.service;

import com.dotfield.dto.JobMatchResponse;
import com.dotfield.entity.Job;
import com.dotfield.entity.Profile;
import com.dotfield.entity.RemoteType;
import com.dotfield.exception.ResourceNotFoundException;
import com.dotfield.matching.*;
import com.dotfield.repository.JobRepository;
import com.dotfield.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobMatchingServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobRequirementExtractor requirementExtractor;

    @Spy
    private SkillMatcher skillMatcher;

    @Spy
    private ExperienceMatcher experienceMatcher;

    @Spy
    private EducationMatcher educationMatcher;

    @Spy
    private LocationMatcher locationMatcher;

    @Spy
    private MatchScoreCalculator scoreCalculator;

    @Spy
    private MatchExplanationBuilder explanationBuilder;

    @InjectMocks
    private JobMatchingService jobMatchingService;

    private Profile sampleProfile;
    private Job sampleJob;

    @BeforeEach
    void setUp() {
        sampleProfile = Profile.builder()
                .id(1L)
                .name("Jane Candidate")
                .location("Bangalore, India")
                .build();

        sampleJob = Job.builder()
                .id(100L)
                .title("Backend Engineer")
                .company("Google")
                .location("Bangalore, India")
                .remoteType(RemoteType.HYBRID)
                .description("Required: Java, Spring Boot. 3+ years exp.")
                .build();
    }

    @Test
    void analyzeJob_success() {
        when(currentUserService.getCurrentUserProfile()).thenReturn(sampleProfile);
        when(jobRepository.findById(100L)).thenReturn(Optional.of(sampleJob));

        JobRequirements reqs = JobRequirements.builder()
                .requiredSkills(Set.of("java", "spring boot"))
                .minimumExperienceYears(3)
                .remoteType(RemoteType.HYBRID)
                .location("Bangalore, India")
                .build();

        when(requirementExtractor.extract(any(Job.class))).thenReturn(reqs);

        JobMatchResponse response = jobMatchingService.analyzeJob(100L);

        assertNotNull(response);
        assertEquals(100L, response.getJobId());
        assertEquals(1L, response.getProfileId());
        assertNotNull(response.getMatchCategory());
        verifyResultStructure(response);
    }

    @Test
    void analyzeJob_profileNotFound_throwsResourceNotFoundException() {
        when(currentUserService.getCurrentUserProfile()).thenThrow(new ResourceNotFoundException("Candidate profile not found"));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> jobMatchingService.analyzeJob(100L));

        assertEquals("Candidate profile not found", ex.getMessage());
    }

    @Test
    void analyzeJob_jobNotFound_throwsResourceNotFoundException() {
        when(currentUserService.getCurrentUserProfile()).thenReturn(sampleProfile);
        when(jobRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> jobMatchingService.analyzeJob(999L));

        assertEquals("Job not found with id: 999", ex.getMessage());
    }

    private void verifyResultStructure(JobMatchResponse response) {
        assertTrue(response.getOverallScore() >= 0 && response.getOverallScore() <= 100);
        assertNotNull(response.getStrengths());
        assertNotNull(response.getGaps());
    }
}
