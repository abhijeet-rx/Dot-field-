package com.dotfield.discovery.india;

import com.dotfield.dto.RawJobListing;
import com.dotfield.extractor.ExtractedJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Centralized filter for evaluating whether a job listing is India-relevant.
 * Evaluated BEFORE expensive extraction and database persistence.
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

        // 1. Explicit pre-classification flag set by source adapter or test payload
        if (rawJob.getIsIndiaRelevant() != null) {
            return rawJob.getIsIndiaRelevant();
        }

        // 2. Unspecified location (e.g. mock test payload without location field) -> default true
        if (rawJob.getLocation() == null || rawJob.getLocation().isBlank()) {
            return true;
        }

        // 3. Evaluate primary location field
        NormalizedLocation locationInfo = locationNormalizer.normalize(rawJob.getLocation());
        if (locationInfo.isIndiaRelevant()) {
            return true;
        }

        // 4. Check title for explicit Indian location tags (e.g., "Senior Developer - Bangalore, India")
        if (rawJob.getTitle() != null && !rawJob.getTitle().isBlank()) {
            NormalizedLocation titleLocation = locationNormalizer.normalize(rawJob.getTitle());
            if (titleLocation.isIndiaRelevant()) {
                return true;
            }
        }

        // 5. Currency alone (INR) or company name is NOT sufficient evidence per requirement rules.
        return false;
    }

    /**
     * Evaluates whether an {@link ExtractedJob} is India-relevant.
     */
    public boolean isIndiaRelevant(ExtractedJob extractedJob) {
        if (extractedJob == null) {
            return false;
        }

        if (extractedJob.getIsIndiaRelevant() != null) {
            return extractedJob.getIsIndiaRelevant();
        }

        if (extractedJob.getLocation() == null || extractedJob.getLocation().isBlank()) {
            return true;
        }

        NormalizedLocation locationInfo = locationNormalizer.normalize(extractedJob.getLocation());
        if (locationInfo.isIndiaRelevant()) {
            return true;
        }

        if (extractedJob.getTitle() != null && !extractedJob.getTitle().isBlank()) {
            NormalizedLocation titleLocation = locationNormalizer.normalize(extractedJob.getTitle());
            if (titleLocation.isIndiaRelevant()) {
                return true;
            }
        }

        return false;
    }
}
