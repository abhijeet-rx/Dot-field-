package com.dotfield.service;

import com.dotfield.dto.CreateJobRequest;
import com.dotfield.dto.JobResponse;
import com.dotfield.dto.PagedResponse;
import com.dotfield.dto.UpdateJobRequest;
import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.Job;
import com.dotfield.entity.JobStatus;
import com.dotfield.entity.RemoteType;
import com.dotfield.exception.BadRequestException;
import com.dotfield.exception.ResourceNotFoundException;
import com.dotfield.mapper.JobMapper;
import com.dotfield.repository.JobRepository;
import com.dotfield.repository.JobSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import com.dotfield.discovery.JobDeduplicationService;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobMapper jobMapper;
    private final JobDeduplicationService deduplicationService;

    public JobResponse createJob(CreateJobRequest request) {
        validateSalaryRange(request.getSalaryMin(), request.getSalaryMax());

        Job job = jobMapper.toEntity(request);
        if (job.getJobUrl() != null && !job.getJobUrl().isBlank()) {
            job.setCanonicalUrl(deduplicationService.canonicalizeUrl(job.getJobUrl()));
        }
        if (job.getDeduplicationFingerprint() == null) {
            job.setDeduplicationFingerprint(deduplicationService.generateFingerprint(
                    job.getCompany(), job.getTitle(), job.getLocation(), job.getDescription()
            ));
        }
        Job savedJob = jobRepository.save(job);
        log.info("Created job opportunity with ID: {}", savedJob.getId());
        return jobMapper.toJobResponse(savedJob);
    }

    @Transactional(readOnly = true)
    public JobResponse getJobById(Long id) {
        Job job = findJobEntityById(id);
        return jobMapper.toJobResponse(job);
    }

    @Transactional(readOnly = true)
    public PagedResponse<JobResponse> getAllJobs(
            JobStatus status,
            String company,
            String source,
            RemoteType remoteType,
            EmploymentType employmentType,
            Pageable pageable) {

        Specification<Job> spec = JobSpecification.withFilters(status, company, source, remoteType, employmentType);
        Page<Job> page = jobRepository.findAll(spec, pageable);
        Page<JobResponse> dtoPage = page.map(jobMapper::toJobResponse);
        return PagedResponse.fromPage(dtoPage);
    }

    public JobResponse updateJob(Long id, UpdateJobRequest request) {
        Job job = findJobEntityById(id);
        validateSalaryRange(request.getSalaryMin(), request.getSalaryMax());

        jobMapper.updateEntityFromRequest(request, job);
        if (job.getJobUrl() != null && !job.getJobUrl().isBlank()) {
            job.setCanonicalUrl(deduplicationService.canonicalizeUrl(job.getJobUrl()));
        }
        job.setDeduplicationFingerprint(deduplicationService.generateFingerprint(
                job.getCompany(), job.getTitle(), job.getLocation(), job.getDescription()
        ));
        Job updatedJob = jobRepository.save(job);
        log.info("Updated job opportunity ID: {}", id);
        return jobMapper.toJobResponse(updatedJob);
    }

    public JobResponse updateJobStatus(Long id, JobStatus status) {
        Job job = findJobEntityById(id);
        job.setStatus(status);
        Job updatedJob = jobRepository.save(job);
        log.info("Updated job status for ID: {} to {}", id, status);
        return jobMapper.toJobResponse(updatedJob);
    }

    public void deleteJob(Long id) {
        Job job = findJobEntityById(id);
        jobRepository.delete(job);
        log.info("Deleted job opportunity ID: {}", id);
    }

    private Job findJobEntityById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
    }

    private void validateSalaryRange(BigDecimal min, BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BadRequestException("Minimum salary cannot be greater than maximum salary");
        }
    }
}
