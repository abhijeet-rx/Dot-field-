package com.dotfield.discovery.india;

import com.dotfield.dto.RawJobListing;
import com.dotfield.extractor.ExtractedJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Centralized filter for evaluating whether a job listing is India-relevant.
 * Evaluated both BEFORE expensive extraction (on RawJobListing) and AFTER extraction (on ExtractedJob).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndiaJobFilter {

    private final IndiaLocationNormalizer locationNormalizer;

    /**
     * Evaluates whether a {@link RawJobListing} is India-relevant.
     * Missing or unpopulated location defaults to false (insufficient evidence)
     * unless explicitly pre-classified as true by a trusted source adapter.
     */
    public boolean isIndiaRelevant(RawJobListing rawJob) {
        if (rawJob == null) {
            return false;
        }

        // 1. Explicit pre-classification flag set by a trusted source adapter (e.g., CompanyCareerPageSource)
        if (Boolean.TRUE.equals(rawJob.getIsIndiaRelevant())) {
            return true;
        }
        if (Boolean.FALSE.equals(rawJob.getIsIndiaRelevant())) {
            return false;
        }

        // 2. Location missing -> insufficient evidence -> reject per Rule 5
        if (rawJob.getLocation() != null && !rawJob.getLocation().isBlank()) {
            NormalizedLocation locationInfo = locationNormalizer.normalize(rawJob.getLocation());
            if (locationInfo.isIndiaRelevant()) {
                return true;
            }
        }

        // 3. Check title for explicit Indian location tags (e.g., "Senior Developer - Bangalore, India")
        if (rawJob.getTitle() != null && !rawJob.getTitle().isBlank()) {
            NormalizedLocation titleLocation = locationNormalizer.normalize(rawJob.getTitle());
            if (titleLocation.isIndiaRelevant()) {
                return true;
            }
        }

        // 4. Currency alone (INR) or company name is NOT sufficient evidence per Rule 7.
        return false;
    }

    /**
     * Evaluates whether an {@link ExtractedJob} is India-relevant post-extraction.
     */
    public boolean isIndiaRelevant(ExtractedJob extractedJob) {
        if (extractedJob == null) {
            return false;
        }

        if (Boolean.FALSE.equals(extractedJob.getIsIndiaRelevant())) {
            return false;
        }

        if (extractedJob.getLocation() != null && !extractedJob.getLocation().isBlank()) {
            NormalizedLocation locationInfo = locationNormalizer.normalize(extractedJob.getLocation());
            return locationInfo.isIndiaRelevant();
        }

        if (extractedJob.getTitle() != null && !extractedJob.getTitle().isBlank()) {
            NormalizedLocation titleLocation = locationNormalizer.normalize(extractedJob.getTitle());
            if (titleLocation.isIndiaRelevant()) {
                return true;
            }
        }

        return Boolean.TRUE.equals(extractedJob.getIsIndiaRelevant());
    }
}
