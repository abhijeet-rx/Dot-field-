package com.dotfield.service;

import com.dotfield.dto.CreateJobRequest;
import com.dotfield.dto.JobResponse;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

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
