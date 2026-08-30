package com.dotfield.extractor;

import com.dotfield.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JobExtractionPipeline {

    private final List<JobExtractor> extractors;

    public ExtractedJob extractAndNormalize(Map<String, Object> rawData, String rawSource) {
        if (rawData == null) {
            throw new BadRequestException("Raw job data is required");
        }

        if (rawSource == null || rawSource.trim().isEmpty()) {
            throw new BadRequestException("Source is required");
        }

        String normalizedSource = JobNormalizationUtil.normalizeSource(rawSource);

        JobExtractor extractor = extractors.stream()
                .filter(e -> e.supports(normalizedSource))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Unsupported job source: " + normalizedSource));

        ExtractedJob extractedJob = extractor.extract(rawData, normalizedSource);

        // Normalize text fields
        extractedJob.setTitle(JobNormalizationUtil.normalizeText(extractedJob.getTitle()));
        extractedJob.setCompany(JobNormalizationUtil.normalizeText(extractedJob.getCompany()));
        extractedJob.setLocation(JobNormalizationUtil.normalizeText(extractedJob.getLocation()));
        extractedJob.setDescription(JobNormalizationUtil.normalizeText(extractedJob.getDescription()));
        extractedJob.setJobUrl(JobNormalizationUtil.normalizeText(extractedJob.getJobUrl()));
        extractedJob.setSource(normalizedSource);

        // Normalize salary
        JobNormalizationUtil.ParsedSalary parsedSalary = JobNormalizationUtil.parseSalary(
                null,
                extractedJob.getSalaryMin(),
                extractedJob.getSalaryMax(),
                extractedJob.getCurrency()
        );
        extractedJob.setSalaryMin(parsedSalary.salaryMin());
        extractedJob.setSalaryMax(parsedSalary.salaryMax());
        extractedJob.setCurrency(parsedSalary.currency());

        // Validate required fields
        if (extractedJob.getTitle() == null) {
            throw new BadRequestException("Job title is required");
        }
        if (extractedJob.getCompany() == null) {
            throw new BadRequestException("Company name is required");
        }

        // Validate salary range
        validateSalaryRange(extractedJob.getSalaryMin(), extractedJob.getSalaryMax());

        return extractedJob;
    }

    private void validateSalaryRange(BigDecimal min, BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BadRequestException("Minimum salary cannot be greater than maximum salary");
        }
    }

}
