package com.dotfield.discovery.india;

import com.dotfield.dto.RawJobListing;
import com.dotfield.extractor.ExtractedJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Centralized filter for evaluating whether a job listing is India-relevant.
 * Evaluated both BEFORE expensive extraction (on RawJobListing) and AFTER extraction (on ExtractedJob).
 * <p>
 * Central India filter is the final authority. Reliable explicit evidence (Indian cities, explicit India-remote)
 * is required for a job to be India-relevant. Foreign location evidence, generic unanchored remote ("Remote", "Worldwide"),
 * currency (INR), or company identity alone are NOT sufficient and MUST NOT be accepted.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndiaJobFilter {

    private final IndiaLocationNormalizer locationNormalizer;

    /**
     * Evaluates whether a {@link RawJobListing} is India-relevant.
     */
    public boolean isIndiaRelevant(RawJobListing rawJob) {
        if (rawJob == null) {
            return false;
        }

        // 1. Check explicit location string first
        if (rawJob.getLocation() != null && !rawJob.getLocation().isBlank()) {
            NormalizedLocation locationInfo = locationNormalizer.normalize(rawJob.getLocation());

            if (locationInfo.isIndiaRelevant()) {
                return true;
            }

            // Explicit foreign location (e.g. "London, UK", "New York, USA") or generic unanchored remote ("Remote", "Worldwide") MUST be rejected
            if (locationInfo.getNormalizedCountry() != null && !"IN".equalsIgnoreCase(locationInfo.getNormalizedCountry())) {
                return false;
            }
            if (locationInfo.isRemote()) {
                return false;
            }
        }

        // 2. Check title secondary evidence for explicit Indian city tags (only if location is missing/ambiguous)
        if (rawJob.getTitle() != null && !rawJob.getTitle().isBlank()) {
            NormalizedLocation titleLocation = locationNormalizer.normalize(rawJob.getTitle());
            if (titleLocation.isIndiaRelevant()) {
                return true;
            }
        }

        // 3. Trusted source flag check (only valid if location was missing and title contains no foreign tags)
        if (Boolean.TRUE.equals(rawJob.getIsIndiaRelevant())) {
            // Re-verify title to ensure no explicit foreign location (e.g. "Developer in London")
            if (rawJob.getTitle() != null && !rawJob.getTitle().isBlank()) {
                NormalizedLocation titleLoc = locationNormalizer.normalize(rawJob.getTitle());
                if (titleLoc.getNormalizedCountry() != null && !"IN".equalsIgnoreCase(titleLoc.getNormalizedCountry())) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    /**
     * Evaluates whether an {@link ExtractedJob} is India-relevant post-extraction.
     */
    public boolean isIndiaRelevant(ExtractedJob extractedJob) {
        if (extractedJob == null) {
            return false;
        }

        // 1. Check extracted location string first
        if (extractedJob.getLocation() != null && !extractedJob.getLocation().isBlank()) {
            NormalizedLocation locationInfo = locationNormalizer.normalize(extractedJob.getLocation());

            if (locationInfo.isIndiaRelevant()) {
                return true;
            }

            if (locationInfo.getNormalizedCountry() != null && !"IN".equalsIgnoreCase(locationInfo.getNormalizedCountry())) {
                return false;
            }
            if (locationInfo.isRemote()) {
                return false;
            }
        }

        // 2. Check title secondary evidence
        if (extractedJob.getTitle() != null && !extractedJob.getTitle().isBlank()) {
            NormalizedLocation titleLocation = locationNormalizer.normalize(extractedJob.getTitle());
            if (titleLocation.isIndiaRelevant()) {
                return true;
            }
        }

        if (Boolean.TRUE.equals(extractedJob.getIsIndiaRelevant())) {
            if (extractedJob.getTitle() != null && !extractedJob.getTitle().isBlank()) {
                NormalizedLocation titleLoc = locationNormalizer.normalize(extractedJob.getTitle());
                if (titleLoc.getNormalizedCountry() != null && !"IN".equalsIgnoreCase(titleLoc.getNormalizedCountry())) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }
}
