package com.dotfield.tailoring;

import com.dotfield.dto.TailoredResumeResponse;
import com.dotfield.entity.Job;
import com.dotfield.entity.Profile;
import com.dotfield.exception.ResourceNotFoundException;
import com.dotfield.matching.JobRequirementExtractor;
import com.dotfield.matching.JobRequirements;
import com.dotfield.repository.JobRepository;
import com.dotfield.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeTailoringServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobRequirementExtractor requirementExtractor;

    @Mock
    private ResumeTailoringEngine tailoringEngine;

    private ResumeTailoringService service;

    @BeforeEach
    void setUp() {
        service = new ResumeTailoringService(profileRepository, jobRepository, requirementExtractor, tailoringEngine);
    }

    @Test
    void tailorResume_success() {
        Profile profile = Profile.builder().id(1L).name("Jane").build();
        Job job = Job.builder().id(10L).title("Backend Developer").build();
        JobRequirements reqs = JobRequirements.builder().build();
        TailoredResumeResponse mockResponse = TailoredResumeResponse.builder().jobId(10L).profileId(1L).build();

        when(profileRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(profile));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(requirementExtractor.extract(job)).thenReturn(reqs);
        when(tailoringEngine.tailor(profile, job, reqs)).thenReturn(mockResponse);

        TailoredResumeResponse response = service.tailorResume(10L);

        assertNotNull(response);
        assertEquals(10L, response.getJobId());
        assertEquals(1L, response.getProfileId());
    }

    @Test
    void tailorResume_jobNotFound_throwsResourceNotFoundException() {
        Profile profile = Profile.builder().id(1L).build();
        when(profileRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(profile));
        when(jobRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.tailorResume(999L));
    }

    @Test
    void tailorResume_profileNotFound_throwsResourceNotFoundException() {
        when(profileRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.tailorResume(10L));
    }

}
