package com.dotfield.service;

import com.dotfield.dto.ExtractJobRequest;
import com.dotfield.dto.JobResponse;
import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.Job;
import com.dotfield.entity.JobStatus;
import com.dotfield.entity.RemoteType;
import com.dotfield.exception.BadRequestException;
import com.dotfield.extractor.CompanyWebsiteJobExtractor;
import com.dotfield.extractor.JobExtractor;
import com.dotfield.mapper.JobMapper;
import com.dotfield.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobExtractionServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Spy
    private JobMapper jobMapper;

    private JobExtractionService jobExtractionService;

    private Job sampleJob;

    @BeforeEach
    void setUp() {
        JobExtractor companyWebsiteExtractor = new CompanyWebsiteJobExtractor();
        List<JobExtractor> extractors = List.of(companyWebsiteExtractor);
        jobExtractionService = new JobExtractionService(extractors, jobRepository, jobMapper);

        sampleJob = Job.builder()
                .id(1L)
                .title("Backend Engineer")
                .company("Acme Corp")
                .source("COMPANY_WEBSITE")
                .status(JobStatus.SAVED)
                .employmentType(EmploymentType.FULL_TIME)
                .remoteType(RemoteType.REMOTE)
                .salaryMin(new BigDecimal("100000.00"))
                .salaryMax(new BigDecimal("150000.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void extractAndIngest_supportedSource_success() {
        var rawData = new HashMap<String, Object>();
        rawData.put("title", "Backend Engineer");
        rawData.put("company", "Acme Corp");
        rawData.put("employmentType", "Full Time");
        rawData.put("remoteType", "Remote");
        rawData.put("salaryMin", 100000);
        rawData.put("salaryMax", 150000);

        ExtractJobRequest request = ExtractJobRequest.builder()
                .source("COMPANY_WEBSITE")
                .rawData(rawData)
                .build();

        when(jobRepository.save(any(Job.class))).thenReturn(sampleJob);

        JobResponse response = jobExtractionService.extractAndIngest(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Backend Engineer", response.getTitle());
        assertEquals("Acme Corp", response.getCompany());
        assertEquals("COMPANY_WEBSITE", response.getSource());

        verify(jobRepository, times(1)).save(any(Job.class));
    }

    @Test
    void extractAndIngest_unsupportedSource_throwsBadRequestException() {
        ExtractJobRequest request = ExtractJobRequest.builder()
                .source("LINKEDIN")
                .rawData(HashMap.newHashMap(0))
                .build();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> jobExtractionService.extractAndIngest(request));

        assertEquals("Unsupported job source: LINKEDIN", exception.getMessage());
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void extractAndIngest_missingTitleOrCompany_throwsBadRequestException() {
        var rawData = new HashMap<String, Object>();
        rawData.put("company", "Acme Corp"); // missing title

        ExtractJobRequest request = ExtractJobRequest.builder()
                .source("COMPANY_WEBSITE")
                .rawData(rawData)
                .build();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> jobExtractionService.extractAndIngest(request));

        assertEquals("Job title is required", exception.getMessage());
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void extractAndIngest_invalidSalaryRange_throwsBadRequestException() {
        var rawData = new HashMap<String, Object>();
        rawData.put("title", "Backend Engineer");
        rawData.put("company", "Acme Corp");
        rawData.put("salaryMin", 200000);
        rawData.put("salaryMax", 100000);

        ExtractJobRequest request = ExtractJobRequest.builder()
                .source("COMPANY_WEBSITE")
                .rawData(rawData)
                .build();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> jobExtractionService.extractAndIngest(request));

        assertEquals("Minimum salary cannot be greater than maximum salary", exception.getMessage());
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void extractAndIngest_duplicateJobUrl_logsWarningAndIngests() {
        var rawData = new HashMap<String, Object>();
        rawData.put("title", "Backend Engineer");
        rawData.put("company", "Acme Corp");
        rawData.put("jobUrl", "https://acme.com/jobs/123");

        ExtractJobRequest request = ExtractJobRequest.builder()
                .source("COMPANY_WEBSITE")
                .rawData(rawData)
                .build();

        Job existingJob = Job.builder().id(99L).source("COMPANY_WEBSITE").jobUrl("https://acme.com/jobs/123").build();
        when(jobRepository.findAll(any(Specification.class))).thenReturn(List.of(existingJob));
        when(jobRepository.save(any(Job.class))).thenReturn(sampleJob);

        JobResponse response = jobExtractionService.extractAndIngest(request);

        assertNotNull(response);
        verify(jobRepository, times(1)).save(any(Job.class));
    }

}
