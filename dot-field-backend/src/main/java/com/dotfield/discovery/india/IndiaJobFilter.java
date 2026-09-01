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
 * Ensures explicit foreign location (e.g. "London, UK") takes precedence over title location or source flags.
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

            // If explicit location normalizes to foreign (e.g., "London, UK"), it MUST be rejected regardless of title or flags
            if (!locationInfo.isIndiaRelevant() && locationInfo.getNormalizedCountry() != null && !"IN".equalsIgnoreCase(locationInfo.getNormalizedCountry())) {
                return false;
            }

            if (locationInfo.isIndiaRelevant()) {
                return true;
            }
        }

        // 2. Explicit pre-classification flag set by a trusted source adapter (only if location is not foreign)
        if (Boolean.TRUE.equals(rawJob.getIsIndiaRelevant())) {
            return true;
        }
        if (Boolean.FALSE.equals(rawJob.getIsIndiaRelevant())) {
            return false;
        }

        // 3. Secondary evidence: Check job title for explicit Indian city tags (only if location is missing/ambiguous)
        if (rawJob.getTitle() != null && !rawJob.getTitle().isBlank()) {
            NormalizedLocation titleLocation = locationNormalizer.normalize(rawJob.getTitle());

            // Title location must NOT override explicit foreign location in title either (e.g., "Developer in London")
            if (titleLocation.isIndiaRelevant()) {
                return true;
            }
        }

        // 4. Currency alone (INR / ₹) or company identity is NOT sufficient evidence
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

            // Explicit foreign location post-extraction rejects listing regardless of flags or title
            if (!locationInfo.isIndiaRelevant() && locationInfo.getNormalizedCountry() != null && !"IN".equalsIgnoreCase(locationInfo.getNormalizedCountry())) {
                return false;
            }

            if (locationInfo.isIndiaRelevant()) {
                return true;
            }
        }

        // 2. Check title secondary evidence
        if (extractedJob.getTitle() != null && !extractedJob.getTitle().isBlank()) {
            NormalizedLocation titleLocation = locationNormalizer.normalize(extractedJob.getTitle());
            if (titleLocation.isIndiaRelevant()) {
                return true;
            }
        }

        if (Boolean.FALSE.equals(extractedJob.getIsIndiaRelevant())) {
            return false;
        }

        return Boolean.TRUE.equals(extractedJob.getIsIndiaRelevant());
    }
}
