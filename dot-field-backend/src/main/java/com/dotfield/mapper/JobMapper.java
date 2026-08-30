package com.dotfield.mapper;

import com.dotfield.dto.CreateJobRequest;
import com.dotfield.dto.JobResponse;
import com.dotfield.dto.UpdateJobRequest;
import com.dotfield.entity.Job;
import com.dotfield.entity.JobStatus;
import com.dotfield.extractor.ExtractedJob;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {

    public JobResponse toJobResponse(Job job) {
        if (job == null) {
            return null;
        }

        return JobResponse.builder()
                .id(job.getId())
                .externalId(job.getExternalId())
                .title(job.getTitle())
                .company(job.getCompany())
                .location(job.getLocation())
                .description(job.getDescription())
                .jobUrl(job.getJobUrl())
                .canonicalUrl(job.getCanonicalUrl())
                .source(job.getSource())
                .employmentType(job.getEmploymentType())
                .remoteType(job.getRemoteType())
                .status(job.getStatus())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .currency(job.getCurrency())
                .postedDate(job.getPostedDate())
                .lastDiscoveredAt(job.getLastDiscoveredAt())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    public Job toEntity(CreateJobRequest request) {
        if (request == null) {
            return null;
        }

        String source = request.getSource();
        if (source == null || source.trim().isEmpty()) {
            source = "MANUAL";
        } else {
            source = source.trim().toUpperCase();
        }

        JobStatus status = request.getStatus();
        if (status == null) {
            status = JobStatus.SAVED;
        }

        return Job.builder()
                .title(request.getTitle() != null ? request.getTitle().trim() : null)
                .company(request.getCompany() != null ? request.getCompany().trim() : null)
                .location(request.getLocation() != null ? request.getLocation().trim() : null)
                .description(request.getDescription())
                .jobUrl(request.getJobUrl() != null ? request.getJobUrl().trim() : null)
                .source(source)
                .employmentType(request.getEmploymentType())
                .remoteType(request.getRemoteType())
                .status(status)
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .currency(request.getCurrency() != null ? request.getCurrency().trim() : null)
                .postedDate(request.getPostedDate())
                .build();
    }

    public void updateEntityFromRequest(UpdateJobRequest request, Job job) {
        if (request == null || job == null) {
            return;
        }

        job.setTitle(request.getTitle() != null ? request.getTitle().trim() : null);
        job.setCompany(request.getCompany() != null ? request.getCompany().trim() : null);
        job.setLocation(request.getLocation() != null ? request.getLocation().trim() : null);
        job.setDescription(request.getDescription());
        job.setJobUrl(request.getJobUrl() != null ? request.getJobUrl().trim() : null);

        if (request.getSource() != null && !request.getSource().trim().isEmpty()) {
            job.setSource(request.getSource().trim().toUpperCase());
        }

        job.setEmploymentType(request.getEmploymentType());
        job.setRemoteType(request.getRemoteType());

        if (request.getStatus() != null) {
            job.setStatus(request.getStatus());
        }

        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setCurrency(request.getCurrency() != null ? request.getCurrency().trim() : null);
        job.setPostedDate(request.getPostedDate());
    }

    public Job toEntity(ExtractedJob extractedJob) {
        if (extractedJob == null) {
            return null;
        }

        String source = extractedJob.getSource();
        if (source == null || source.trim().isEmpty()) {
            source = "OTHER";
        } else {
            source = source.trim().toUpperCase();
        }

        return Job.builder()
                .title(extractedJob.getTitle() != null ? extractedJob.getTitle().trim() : null)
                .company(extractedJob.getCompany() != null ? extractedJob.getCompany().trim() : null)
                .location(extractedJob.getLocation() != null ? extractedJob.getLocation().trim() : null)
                .description(extractedJob.getDescription())
                .jobUrl(extractedJob.getJobUrl() != null ? extractedJob.getJobUrl().trim() : null)
                .source(source)
                .employmentType(extractedJob.getEmploymentType())
                .remoteType(extractedJob.getRemoteType())
                .status(JobStatus.SAVED)
                .salaryMin(extractedJob.getSalaryMin())
                .salaryMax(extractedJob.getSalaryMax())
                .currency(extractedJob.getCurrency() != null ? extractedJob.getCurrency().trim() : null)
                .postedDate(extractedJob.getPostedDate())
                .build();
    }

}
