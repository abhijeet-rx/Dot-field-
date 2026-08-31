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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private com.dotfield.discovery.JobDeduplicationService deduplicationService;

    @Spy
    private JobMapper jobMapper;

    @InjectMocks
    private JobService jobService;

    private Job sampleJob;
    private CreateJobRequest createRequest;

    @BeforeEach
    void setUp() {
        sampleJob = Job.builder()
                .id(1L)
                .title("Software Engineer")
                .company("Google")
                .location("Mountain View, CA")
                .description("Backend development")
                .jobUrl("https://careers.google.com/jobs/123")
                .source("LINKEDIN")
                .employmentType(EmploymentType.FULL_TIME)
                .remoteType(RemoteType.HYBRID)
                .status(JobStatus.SAVED)
                .salaryMin(new BigDecimal("120000.00"))
                .salaryMax(new BigDecimal("180000.00"))
                .currency("USD")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = CreateJobRequest.builder()
                .title("Software Engineer")
                .company("Google")
                .location("Mountain View, CA")
                .description("Backend development")
                .jobUrl("https://careers.google.com/jobs/123")
                .source("LINKEDIN")
                .employmentType(EmploymentType.FULL_TIME)
                .remoteType(RemoteType.HYBRID)
                .salaryMin(new BigDecimal("120000.00"))
                .salaryMax(new BigDecimal("180000.00"))
                .currency("USD")
                .build();
    }

    @Test
    void createJob_success() {
        when(jobRepository.save(any(Job.class))).thenReturn(sampleJob);

        JobResponse response = jobService.createJob(createRequest);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Software Engineer", response.getTitle());
        assertEquals("Google", response.getCompany());
        assertEquals("LINKEDIN", response.getSource());
        assertEquals(JobStatus.SAVED, response.getStatus());

        verify(jobRepository, times(1)).save(any(Job.class));
    }

    @Test
    void createJob_invalidSalary_throwsBadRequestException() {
        CreateJobRequest invalidSalaryRequest = CreateJobRequest.builder()
                .title("Software Engineer")
                .company("Google")
                .salaryMin(new BigDecimal("200000.00"))
                .salaryMax(new BigDecimal("100000.00"))
                .build();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> jobService.createJob(invalidSalaryRequest));

        assertEquals("Minimum salary cannot be greater than maximum salary", exception.getMessage());
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void getJobById_success() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));

        JobResponse response = jobService.getJobById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Software Engineer", response.getTitle());
    }

    @Test
    void getJobById_notFound_throwsResourceNotFoundException() {
        when(jobRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> jobService.getJobById(999L));

        assertEquals("Job not found with id: 999", exception.getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllJobs_withFiltersAndPagination_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Job> page = new PageImpl<>(List.of(sampleJob), pageable, 1);

        when(jobRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        PagedResponse<JobResponse> result = jobService.getAllJobs(
                JobStatus.SAVED,
                "Google",
                "LINKEDIN",
                RemoteType.HYBRID,
                EmploymentType.FULL_TIME,
                pageable
        );

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(0, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isLast());

        verify(jobRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllJobs_noFilters_success() {
        Pageable pageable = PageRequest.of(0, 5);
        List<Job> jobs = List.of(sampleJob, sampleJob, sampleJob, sampleJob, sampleJob);
        Page<Job> page = new PageImpl<>(jobs, pageable, 10);

        when(jobRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        PagedResponse<JobResponse> result = jobService.getAllJobs(null, null, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(0, result.getPage());
        assertEquals(5, result.getSize());
        assertEquals(10, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        assertFalse(result.isLast());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllJobs_noMatchingResults_returnsEmptyPagedResponse() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Job> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(jobRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

        PagedResponse<JobResponse> result = jobService.getAllJobs(JobStatus.REJECTED, "NonExistent", "OTHER", RemoteType.ONSITE, EmploymentType.CONTRACT, pageable);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getTotalPages());
        assertTrue(result.isLast());
    }

    @Test
    void updateJob_success() {
        UpdateJobRequest updateRequest = UpdateJobRequest.builder()
                .title("Senior Software Engineer")
                .company("Google")
                .location("Remote")
                .employmentType(EmploymentType.FULL_TIME)
                .remoteType(RemoteType.REMOTE)
                .status(JobStatus.APPLIED)
                .salaryMin(new BigDecimal("150000.00"))
                .salaryMax(new BigDecimal("220000.00"))
                .currency("USD")
                .build();

        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));
        when(jobRepository.save(any(Job.class))).thenReturn(sampleJob);

        JobResponse response = jobService.updateJob(1L, updateRequest);

        assertNotNull(response);
        verify(jobRepository, times(1)).save(sampleJob);
    }

    @Test
    void updateJob_nonExistent_throwsResourceNotFoundException() {
        UpdateJobRequest updateRequest = UpdateJobRequest.builder()
                .title("Senior Engineer")
                .company("Google")
                .build();

        when(jobRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> jobService.updateJob(999L, updateRequest));

        assertEquals("Job not found with id: 999", exception.getMessage());
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void updateJob_invalidSalary_throwsBadRequestException() {
        UpdateJobRequest updateRequest = UpdateJobRequest.builder()
                .title("Senior Software Engineer")
                .company("Google")
                .salaryMin(new BigDecimal("250000.00"))
                .salaryMax(new BigDecimal("150000.00"))
                .build();

        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> jobService.updateJob(1L, updateRequest));

        assertEquals("Minimum salary cannot be greater than maximum salary", exception.getMessage());
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void updateJobStatus_success() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));
        when(jobRepository.save(any(Job.class))).thenReturn(sampleJob);

        JobResponse response = jobService.updateJobStatus(1L, JobStatus.INTERVIEW);

        assertNotNull(response);
        assertEquals(JobStatus.INTERVIEW, sampleJob.getStatus());
        verify(jobRepository, times(1)).save(sampleJob);
    }

    @Test
    void updateJobStatus_nonExistent_throwsResourceNotFoundException() {
        when(jobRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> jobService.updateJobStatus(999L, JobStatus.OFFER));

        assertEquals("Job not found with id: 999", exception.getMessage());
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void deleteJob_success() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));
        doNothing().when(jobRepository).delete(sampleJob);

        jobService.deleteJob(1L);

        verify(jobRepository, times(1)).delete(sampleJob);
    }

    @Test
    void deleteJob_notFound_throwsResourceNotFoundException() {
        when(jobRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> jobService.deleteJob(999L));

        assertEquals("Job not found with id: 999", exception.getMessage());
        verify(jobRepository, never()).delete(any(Job.class));
    }

}
