package com.dotfield.service;

import com.dotfield.dto.*;
import com.dotfield.entity.Application;
import com.dotfield.entity.ApplicationStatus;
import com.dotfield.entity.Job;
import com.dotfield.entity.Profile;
import com.dotfield.exception.ConflictException;
import com.dotfield.exception.ResourceNotFoundException;
import com.dotfield.mapper.JobMapper;
import com.dotfield.repository.ApplicationRepository;
import com.dotfield.repository.JobRepository;
import com.dotfield.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ProfileRepository profileRepository;
    private final JobRepository jobRepository;
    private final JobMapper jobMapper;
    private final JobMatchingService jobMatchingService;
    private final ApplicationStatusTransitionValidator statusTransitionValidator;

    public ApplicationResponse createApplication(Long userId, CreateApplicationRequest request) {
        Profile profile = findProfileByUserId(userId);
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + request.getJobId()));

        // Issue 1: Validate that the requested creation status is allowed (only SAVED or APPLIED)
        statusTransitionValidator.validateCreationStatus(request.getStatus());

        if (applicationRepository.existsByProfileIdAndJobId(profile.getId(), job.getId())) {
            throw new ConflictException("Application already exists for job id: " + job.getId());
        }

        ApplicationStatus status = request.getStatus() != null ? request.getStatus() : ApplicationStatus.SAVED;
        LocalDateTime appliedAt = (status == ApplicationStatus.APPLIED) ? LocalDateTime.now() : null;

        Integer fitScore = null;
        String matchCategory = null;
        try {
            JobMatchResponse match = jobMatchingService.analyzeJob(job.getId());
            fitScore = match.getOverallScore();
            matchCategory = match.getMatchCategory();
        } catch (Exception ex) {
            log.warn("Could not calculate fit match score snapshot for Job ID: {} on application creation: {}", job.getId(), ex.getMessage());
        }

        Application application = Application.builder()
                .profile(profile)
                .job(job)
                .status(status)
                .notes(request.getNotes())
                .appliedAt(appliedAt)
                .fitScore(fitScore)
                .matchCategory(matchCategory)
                .build();

        Application saved = applicationRepository.save(application);
        log.info("User ID: {} created application ID: {} for job ID: {}", userId, saved.getId(), job.getId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ApplicationResponse> getApplications(Long userId, ApplicationStatus status, String search, Pageable pageable) {
        Profile profile = findProfileByUserId(userId);
        String searchQuery = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<Application> page;
        if (searchQuery == null) {
            if (status == null) {
                page = applicationRepository.findAllByProfileId(profile.getId(), pageable);
            } else {
                page = applicationRepository.findAllByProfileIdAndStatus(profile.getId(), status, pageable);
            }
        } else {
            page = applicationRepository.searchApplications(profile.getId(), status, searchQuery, pageable);
        }
        Page<ApplicationResponse> dtoPage = page.map(this::mapToResponse);
        return PagedResponse.fromPage(dtoPage);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(Long userId, Long applicationId) {
        Profile profile = findProfileByUserId(userId);
        Application application = applicationRepository.findByIdAndProfileId(applicationId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));
        return mapToResponse(application);
    }

    @Transactional(readOnly = true)
    public Optional<ApplicationResponse> getApplicationByJobId(Long userId, Long jobId) {
        Profile profile = findProfileByUserId(userId);
        return applicationRepository.findByProfileIdAndJobId(profile.getId(), jobId)
                .map(this::mapToResponse);
    }

    public ApplicationResponse updateStatus(Long userId, Long applicationId, ApplicationStatus newStatus) {
        Profile profile = findProfileByUserId(userId);
        Application application = applicationRepository.findByIdAndProfileId(applicationId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        statusTransitionValidator.validateTransition(application.getStatus(), newStatus);

        application.setStatus(newStatus);
        if (newStatus == ApplicationStatus.APPLIED && application.getAppliedAt() == null) {
            application.setAppliedAt(LocalDateTime.now());
        }

        Application updated = applicationRepository.save(application);
        log.info("User ID: {} updated status of application ID: {} to {}", userId, applicationId, newStatus);
        return mapToResponse(updated);
    }

    public ApplicationResponse updateNotes(Long userId, Long applicationId, String notes) {
        Profile profile = findProfileByUserId(userId);
        Application application = applicationRepository.findByIdAndProfileId(applicationId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        application.setNotes(notes);
        Application updated = applicationRepository.save(application);
        log.info("User ID: {} updated notes for application ID: {}", userId, applicationId);
        return mapToResponse(updated);
    }

    public void deleteApplication(Long userId, Long applicationId) {
        Profile profile = findProfileByUserId(userId);
        Application application = applicationRepository.findByIdAndProfileId(applicationId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        applicationRepository.delete(application);
        log.info("User ID: {} deleted application ID: {}", userId, applicationId);
    }

    /**
     * Issue 4 & 5: Analytics computed via database aggregate queries.
     * Applied base uses appliedAt IS NOT NULL (persisted timestamp) as source of truth,
     * so that APPLIED->WITHDRAWN still counts as an applied application,
     * while SAVED->WITHDRAWN does not.
     */
    @Transactional(readOnly = true)
    public ApplicationAnalyticsResponse getAnalytics(Long userId) {
        Profile profile = findProfileByUserId(userId);
        Long profileId = profile.getId();

        // Issue 4: Use repository-level aggregate queries instead of loading all entities
        long total = applicationRepository.countByProfileId(profileId);

        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus s : ApplicationStatus.values()) {
            counts.put(s, 0L);
        }
        List<Object[]> statusCountRows = applicationRepository.countByProfileIdGroupByStatus(profileId);
        for (Object[] row : statusCountRows) {
            ApplicationStatus status = (ApplicationStatus) row[0];
            Long count = (Long) row[1];
            counts.put(status, count);
        }

        // Issue 5: Applied base = applications where appliedAt IS NOT NULL
        // This correctly includes APPLIED->WITHDRAWN (had appliedAt set) and
        // excludes SAVED->WITHDRAWN (never had appliedAt set).
        long appliedBaseCount = applicationRepository.countByProfileIdAndAppliedAtIsNotNull(profileId);

        // Responded = SCREENING + INTERVIEW + OFFER + REJECTED (employer acted on it)
        long respondedCount = counts.getOrDefault(ApplicationStatus.SCREENING, 0L)
                + counts.getOrDefault(ApplicationStatus.INTERVIEW, 0L)
                + counts.getOrDefault(ApplicationStatus.OFFER, 0L)
                + counts.getOrDefault(ApplicationStatus.REJECTED, 0L);

        long interviewCount = counts.getOrDefault(ApplicationStatus.INTERVIEW, 0L)
                + counts.getOrDefault(ApplicationStatus.OFFER, 0L);
        long offerCount = counts.getOrDefault(ApplicationStatus.OFFER, 0L);

        Double avgFitScoreRaw = applicationRepository.averageFitScoreByProfileId(profileId);
        double avgFitScore = avgFitScoreRaw != null ? avgFitScoreRaw : 0.0;

        double responseRate = appliedBaseCount > 0 ? (double) respondedCount / appliedBaseCount * 100.0 : 0.0;
        double interviewRate = appliedBaseCount > 0 ? (double) interviewCount / appliedBaseCount * 100.0 : 0.0;
        double offerRate = appliedBaseCount > 0 ? (double) offerCount / appliedBaseCount * 100.0 : 0.0;

        return ApplicationAnalyticsResponse.builder()
                .totalApplications(total)
                .statusCounts(counts)
                .responseRate(Math.round(responseRate * 10.0) / 10.0)
                .interviewRate(Math.round(interviewRate * 10.0) / 10.0)
                .offerRate(Math.round(offerRate * 10.0) / 10.0)
                .averageFitScore(Math.round(avgFitScore * 10.0) / 10.0)
                .build();
    }

    private Profile findProfileByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found for user ID: " + userId));
    }

    private ApplicationResponse mapToResponse(Application app) {
        JobResponse jobDto = jobMapper.toJobResponse(app.getJob());

        return ApplicationResponse.builder()
                .id(app.getId())
                .job(jobDto)
                .status(app.getStatus())
                .notes(app.getNotes())
                .fitScore(app.getFitScore())
                .matchCategory(app.getMatchCategory())
                .appliedAt(app.getAppliedAt())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
