package com.dotfield.discovery;

import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.JobDiscoveryResponse;
import com.dotfield.dto.RawJobListing;
import com.dotfield.dto.SourceDiscoveryResult;
import com.dotfield.entity.Job;
import com.dotfield.entity.JobStatus;
import com.dotfield.exception.BadRequestException;
import com.dotfield.extractor.ExtractedJob;
import com.dotfield.extractor.JobExtractionPipeline;

import com.dotfield.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class JobDiscoveryService {

    private final JobSourceRegistry sourceRegistry;
    private final JobExtractionPipeline extractionPipeline;
    private final JobDeduplicationService deduplicationService;
    private final JobRepository jobRepository;

    public JobDiscoveryResponse discoverJobs(JobDiscoveryRequest request) {
        if (request == null || request.getSource() == null || request.getSource().isBlank()) {
            throw new BadRequestException("Source is required");
        }

        if (request.getMaxResults() != null && (request.getMaxResults() < 1 || request.getMaxResults() > 100)) {
            throw new BadRequestException("maxResults must be between 1 and 100");
        }

        JobSource source = sourceRegistry.getRequiredSource(request.getSource());

        int totalDiscovered = 0;
        int newJobs = 0;
        int updatedJobs = 0;
        int unchangedJobs = 0;
        int failed = 0;

        List<RawJobListing> rawListings;
        try {
            rawListings = source.discover(request);
        } catch (Exception e) {
            log.error("Failed to discover jobs from source: {}", source.getSourceName(), e);
            failed = 1;
            rawListings = List.of();
        }

        totalDiscovered = rawListings.size();

        for (RawJobListing rawListing : rawListings) {
            try {
                ProcessResult result = processListing(rawListing, source.getSourceName());
                switch (result) {
                    case NEW -> newJobs++;
                    case UPDATED -> updatedJobs++;
                    case UNCHANGED -> unchangedJobs++;
                }
            } catch (Exception e) {
                log.error("Failed to process listing from source: {}", source.getSourceName(), e);
                failed++;
            }
        }

        int duplicates = totalDiscovered - (newJobs + updatedJobs + unchangedJobs);
        if (duplicates < 0) duplicates = 0;

        SourceDiscoveryResult sourceResult = SourceDiscoveryResult.builder()
                .source(source.getSourceName())
                .discovered(totalDiscovered)
                .newJobs(newJobs)
                .updatedJobs(updatedJobs)
                .unchangedJobs(unchangedJobs)
                .failed(failed)
                .build();

        log.info("Job discovery completed for source: {} -> Discovered: {}, New: {}, Updated: {}, Unchanged: {}, Failed: {}",
                source.getSourceName(), totalDiscovered, newJobs, updatedJobs, unchangedJobs, failed);

        return JobDiscoveryResponse.builder()
                .discovered(totalDiscovered)
                .newJobs(newJobs)
                .updatedJobs(updatedJobs)
                .unchangedJobs(unchangedJobs)
                .duplicates(duplicates)
                .failed(failed)
                .sourceResults(List.of(sourceResult))
                .build();
    }

    private ProcessResult processListing(RawJobListing rawListing, String defaultSource) {
        Map<String, Object> rawData = rawListing.getRawData() != null ? new HashMap<>(rawListing.getRawData()) : new HashMap<>();
        if (rawListing.getTitle() != null) rawData.putIfAbsent("title", rawListing.getTitle());
        if (rawListing.getCompany() != null) rawData.putIfAbsent("company", rawListing.getCompany());
        if (rawListing.getLocation() != null) rawData.putIfAbsent("location", rawListing.getLocation());
        if (rawListing.getDescription() != null) rawData.putIfAbsent("description", rawListing.getDescription());
        if (rawListing.getJobUrl() != null) rawData.putIfAbsent("jobUrl", rawListing.getJobUrl());
        if (rawListing.getEmploymentType() != null) rawData.putIfAbsent("employmentType", rawListing.getEmploymentType().name());
        if (rawListing.getRemoteType() != null) rawData.putIfAbsent("remoteType", rawListing.getRemoteType().name());
        if (rawListing.getSalaryMin() != null) rawData.putIfAbsent("salaryMin", rawListing.getSalaryMin());
        if (rawListing.getSalaryMax() != null) rawData.putIfAbsent("salaryMax", rawListing.getSalaryMax());
        if (rawListing.getCurrency() != null) rawData.putIfAbsent("currency", rawListing.getCurrency());
        if (rawListing.getPostedDate() != null) rawData.putIfAbsent("postedDate", rawListing.getPostedDate().toString());

        String sourceName = rawListing.getSource() != null ? rawListing.getSource() : defaultSource;
        ExtractedJob extractedJob = extractionPipeline.extractAndNormalize(rawData, sourceName);

        String canonicalUrl = deduplicationService.canonicalizeUrl(extractedJob.getJobUrl());
        String fingerprint = deduplicationService.generateFingerprint(
                extractedJob.getCompany(),
                extractedJob.getTitle(),
                extractedJob.getLocation(),
                extractedJob.getDescription()
        );

        Optional<Job> existingOpt = deduplicationService.findExistingJob(
                extractedJob.getSource(),
                rawListing.getExternalId(),
                extractedJob.getJobUrl(),
                extractedJob.getCompany(),
                extractedJob.getTitle(),
                extractedJob.getLocation(),
                extractedJob.getDescription()
        );

        LocalDateTime now = LocalDateTime.now();

        if (existingOpt.isPresent()) {
            Job existingJob = existingOpt.get();
            existingJob.setLastDiscoveredAt(now);

            boolean changed = updateJobFieldsIfChanged(existingJob, extractedJob, rawListing.getExternalId(), canonicalUrl, fingerprint);

            jobRepository.save(existingJob);
            return changed ? ProcessResult.UPDATED : ProcessResult.UNCHANGED;
        }

        // Create new job
        Job newJob = Job.builder()
                .externalId(rawListing.getExternalId())
                .title(extractedJob.getTitle())
                .company(extractedJob.getCompany())
                .location(extractedJob.getLocation())
                .description(extractedJob.getDescription())
                .jobUrl(extractedJob.getJobUrl())
                .canonicalUrl(canonicalUrl)
                .deduplicationFingerprint(fingerprint)
                .source(extractedJob.getSource())
                .employmentType(extractedJob.getEmploymentType())
                .remoteType(extractedJob.getRemoteType())
                .status(JobStatus.SAVED) // Default state for newly discovered job
                .salaryMin(extractedJob.getSalaryMin())
                .salaryMax(extractedJob.getSalaryMax())
                .currency(extractedJob.getCurrency())
                .postedDate(extractedJob.getPostedDate())
                .lastDiscoveredAt(now)
                .build();

        try {
            jobRepository.save(newJob);
            return ProcessResult.NEW;
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrency collision detected for job URL/externalId during save. Falling back to re-fetch and update.", e);
            Optional<Job> fallbackOpt = deduplicationService.findExistingJob(
                    extractedJob.getSource(),
                    rawListing.getExternalId(),
                    extractedJob.getJobUrl(),
                    extractedJob.getCompany(),
                    extractedJob.getTitle(),
                    extractedJob.getLocation(),
                    extractedJob.getDescription()
            );
            if (fallbackOpt.isPresent()) {
                Job fallbackJob = fallbackOpt.get();
                fallbackJob.setLastDiscoveredAt(now);
                boolean changed = updateJobFieldsIfChanged(fallbackJob, extractedJob, rawListing.getExternalId(), canonicalUrl, fingerprint);
                jobRepository.save(fallbackJob);
                return changed ? ProcessResult.UPDATED : ProcessResult.UNCHANGED;
            }
            throw e;
        }
    }

    private boolean updateJobFieldsIfChanged(Job job, ExtractedJob extracted, String externalId, String canonicalUrl, String fingerprint) {
        boolean changed = false;

        if (!Objects.equals(job.getExternalId(), externalId)) {
            job.setExternalId(externalId);
            changed = true;
        }
        if (!Objects.equals(job.getTitle(), extracted.getTitle())) {
            job.setTitle(extracted.getTitle());
            changed = true;
        }
        if (!Objects.equals(job.getCompany(), extracted.getCompany())) {
            job.setCompany(extracted.getCompany());
            changed = true;
        }
        if (!Objects.equals(job.getLocation(), extracted.getLocation())) {
            job.setLocation(extracted.getLocation());
            changed = true;
        }
        if (!Objects.equals(job.getDescription(), extracted.getDescription())) {
            job.setDescription(extracted.getDescription());
            changed = true;
        }
        if (!Objects.equals(job.getJobUrl(), extracted.getJobUrl())) {
            job.setJobUrl(extracted.getJobUrl());
            changed = true;
        }
        if (!Objects.equals(job.getCanonicalUrl(), canonicalUrl)) {
            job.setCanonicalUrl(canonicalUrl);
            changed = true;
        }
        if (!Objects.equals(job.getDeduplicationFingerprint(), fingerprint)) {
            job.setDeduplicationFingerprint(fingerprint);
            changed = true;
        }
        if (!Objects.equals(job.getEmploymentType(), extracted.getEmploymentType())) {
            job.setEmploymentType(extracted.getEmploymentType());
            changed = true;
        }
        if (!Objects.equals(job.getRemoteType(), extracted.getRemoteType())) {
            job.setRemoteType(extracted.getRemoteType());
            changed = true;
        }
        if (!Objects.equals(job.getSalaryMin(), extracted.getSalaryMin())) {
            job.setSalaryMin(extracted.getSalaryMin());
            changed = true;
        }
        if (!Objects.equals(job.getSalaryMax(), extracted.getSalaryMax())) {
            job.setSalaryMax(extracted.getSalaryMax());
            changed = true;
        }
        if (!Objects.equals(job.getCurrency(), extracted.getCurrency())) {
            job.setCurrency(extracted.getCurrency());
            changed = true;
        }
        if (!Objects.equals(job.getPostedDate(), extracted.getPostedDate())) {
            job.setPostedDate(extracted.getPostedDate());
            changed = true;
        }

        // CRITICAL: job.getStatus() is NEVER modified here! Candidate status is preserved.

        return changed;
    }

    private enum ProcessResult {
        NEW, UPDATED, UNCHANGED
    }

}
