package com.dotfield.discovery;

import com.dotfield.dto.IngestionStatusResponse;
import com.dotfield.dto.JobDiscoveryResponse;
import com.dotfield.dto.SourceDiscoveryResult;
import com.dotfield.dto.SourceStatusDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Thread-safe in-memory monitor for job ingestion state and per-source observability.
 * <p>
 * Maintains overall run statistics and per-source status records without exposing
 * sensitive credentials, API keys, or secret headers.
 */
@Slf4j
@Component
public class JobIngestionMonitor {

    private static final Pattern SECRET_PARAM_PATTERN = Pattern.compile("(?i)(api_?key|key|token|secret|password|access_token|client_secret)=[^&\\s]+");
    private static final Pattern AUTH_HEADER_PATTERN = Pattern.compile("(?i)(Authorization|Bearer|Basic)\\s+[^&\\s]+");

    private final ConcurrentHashMap<String, SourceStatusDto> sourceStatuses = new ConcurrentHashMap<>();
    private final AtomicReference<IngestionStatusResponse> lastOverallStatus = new AtomicReference<>();

    /**
     * Record the outcome of an ingestion run across processed sources.
     */
    public synchronized void recordRun(JobDiscoveryResponse response, long durationMs, LocalDateTime runTimestamp) {
        if (response == null) {
            return;
        }

        LocalDateTime now = runTimestamp != null ? runTimestamp : LocalDateTime.now();
        List<SourceDiscoveryResult> sourceResults = response.getSourceResults() != null
                ? response.getSourceResults()
                : Collections.emptyList();

        for (SourceDiscoveryResult result : sourceResults) {
            String sourceName = result.getSource() != null ? result.getSource().trim().toUpperCase() : "UNKNOWN";
            SourceStatusDto existing = sourceStatuses.get(sourceName);

            LocalDateTime lastSuccess = existing != null ? existing.getLastSuccessfulRun() : null;
            LocalDateTime lastFailure = existing != null ? existing.getLastFailure() : null;

            String statusStr;
            String sanitizedError = sanitizeErrorMessage(result.getErrorMessage());

            if ("FAILED".equalsIgnoreCase(result.getStatus()) || result.getFailed() > 0 && result.getNewJobs() == 0 && result.getUpdatedJobs() == 0 && result.getUnchangedJobs() == 0) {
                statusStr = "FAILED";
                lastFailure = now;
            } else if (result.getFailed() > 0) {
                statusStr = "PARTIAL_SUCCESS";
                lastSuccess = now;
                lastFailure = now;
            } else if (result.getDiscovered() == 0) {
                statusStr = "NO_JOBS";
                lastSuccess = now;
            } else {
                statusStr = "SUCCESS";
                lastSuccess = now;
            }

            SourceStatusDto updatedSourceStatus = SourceStatusDto.builder()
                    .source(sourceName)
                    .status(statusStr)
                    .lastSuccessfulRun(lastSuccess)
                    .lastFailure(lastFailure)
                    .jobsFetched(result.getDiscovered())
                    .errorMessage(sanitizedError)
                    .build();

            sourceStatuses.put(sourceName, updatedSourceStatus);
        }

        List<SourceStatusDto> currentSources = new ArrayList<>(sourceStatuses.values());
        currentSources.sort(Comparator.comparing(SourceStatusDto::getSource));

        IngestionStatusResponse overall = IngestionStatusResponse.builder()
                .lastRun(now)
                .durationMs(durationMs)
                .sourcesProcessed(sourceResults.size())
                .jobsFetched(response.getDiscovered())
                .indiaFiltered(response.getIndiaFiltered())
                .jobsInserted(response.getNewJobs())
                .jobsUpdated(response.getUpdatedJobs())
                .duplicates(response.getDuplicates() + response.getUnchangedJobs())
                .failures(response.getFailed())
                .sources(currentSources)
                .build();

        lastOverallStatus.set(overall);
    }

    /**
     * Retrieve the current overall and per-source ingestion status.
     */
    public IngestionStatusResponse getCurrentStatus() {
        IngestionStatusResponse current = lastOverallStatus.get();
        List<SourceStatusDto> currentSources = new ArrayList<>(sourceStatuses.values());
        currentSources.sort(Comparator.comparing(SourceStatusDto::getSource));

        if (current == null) {
            return IngestionStatusResponse.builder()
                    .sourcesProcessed(0)
                    .jobsFetched(0)
                    .jobsInserted(0)
                    .jobsUpdated(0)
                    .duplicates(0)
                    .failures(0)
                    .sources(currentSources)
                    .build();
        }

        return IngestionStatusResponse.builder()
                .lastRun(current.getLastRun())
                .durationMs(current.getDurationMs())
                .sourcesProcessed(current.getSourcesProcessed())
                .jobsFetched(current.getJobsFetched())
                .jobsInserted(current.getJobsInserted())
                .jobsUpdated(current.getJobsUpdated())
                .duplicates(current.getDuplicates())
                .failures(current.getFailures())
                .sources(currentSources)
                .build();
    }

    /**
     * Sanitizes raw error messages by redacting sensitive query parameters, tokens, and authorization headers.
     */
    public String sanitizeErrorMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return null;
        }

        String sanitized = rawMessage;
        sanitized = SECRET_PARAM_PATTERN.matcher(sanitized).replaceAll("$1=[REDACTED]");
        sanitized = sanitized.replaceAll("(?i)Authorization\\s+:[\\s]*[^&\\s]+", "Authorization [REDACTED]");
        sanitized = sanitized.replaceAll("(?i)Authorization\\s+(Bearer|Basic)\\s+[^&\\s]+", "Authorization [REDACTED]");
        sanitized = sanitized.replaceAll("(?i)(Bearer|Basic)\\s+[^&\\s]+", "[REDACTED]");

        return sanitized;
    }
}
