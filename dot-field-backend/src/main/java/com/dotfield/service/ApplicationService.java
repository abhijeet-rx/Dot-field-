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

    public ApplicationResponse createApplication(Long userId, CreateApplicationRequest request) {
        Profile profile = findProfileByUserId(userId);
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + request.getJobId()));

        if (applicationRepository.existsByProfileIdAndJobId(profile.getId(), job.getId())) {
            throw new ConflictException("Application already exists for job id: " + job.getId());
        }

        ApplicationStatus status = request.getStatus() != null ? request.getStatus() : ApplicationStatus.SAVED;
        LocalDateTime appliedAt = (status == ApplicationStatus.APPLIED) ? LocalDateTime.now() : null;

        Application application = Application.builder()
                .profile(profile)
                .job(job)
                .status(status)
                .notes(request.getNotes())
                .appliedAt(appliedAt)
                .build();

        Application saved = applicationRepository.save(application);
        log.info("User ID: {} created application ID: {} for job ID: {}", userId, saved.getId(), job.getId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ApplicationResponse> getApplications(Long userId, ApplicationStatus status, Pageable pageable) {
        Profile profile = findProfileByUserId(userId);
        Page<Application> page;

        if (status != null) {
            page = applicationRepository.findAllByProfileIdAndStatus(profile.getId(), status, pageable);
        } else {
            page = applicationRepository.findAllByProfileId(profile.getId(), pageable);
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

    public ApplicationResponse updateStatus(Long userId, Long applicationId, ApplicationStatus newStatus) {
        Profile profile = findProfileByUserId(userId);
        Application application = applicationRepository.findByIdAndProfileId(applicationId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

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

    @Transactional(readOnly = true)
    public ApplicationAnalyticsResponse getAnalytics(Long userId) {
        Profile profile = findProfileByUserId(userId);
        List<Application> applications = applicationRepository.findAllByProfileId(profile.getId());

        long total = applications.size();
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus s : ApplicationStatus.values()) {
            counts.put(s, 0L);
        }

        double sumFitScore = 0.0;
        int fitCount = 0;

        long respondedCount = 0;
        long interviewCount = 0;
        long offerCount = 0;
        long appliedBaseCount = 0;

        for (Application app : applications) {
            counts.put(app.getStatus(), counts.getOrDefault(app.getStatus(), 0L) + 1);

            if (app.getStatus() != ApplicationStatus.SAVED && app.getStatus() != ApplicationStatus.WITHDRAWN) {
                appliedBaseCount++;
            }

            if (app.getStatus() == ApplicationStatus.SCREENING
                    || app.getStatus() == ApplicationStatus.INTERVIEW
                    || app.getStatus() == ApplicationStatus.OFFER
                    || app.getStatus() == ApplicationStatus.REJECTED) {
                respondedCount++;
            }

            if (app.getStatus() == ApplicationStatus.INTERVIEW || app.getStatus() == ApplicationStatus.OFFER) {
                interviewCount++;
            }

            if (app.getStatus() == ApplicationStatus.OFFER) {
                offerCount++;
            }

            try {
                JobMatchResponse match = jobMatchingService.analyzeJob(app.getJob().getId());
                sumFitScore += match.getOverallScore();
                fitCount++;
            } catch (Exception ignored) {}
        }

        double responseRate = appliedBaseCount > 0 ? (double) respondedCount / appliedBaseCount * 100.0 : 0.0;
        double interviewRate = appliedBaseCount > 0 ? (double) interviewCount / appliedBaseCount * 100.0 : 0.0;
        double offerRate = appliedBaseCount > 0 ? (double) offerCount / appliedBaseCount * 100.0 : 0.0;
        double avgFitScore = fitCount > 0 ? sumFitScore / fitCount : 0.0;

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
        Integer fitScore = null;
        String matchCategory = null;

        try {
            JobMatchResponse match = jobMatchingService.analyzeJob(app.getJob().getId());
            fitScore = match.getOverallScore();
            matchCategory = match.getMatchCategory();
        } catch (Exception ignored) {}

        return ApplicationResponse.builder()
                .id(app.getId())
                .job(jobDto)
                .status(app.getStatus())
                .notes(app.getNotes())
                .fitScore(fitScore)
                .matchCategory(matchCategory)
                .appliedAt(app.getAppliedAt())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
