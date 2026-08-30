package com.dotfield.service;

import com.dotfield.dto.ExtractJobRequest;
import com.dotfield.dto.JobResponse;
import com.dotfield.entity.Job;
import com.dotfield.exception.BadRequestException;
import com.dotfield.extractor.ExtractedJob;
import com.dotfield.extractor.JobExtractor;
import com.dotfield.extractor.JobNormalizationUtil;
import com.dotfield.mapper.JobMapper;
import com.dotfield.repository.JobRepository;
import com.dotfield.repository.JobSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class JobExtractionService {

    private final List<JobExtractor> extractors;
    private final JobRepository jobRepository;
    private final JobMapper jobMapper;

    public JobResponse extractAndIngest(ExtractJobRequest request) {
        if (request == null || request.getRawData() == null) {
            throw new BadRequestException("Raw job data is required");
        }

        String rawSource = request.getSource();
        if (rawSource == null || rawSource.trim().isEmpty()) {
            throw new BadRequestException("Source is required");
        }

        String normalizedSource = JobNormalizationUtil.normalizeSource(rawSource);

        JobExtractor extractor = extractors.stream()
                .filter(e -> e.supports(normalizedSource))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Unsupported job source: " + normalizedSource));

        ExtractedJob extractedJob = extractor.extract(request.getRawData(), normalizedSource);

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

        // Check for duplicates and log warning
        checkAndLogDuplicate(normalizedSource, extractedJob.getJobUrl());

        Job job = jobMapper.toEntity(extractedJob);
        Job savedJob = jobRepository.save(job);

        log.info("Extracted and ingested job opportunity ID: {} from source: {}", savedJob.getId(), normalizedSource);
        return jobMapper.toJobResponse(savedJob);
    }

    private void checkAndLogDuplicate(String source, String jobUrl) {
        if (jobUrl != null && !jobUrl.trim().isEmpty()) {
            Specification<Job> spec = JobSpecification.withFilters(null, null, source, null, null);
            List<Job> matches = jobRepository.findAll(spec);
            boolean duplicateExists = matches.stream()
                    .anyMatch(j -> jobUrl.equalsIgnoreCase(j.getJobUrl()));
            if (duplicateExists) {
                log.warn("Duplicate job listing detected for source: {} and jobUrl: {}", source, jobUrl);
            }
        }
    }

    private void validateSalaryRange(BigDecimal min, BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BadRequestException("Minimum salary cannot be greater than maximum salary");
        }
    }
}
