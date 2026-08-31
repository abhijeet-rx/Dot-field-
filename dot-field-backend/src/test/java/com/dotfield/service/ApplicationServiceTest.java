package com.dotfield.service;

import com.dotfield.dto.*;
import com.dotfield.entity.*;
import com.dotfield.exception.BadRequestException;
import com.dotfield.exception.ConflictException;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

    // ==============================
    // Issue 1: Creation status validation
    // ==============================

    @Test
    @DisplayName("createApplication(status=SAVED) — Success")
    void createApplication_statusSaved_success() {
        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .jobId(10L)
                .status(ApplicationStatus.SAVED)
                .notes("Bookmarked")
                .build();

        JobMatchResponse mockMatch = JobMatchResponse.builder().overallScore(85).matchCategory("STRONG_MATCH").build();
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(sampleProfile));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(sampleJob));
        when(applicationRepository.existsByProfileIdAndJobId(1L, 10L)).thenReturn(false);
        when(jobMatchingService.analyzeJob(10L)).thenReturn(mockMatch);
        when(applicationRepository.save(any(Application.class))).thenReturn(sampleApplication);

        ApplicationResponse response = applicationService.createApplication(1L, request);

        assertNotNull(response);
        verify(applicationRepository, times(1)).save(any(Application.class));
    }

    @Test
    @DisplayName("createApplication(status=APPLIED) — Success with fit score snapshot")
    void createApplication_statusApplied_success() {
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
    @DisplayName("createApplication(status=INTERVIEW) — 400 Bad Request")
    void createApplication_statusInterview_throwsBadRequest() {
        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .jobId(10L)
                .status(ApplicationStatus.INTERVIEW)
                .build();

        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(sampleProfile));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(sampleJob));

        assertThrows(BadRequestException.class, () -> applicationService.createApplication(1L, request));
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    @DisplayName("createApplication(status=OFFER) — 400 Bad Request")
    void createApplication_statusOffer_throwsBadRequest() {
        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .jobId(10L)
                .status(ApplicationStatus.OFFER)
                .build();

        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(sampleProfile));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(sampleJob));

        assertThrows(BadRequestException.class, () -> applicationService.createApplication(1L, request));
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    @DisplayName("createApplication(status=REJECTED) — 400 Bad Request")
    void createApplication_statusRejected_throwsBadRequest() {
        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .jobId(10L)
                .status(ApplicationStatus.REJECTED)
                .build();

        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(sampleProfile));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(sampleJob));

        assertThrows(BadRequestException.class, () -> applicationService.createApplication(1L, request));
        verify(applicationRepository, never()).save(any(Application.class));
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

    // ==============================
    // Status transition validation
    // ==============================

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

    // ==============================
    // Issue 4 & 5: Analytics with aggregate queries and appliedAt-based denominator
    // ==============================

    @Test
    @DisplayName("getAnalytics — Uses aggregate queries, no N+1 calls, appliedAt-based denominator")
    void getAnalytics_usesAggregateQueries() {
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(sampleProfile));
        when(applicationRepository.countByProfileId(1L)).thenReturn(3L);
        when(applicationRepository.countByProfileIdGroupByStatus(1L)).thenReturn(List.of(
                new Object[]{ApplicationStatus.APPLIED, 1L},
                new Object[]{ApplicationStatus.INTERVIEW, 1L},
                new Object[]{ApplicationStatus.WITHDRAWN, 1L}
        ));
        // 2 had appliedAt (APPLIED + WITHDRAWN that was previously APPLIED)
        when(applicationRepository.countByProfileIdAndAppliedAtIsNotNull(1L)).thenReturn(2L);
        when(applicationRepository.averageFitScoreByProfileId(1L)).thenReturn(75.0);

        ApplicationAnalyticsResponse analytics = applicationService.getAnalytics(1L);

        assertNotNull(analytics);
        assertEquals(3, analytics.getTotalApplications());
        assertEquals(1, analytics.getStatusCounts().get(ApplicationStatus.APPLIED));
        assertEquals(1, analytics.getStatusCounts().get(ApplicationStatus.INTERVIEW));
        assertEquals(1, analytics.getStatusCounts().get(ApplicationStatus.WITHDRAWN));
        // responded = INTERVIEW (1), applied base = 2 → responseRate = 50%
        assertEquals(50.0, analytics.getResponseRate());
        // interview = INTERVIEW (1), applied base = 2 → interviewRate = 50%
        assertEquals(50.0, analytics.getInterviewRate());
        assertEquals(0.0, analytics.getOfferRate());
        assertEquals(75.0, analytics.getAverageFitScore());

        // Verify no N+1: jobMatchingService.analyzeJob() never called
        verify(jobMatchingService, never()).analyzeJob(anyLong());
        // Verify no full-list loading: findAllByProfileId(Long) never called
        verify(applicationRepository, never()).findAllByProfileId(1L);
    }

    @Test
    @DisplayName("getAnalytics — SAVED->WITHDRAWN does not count as applied, APPLIED->WITHDRAWN does")
    void getAnalytics_appliedAtBasedDenominator() {
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(sampleProfile));
        when(applicationRepository.countByProfileId(1L)).thenReturn(2L);
        when(applicationRepository.countByProfileIdGroupByStatus(1L)).thenReturn(
                java.util.Collections.singletonList(new Object[]{ApplicationStatus.WITHDRAWN, 2L})
        );
        // Only 1 had appliedAt set (was APPLIED before withdrawn). The other was SAVED->WITHDRAWN.
        when(applicationRepository.countByProfileIdAndAppliedAtIsNotNull(1L)).thenReturn(1L);
        when(applicationRepository.averageFitScoreByProfileId(1L)).thenReturn(null);

        ApplicationAnalyticsResponse analytics = applicationService.getAnalytics(1L);

        assertEquals(2, analytics.getTotalApplications());
        assertEquals(0.0, analytics.getResponseRate());
        assertEquals(0.0, analytics.getAverageFitScore());
    }
}
