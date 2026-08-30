package com.dotfield.service;

import com.dotfield.dto.ExtractJobRequest;
import com.dotfield.dto.JobResponse;
import com.dotfield.entity.Job;
import com.dotfield.extractor.ExtractedJob;
import com.dotfield.extractor.JobExtractionPipeline;
import com.dotfield.mapper.JobMapper;
import com.dotfield.repository.JobRepository;
import com.dotfield.repository.JobSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class JobExtractionService {

    private final JobExtractionPipeline extractionPipeline;
    private final JobRepository jobRepository;
    private final JobMapper jobMapper;

    public JobResponse extractAndIngest(ExtractJobRequest request) {
        if (request == null) {
            throw new com.dotfield.exception.BadRequestException("Raw job data is required");
        }

        ExtractedJob extractedJob = extractionPipeline.extractAndNormalize(request.getRawData(), request.getSource());

        // Check for duplicates and log warning
        checkAndLogDuplicate(extractedJob.getSource(), extractedJob.getJobUrl());

        Job job = jobMapper.toEntity(extractedJob);
        Job savedJob = jobRepository.save(job);

        log.info("Extracted and ingested job opportunity ID: {} from source: {}", savedJob.getId(), extractedJob.getSource());
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
}
