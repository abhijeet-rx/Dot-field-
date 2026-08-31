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
import org.springframework.beans.factory.annotation.Value;
import lombok.Setter;
import org.springframework.stereotype.Service;

import com.dotfield.dto.IngestionStatusResponse;
import com.dotfield.dto.JobIngestionRunResponse;
import com.dotfield.exception.ConflictException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orchestrates job ingestion across single or multiple registered job sources.
 * Resolves source adapters, invokes the shared extraction pipeline, deduplicates,
 * and persists results with strict error isolation per source.
 * <p>
 * This class is intentionally NOT {@code @Transactional} at the class level.
 * Each listing is persisted in its own {@code REQUIRES_NEW} transaction via
 * {@link JobDiscoveryPersistenceHelper}, so that a constraint violation on
 * one listing does not roll back all others.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobDiscoveryService implements JobIngestionOrchestrator {

    private final JobSourceRegistry sourceRegistry;
    private final JobExtractionPipeline extractionPipeline;
    private final JobDeduplicationService deduplicationService;
    private final JobDiscoveryPersistenceHelper persistenceHelper;
    private final JobRepository jobRepository;
    private final JobIngestionMonitor ingestionMonitor;

    private final AtomicBoolean ingestionRunning = new AtomicBoolean(false);

    @Setter
    @Value("${job.ingestion.freshness.threshold-days:7}")
    private int freshnessThresholdDays = 7;

    public boolean isIngestionRunning() {
        return ingestionRunning.get();
    }

    /**
     * Controlled manual ingestion trigger with shared concurrency prevention.
     */
    public JobIngestionRunResponse runManualIngestion(JobDiscoveryRequest request) {
        JobDiscoveryRequest req = (request != null && request.getSource() != null && !request.getSource().isBlank())
                ? request
                : JobDiscoveryRequest.builder().source("ALL").build();

        JobDiscoveryResponse response = discoverJobs(req);
        return JobIngestionRunResponse.fromJobDiscoveryResponse(response);
    }

    public IngestionStatusResponse getIngestionStatus() {
        return ingestionMonitor.getCurrentStatus();
    }

    @Override
    public JobDiscoveryResponse discoverJobs(JobDiscoveryRequest request) {
        if (request == null || request.getSource() == null || request.getSource().isBlank()) {
            throw new BadRequestException("Source is required");
        }

        if (request.getMaxResults() != null && (request.getMaxResults() < 1 || request.getMaxResults() > 100)) {
            throw new BadRequestException("maxResults must be between 1 and 100");
        }

        if (!ingestionRunning.compareAndSet(false, true)) {
            log.warn("Job ingestion request rejected — an ingestion run is already active");
            throw new ConflictException("Job ingestion run is already in progress. Please wait for the current run to complete.");
        }

        try {
            if ("ALL".equalsIgnoreCase(request.getSource().trim())) {
                return performDiscoverFromAllSources(request);
            }
            return performDiscoverSingleSource(request);
        } finally {
            ingestionRunning.set(false);
        }
    }

    @Override
    public JobDiscoveryResponse discoverFromAllSources(JobDiscoveryRequest request) {
        JobDiscoveryRequest req = (request != null && request.getSource() != null && !request.getSource().isBlank())
                ? request
                : JobDiscoveryRequest.builder().source("ALL").build();

        return discoverJobs(req);
    }

    private JobDiscoveryResponse performDiscoverSingleSource(JobDiscoveryRequest request) {
        long startTime = System.currentTimeMillis();
        LocalDateTime runTimestamp = LocalDateTime.now();

        JobSource source = sourceRegistry.getRequiredSource(request.getSource());
        SourceDiscoveryResult result = processSingleSource(source, request);

        JobDiscoveryResponse response = JobDiscoveryResponse.builder()
                .discovered(result.getDiscovered())
                .newJobs(result.getNewJobs())
                .updatedJobs(result.getUpdatedJobs())
                .unchangedJobs(result.getUnchangedJobs())
                .duplicates(result.getDuplicates())
                .failed(result.getFailed())
                .sourceResults(List.of(result))
                .build();

        long durationMs = System.currentTimeMillis() - startTime;
        ingestionMonitor.recordRun(response, durationMs, runTimestamp);

        return response;
    }

    /**
     * Executes job discovery across ALL registered job sources with error isolation per source.
     * A failure in one source is logged and reported without halting other sources.
     */
    private JobDiscoveryResponse performDiscoverFromAllSources(JobDiscoveryRequest request) {
        long startTime = System.currentTimeMillis();
        LocalDateTime runTimestamp = LocalDateTime.now();

        List<JobSource> sources = sourceRegistry.getAllSources();
        List<SourceDiscoveryResult> sourceResults = new ArrayList<>();

        int totalDiscovered = 0;
        int totalNewJobs = 0;
        int totalUpdatedJobs = 0;
        int totalUnchangedJobs = 0;
        int totalDuplicates = 0;
        int totalFailed = 0;

        for (JobSource source : sources) {
            SourceDiscoveryResult result = processSingleSource(source, request);
            sourceResults.add(result);

            totalDiscovered += result.getDiscovered();
            totalNewJobs += result.getNewJobs();
            totalUpdatedJobs += result.getUpdatedJobs();
            totalUnchangedJobs += result.getUnchangedJobs();
            totalDuplicates += result.getDuplicates();
            totalFailed += result.getFailed();
        }

        JobDiscoveryResponse response = JobDiscoveryResponse.builder()
                .discovered(totalDiscovered)
                .newJobs(totalNewJobs)
                .updatedJobs(totalUpdatedJobs)
                .unchangedJobs(totalUnchangedJobs)
                .duplicates(totalDuplicates)
                .failed(totalFailed)
                .sourceResults(sourceResults)
                .build();

        long durationMs = System.currentTimeMillis() - startTime;
        ingestionMonitor.recordRun(response, durationMs, runTimestamp);

        return response;
    }

    private SourceDiscoveryResult processSingleSource(JobSource source, JobDiscoveryRequest request) {
        List<RawJobListing> rawListings;
        try {
            rawListings = source.discover(request);
        } catch (Exception e) {
            log.error("Failed to discover jobs from source: {}", source.getSourceName(), e);
            return SourceDiscoveryResult.builder()
                    .source(source.getSourceName())
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .discovered(0)
                    .failed(1)
                    .build();
        }

        int totalDiscovered = rawListings.size();
        int newJobs = 0;
        int updatedJobs = 0;
        int unchangedJobs = 0;
        int duplicates = 0;
        int failed = 0;

        Set<String> seenExternalIds = new HashSet<>();

        for (RawJobListing rawListing : rawListings) {
            try {
                if (rawListing.getExternalId() != null && !rawListing.getExternalId().isBlank()) {
                    if (!seenExternalIds.add(rawListing.getExternalId().trim())) {
                        log.debug("Within-batch duplicate detected for externalId: {}", rawListing.getExternalId());
                        duplicates++;
                        continue;
                    }
                }

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

        log.info("Job discovery completed for source: {} -> Discovered: {}, New: {}, Updated: {}, Unchanged: {}, Duplicates: {}, Failed: {}",
                source.getSourceName(), totalDiscovered, newJobs, updatedJobs, unchangedJobs, duplicates, failed);

        expireStaleJobsForSource(source.getSourceName());

        return SourceDiscoveryResult.builder()
                .source(source.getSourceName())
                .status("SUCCESS")
                .discovered(totalDiscovered)
                .newJobs(newJobs)
                .updatedJobs(updatedJobs)
                .unchangedJobs(unchangedJobs)
                .duplicates(duplicates)
                .failed(failed)
                .build();
    }

    public int expireStaleJobsForSource(String sourceName) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(freshnessThresholdDays);
        return expireStaleJobsForSource(sourceName, threshold);
    }

    public int expireStaleJobsForSource(String sourceName, LocalDateTime threshold) {
        try {
            List<Job> staleJobs = jobRepository.findStaleJobsForSource(sourceName, JobStatus.EXPIRED, threshold);
            int expiredCount = 0;
            for (Job job : staleJobs) {
                job.setStatus(JobStatus.EXPIRED);
                persistenceHelper.updateExistingJob(job);
                expiredCount++;
            }
            if (expiredCount > 0) {
                log.info("Marked {} stale jobs as EXPIRED for source: {} (threshold: {})", expiredCount, sourceName, threshold);
            }
            return expiredCount;
        } catch (Exception e) {
            log.error("Failed to expire stale jobs for source: {}", sourceName, e);
            return 0;
        }
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
            existingJob.setLastSeenAt(now);
            existingJob.setLastDiscoveredAt(now);
            if (existingJob.getFirstSeenAt() == null) {
                existingJob.setFirstSeenAt(existingJob.getCreatedAt() != null ? existingJob.getCreatedAt() : now);
            }

            boolean changed = updateJobFieldsIfChanged(existingJob, extractedJob, rawListing.getExternalId(), canonicalUrl, fingerprint);

            persistenceHelper.updateExistingJob(existingJob);
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
                .status(JobStatus.ACTIVE)
                .salaryMin(extractedJob.getSalaryMin())
                .salaryMax(extractedJob.getSalaryMax())
                .currency(extractedJob.getCurrency())
                .postedDate(extractedJob.getPostedDate())
                .firstSeenAt(now)
                .lastSeenAt(now)
                .lastDiscoveredAt(now)
                .build();

        try {
            persistenceHelper.saveNewJob(newJob);
            return ProcessResult.NEW;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.info("Concurrent duplicate detected. Re-fetching existing job for source={}, externalId={}, canonicalUrl={}",
                    extractedJob.getSource(), rawListing.getExternalId(), canonicalUrl);

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
                fallbackJob.setLastSeenAt(now);
                fallbackJob.setLastDiscoveredAt(now);
                if (fallbackJob.getFirstSeenAt() == null) {
                    fallbackJob.setFirstSeenAt(now);
                }
                boolean changed = updateJobFieldsIfChanged(fallbackJob, extractedJob, rawListing.getExternalId(), canonicalUrl, fingerprint);
                persistenceHelper.updateExistingJob(fallbackJob);
                return changed ? ProcessResult.UPDATED : ProcessResult.UNCHANGED;
            }

            log.warn("Could not re-fetch job after constraint violation. source={}, externalId={}",
                    extractedJob.getSource(), rawListing.getExternalId());
            return ProcessResult.UNCHANGED;
        }
    }

    private boolean updateJobFieldsIfChanged(Job job, ExtractedJob extracted, String externalId, String canonicalUrl, String fingerprint) {
        boolean changed = false;

        if (externalId != null && !externalId.isBlank() && !Objects.equals(job.getExternalId(), externalId)) {
            job.setExternalId(externalId);
            changed = true;
        }
        if (extracted.getTitle() != null && !extracted.getTitle().isBlank() && !Objects.equals(job.getTitle(), extracted.getTitle())) {
            job.setTitle(extracted.getTitle());
            changed = true;
        }
        if (extracted.getCompany() != null && !extracted.getCompany().isBlank() && !Objects.equals(job.getCompany(), extracted.getCompany())) {
            job.setCompany(extracted.getCompany());
            changed = true;
        }
        if (extracted.getLocation() != null && !extracted.getLocation().isBlank() && !Objects.equals(job.getLocation(), extracted.getLocation())) {
            job.setLocation(extracted.getLocation());
            changed = true;
        }
        if (extracted.getDescription() != null && !extracted.getDescription().isBlank() && !Objects.equals(job.getDescription(), extracted.getDescription())) {
            job.setDescription(extracted.getDescription());
            changed = true;
        }
        if (extracted.getJobUrl() != null && !extracted.getJobUrl().isBlank() && !Objects.equals(job.getJobUrl(), extracted.getJobUrl())) {
            job.setJobUrl(extracted.getJobUrl());
            changed = true;
        }
        if (canonicalUrl != null && !canonicalUrl.isBlank() && !Objects.equals(job.getCanonicalUrl(), canonicalUrl)) {
            job.setCanonicalUrl(canonicalUrl);
            changed = true;
        }
        if (fingerprint != null && !fingerprint.isBlank() && !Objects.equals(job.getDeduplicationFingerprint(), fingerprint)) {
            job.setDeduplicationFingerprint(fingerprint);
            changed = true;
        }
        if (extracted.getEmploymentType() != null && !Objects.equals(job.getEmploymentType(), extracted.getEmploymentType())) {
            job.setEmploymentType(extracted.getEmploymentType());
            changed = true;
        }
        if (extracted.getRemoteType() != null && !Objects.equals(job.getRemoteType(), extracted.getRemoteType())) {
            job.setRemoteType(extracted.getRemoteType());
            changed = true;
        }
        if (extracted.getSalaryMin() != null && !Objects.equals(job.getSalaryMin(), extracted.getSalaryMin())) {
            job.setSalaryMin(extracted.getSalaryMin());
            changed = true;
        }
        if (extracted.getSalaryMax() != null && !Objects.equals(job.getSalaryMax(), extracted.getSalaryMax())) {
            job.setSalaryMax(extracted.getSalaryMax());
            changed = true;
        }
        if (extracted.getCurrency() != null && !extracted.getCurrency().isBlank() && !Objects.equals(job.getCurrency(), extracted.getCurrency())) {
            job.setCurrency(extracted.getCurrency());
            changed = true;
        }
        if (extracted.getPostedDate() != null && !Objects.equals(job.getPostedDate(), extracted.getPostedDate())) {
            job.setPostedDate(extracted.getPostedDate());
            changed = true;
        }

        return changed;
    }

    private enum ProcessResult {
        NEW, UPDATED, UNCHANGED
    }

}
