package com.dotfield.service;

import com.dotfield.dto.*;
import com.dotfield.entity.*;
import com.dotfield.exception.BadRequestException;
import com.dotfield.exception.ConflictException;
import com.dotfield.exception.ResourceNotFoundException;
import com.dotfield.mapper.JobMapper;
import com.dotfield.repository.ApplicationRepository;
import com.dotfield.repository.JobRepository;
import com.dotfield.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private JobRepository jobRepository;

    @Spy
    private JobMapper jobMapper;

    @Mock
    private JobMatchingService jobMatchingService;

    @Spy
    private ApplicationStatusTransitionValidator statusTransitionValidator = new ApplicationStatusTransitionValidator();

    @InjectMocks
    private ApplicationService applicationService;

    private Profile sampleProfile;
    private Job sampleJob;
    private Application sampleApplication;

    @BeforeEach
    void setUp() {
        sampleProfile = Profile.builder()
                .id(1L)
                .name("John Candidate")
                .email("candidate@example.com")
                .build();

        sampleJob = Job.builder()
                .id(10L)
                .title("Senior Java Developer")
                .company("Acme Corp")
                .source("LINKEDIN")
                .status(JobStatus.SAVED)
                .build();

        sampleApplication = Application.builder()
                .id(100L)
                .profile(sampleProfile)
                .job(sampleJob)
                .status(ApplicationStatus.SAVED)
                .notes("Interesting role")
                .fitScore(85)
                .matchCategory("STRONG_MATCH")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("createApplication — Success creating application and persisting fit score snapshot")
    void createApplication_success() {
        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .jobId(10L)
                .status(ApplicationStatus.APPLIED)
                .notes("Applied via referral")
                .build();

        JobMatchResponse mockMatch = JobMatchResponse.builder().overallScore(85).matchCategory("STRONG_MATCH").build();
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(sampleProfile));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(sampleJob));
        when(applicationRepository.existsByProfileIdAndJobId(1L, 10L)).thenReturn(false);
        when(jobMatchingService.analyzeJob(10L)).thenReturn(mockMatch);
        when(applicationRepository.save(any(Application.class))).thenReturn(sampleApplication);

        ApplicationResponse response = applicationService.createApplication(1L, request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        verify(applicationRepository, times(1)).save(any(Application.class));
    }

    @Test
    @DisplayName("createApplication — Duplicate application throws ConflictException (409)")
    void createApplication_duplicate_throwsConflictException() {
        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .jobId(10L)
                .build();

        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(sampleProfile));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(sampleJob));
        when(applicationRepository.existsByProfileIdAndJobId(1L, 10L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> applicationService.createApplication(1L, request));
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    @DisplayName("updateStatus — Valid transition SAVED -> APPLIED sets appliedAt timestamp")
    void updateStatus_validTransition_setsAppliedAt() {
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(sampleProfile));
        when(applicationRepository.findByIdAndProfileId(100L, 1L)).thenReturn(Optional.of(sampleApplication));
        when(applicationRepository.save(any(Application.class))).thenReturn(sampleApplication);

        ApplicationResponse response = applicationService.updateStatus(1L, 100L, ApplicationStatus.APPLIED);

        assertNotNull(response);
        assertEquals(ApplicationStatus.APPLIED, sampleApplication.getStatus());
        assertNotNull(sampleApplication.getAppliedAt());
    }

    @Test
    @DisplayName("updateStatus — Invalid transition SAVED -> OFFER throws BadRequestException (400)")
    void updateStatus_invalidTransitionSavedToOffer_throwsBadRequestException() {
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(sampleProfile));
        when(applicationRepository.findByIdAndProfileId(100L, 1L)).thenReturn(Optional.of(sampleApplication));

        assertThrows(BadRequestException.class, () -> applicationService.updateStatus(1L, 100L, ApplicationStatus.OFFER));
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    @DisplayName("updateStatus — Transition out of terminal state REJECTED -> INTERVIEW throws BadRequestException (400)")
    void updateStatus_terminalStateRejected_throwsBadRequestException() {
        sampleApplication.setStatus(ApplicationStatus.REJECTED);
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(sampleProfile));
        when(applicationRepository.findByIdAndProfileId(100L, 1L)).thenReturn(Optional.of(sampleApplication));

        assertThrows(BadRequestException.class, () -> applicationService.updateStatus(1L, 100L, ApplicationStatus.INTERVIEW));
    }

    @Test
    @DisplayName("updateStatus — Subsequent valid transition preserves original appliedAt timestamp")
    void updateStatus_preservesOriginalAppliedAtTimestamp() {
        LocalDateTime originalAppliedAt = LocalDateTime.now().minusDays(5);
        sampleApplication.setStatus(ApplicationStatus.APPLIED);
        sampleApplication.setAppliedAt(originalAppliedAt);

        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(sampleProfile));
        when(applicationRepository.findByIdAndProfileId(100L, 1L)).thenReturn(Optional.of(sampleApplication));
        when(applicationRepository.save(any(Application.class))).thenReturn(sampleApplication);

        applicationService.updateStatus(1L, 100L, ApplicationStatus.INTERVIEW);

        assertEquals(ApplicationStatus.INTERVIEW, sampleApplication.getStatus());
        assertEquals(originalAppliedAt, sampleApplication.getAppliedAt());
    }

    @Test
    @DisplayName("getAnalytics — Computes counts, rates, and fit score average from persisted snapshots without N+1 calls")
    void getAnalytics_success() {
        Application app1 = Application.builder().id(1L).profile(sampleProfile).job(sampleJob).status(ApplicationStatus.APPLIED).fitScore(80).build();
        Application app2 = Application.builder().id(2L).profile(sampleProfile).job(sampleJob).status(ApplicationStatus.INTERVIEW).fitScore(90).build();

        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(sampleProfile));
        when(applicationRepository.findAllByProfileId(1L)).thenReturn(List.of(app1, app2));

        ApplicationAnalyticsResponse analytics = applicationService.getAnalytics(1L);

        assertNotNull(analytics);
        assertEquals(2, analytics.getTotalApplications());
        assertEquals(1, analytics.getStatusCounts().get(ApplicationStatus.APPLIED));
        assertEquals(1, analytics.getStatusCounts().get(ApplicationStatus.INTERVIEW));
        assertEquals(50.0, analytics.getResponseRate());
        assertEquals(50.0, analytics.getInterviewRate());
        assertEquals(85.0, analytics.getAverageFitScore());

        verify(jobMatchingService, never()).analyzeJob(anyLong());
    }
}
