package com.dotfield.discovery;

import com.dotfield.dto.CreateApplicationRequest;
import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.JobDiscoveryResponse;
import com.dotfield.dto.RawJobListing;
import com.dotfield.entity.Application;
import com.dotfield.entity.Job;
import com.dotfield.entity.JobStatus;
import com.dotfield.entity.Profile;
import com.dotfield.entity.Role;
import com.dotfield.entity.User;
import com.dotfield.repository.ApplicationRepository;
import com.dotfield.repository.JobRepository;
import com.dotfield.repository.ProfileRepository;
import com.dotfield.repository.UserRepository;
import com.dotfield.service.ApplicationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class JobFreshnessLifecycleTest {

    @Autowired
    private JobDiscoveryService discoveryService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private JobDeduplicationService deduplicationService;

    @MockBean
    private JobSourceRegistry sourceRegistry;

    private JobSource mockJobSource;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        applicationRepository.deleteAll();
        jobRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        mockJobSource = org.mockito.Mockito.mock(JobSource.class);
        when(sourceRegistry.getRequiredSource("REMOTIVE")).thenReturn(mockJobSource);
        when(mockJobSource.getSourceName()).thenReturn("REMOTIVE");
        discoveryService.setFreshnessThresholdDays(7);
    }

    @Test
    @DisplayName("1. Newly Seen Job — Tracks firstSeenAt, lastSeenAt, postedAt, and sets status to ACTIVE")
    void discoverJobs_newlySeenJob_setsFreshnessFieldsAndActiveStatus() {
        LocalDate postedDate = LocalDate.now().minusDays(2);
        RawJobListing rawListing = RawJobListing.builder()
                .externalId("REM-101")
                .title("Senior Java Architect")
                .company("CloudCorp")
                .location("Remote")
                .jobUrl("https://remotive.com/jobs/101")
                .postedDate(postedDate)
                .build();

        JobDiscoveryRequest request = JobDiscoveryRequest.builder().source("REMOTIVE").maxResults(10).build();
        when(mockJobSource.discover(request)).thenReturn(List.of(rawListing));

        LocalDateTime beforeRun = LocalDateTime.now().minusSeconds(2);
        JobDiscoveryResponse response = discoveryService.discoverJobs(request);

        assertEquals(1, response.getNewJobs());

        Job job = jobRepository.findBySourceAndExternalId("REMOTIVE", "REM-101").orElseThrow();
        assertEquals(JobStatus.ACTIVE, job.getStatus());
        assertEquals("REMOTIVE", job.getSource());
        assertEquals(postedDate, job.getPostedDate());
        assertEquals(postedDate, job.getPostedAt());
        assertNotNull(job.getFirstSeenAt());
        assertNotNull(job.getLastSeenAt());
        assertTrue(job.getFirstSeenAt().isAfter(beforeRun) || job.getFirstSeenAt().isEqual(beforeRun));
        assertTrue(job.getLastSeenAt().isAfter(beforeRun) || job.getLastSeenAt().isEqual(beforeRun));
    }

    @Test
    @DisplayName("2. Repeated Job — Updates lastSeenAt to now while preserving firstSeenAt")
    void discoverJobs_repeatedJob_updatesLastSeenAtPreservesFirstSeenAt() throws InterruptedException {
        LocalDateTime originalFirstSeen = LocalDateTime.now().minusDays(5).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        LocalDateTime originalLastSeen = LocalDateTime.now().minusDays(2).truncatedTo(java.time.temporal.ChronoUnit.MICROS);

        String fingerprint = deduplicationService.generateFingerprint("CloudCorp", "Backend Engineer", "Remote", "Build scalable distributed backend services");

        Job existingJob = Job.builder()
                .externalId("REM-102")
                .title("Backend Engineer")
                .company("CloudCorp")
                .location("Remote")
                .description("Build scalable distributed backend services")
                .jobUrl("https://remotive.com/jobs/102")
                .canonicalUrl("https://remotive.com/jobs/102")
                .deduplicationFingerprint(fingerprint)
                .source("REMOTIVE")
                .employmentType(com.dotfield.entity.EmploymentType.FULL_TIME)
                .remoteType(com.dotfield.entity.RemoteType.REMOTE)
                .status(JobStatus.ACTIVE)
                .firstSeenAt(originalFirstSeen)
                .lastSeenAt(originalLastSeen)
                .lastDiscoveredAt(originalLastSeen)
                .build();
        jobRepository.saveAndFlush(existingJob);

        RawJobListing rawListing = RawJobListing.builder()
                .externalId("REM-102")
                .title("Backend Engineer")
                .company("CloudCorp")
                .location("Remote")
                .description("Build scalable distributed backend services")
                .jobUrl("https://remotive.com/jobs/102")
                .employmentType(com.dotfield.entity.EmploymentType.FULL_TIME)
                .remoteType(com.dotfield.entity.RemoteType.REMOTE)
                .build();

        JobDiscoveryRequest request = JobDiscoveryRequest.builder().source("REMOTIVE").maxResults(10).build();
        when(mockJobSource.discover(request)).thenReturn(List.of(rawListing));

        LocalDateTime beforeRun = LocalDateTime.now().minusSeconds(1);
        JobDiscoveryResponse response = discoveryService.discoverJobs(request);

        assertEquals(1, response.getUnchangedJobs());

        Job refreshedJob = jobRepository.findById(existingJob.getId()).orElseThrow();
        assertEquals(originalFirstSeen, refreshedJob.getFirstSeenAt());
        assertTrue(refreshedJob.getLastSeenAt().isAfter(beforeRun));
    }

    @Test
    @DisplayName("3. Stale Job — Jobs older than threshold get marked EXPIRED on successful source run")
    void expireStaleJobs_jobOlderThanThreshold_markedExpired() {
        LocalDateTime staleTime = LocalDateTime.now().minusDays(10);

        Job staleJob = Job.builder()
                .externalId("REM-103")
                .title("DevOps Lead")
                .company("CloudCorp")
                .source("REMOTIVE")
                .status(JobStatus.ACTIVE)
                .firstSeenAt(staleTime.minusDays(5))
                .lastSeenAt(staleTime)
                .lastDiscoveredAt(staleTime)
                .build();
        jobRepository.saveAndFlush(staleJob);

        RawJobListing currentListing = RawJobListing.builder()
                .externalId("REM-104")
                .title("Frontend Developer")
                .company("CloudCorp")
                .build();

        JobDiscoveryRequest request = JobDiscoveryRequest.builder().source("REMOTIVE").maxResults(10).build();
        when(mockJobSource.discover(request)).thenReturn(List.of(currentListing));

        discoveryService.discoverJobs(request);

        Job updatedStaleJob = jobRepository.findById(staleJob.getId()).orElseThrow();
        assertEquals(JobStatus.EXPIRED, updatedStaleJob.getStatus());
    }

    @Test
    @DisplayName("4. Failed Source Run — Job does NOT become expired when ingestion request fails")
    void discoverJobs_failedSourceRun_doesNotExpireStaleJobs() {
        LocalDateTime staleTime = LocalDateTime.now().minusDays(10);

        Job staleJob = Job.builder()
                .externalId("REM-105")
                .title("QA Manager")
                .company("CloudCorp")
                .source("REMOTIVE")
                .status(JobStatus.ACTIVE)
                .firstSeenAt(staleTime.minusDays(5))
                .lastSeenAt(staleTime)
                .lastDiscoveredAt(staleTime)
                .build();
        jobRepository.saveAndFlush(staleJob);

        JobDiscoveryRequest request = JobDiscoveryRequest.builder().source("REMOTIVE").maxResults(10).build();
        when(mockJobSource.discover(request)).thenThrow(new RuntimeException("API connection timeout"));

        JobDiscoveryResponse response = discoveryService.discoverJobs(request);
        assertEquals(1, response.getFailed());

        Job untouchedJob = jobRepository.findById(staleJob.getId()).orElseThrow();
        assertEquals(JobStatus.ACTIVE, untouchedJob.getStatus());
    }

    @Test
    @DisplayName("5. Successful Source Run Where Job Disappears — Unseen job beyond threshold transitions to EXPIRED")
    void discoverJobs_successfulRunWhereJobDisappears_marksJobExpired() {
        LocalDateTime oldLastSeen = LocalDateTime.now().minusDays(8);

        Job disappearedJob = Job.builder()
                .externalId("REM-106")
                .title("Legacy Specialist")
                .company("CloudCorp")
                .source("REMOTIVE")
                .status(JobStatus.ACTIVE)
                .firstSeenAt(oldLastSeen.minusDays(10))
                .lastSeenAt(oldLastSeen)
                .lastDiscoveredAt(oldLastSeen)
                .build();
        jobRepository.saveAndFlush(disappearedJob);

        // Source returns empty list (job disappeared)
        JobDiscoveryRequest request = JobDiscoveryRequest.builder().source("REMOTIVE").maxResults(10).build();
        when(mockJobSource.discover(request)).thenReturn(List.of());

        JobDiscoveryResponse response = discoveryService.discoverJobs(request);
        assertEquals(0, response.getDiscovered());

        Job updatedJob = jobRepository.findById(disappearedJob.getId()).orElseThrow();
        assertEquals(JobStatus.EXPIRED, updatedJob.getStatus());
    }

    @Test
    @Transactional
    @DisplayName("6. Application Referencing Expired Job — Candidate application reference remains valid after job expires")
    void applicationReferencingExpiredJob_remainsValidAndIntact() {
        LocalDateTime oldLastSeen = LocalDateTime.now().minusDays(9);

        Job job = Job.builder()
                .externalId("REM-107")
                .title("Data Scientist")
                .company("CloudCorp")
                .source("REMOTIVE")
                .status(JobStatus.ACTIVE)
                .firstSeenAt(oldLastSeen.minusDays(5))
                .lastSeenAt(oldLastSeen)
                .lastDiscoveredAt(oldLastSeen)
                .build();
        Job savedJob = jobRepository.saveAndFlush(job);

        User user = User.builder()
                .email("candidate@dotfield.com")
                .passwordHash("hashed")
                .role(Role.USER)
                .build();
        User savedUser = userRepository.saveAndFlush(user);

        Profile profile = Profile.builder()
                .user(savedUser)
                .name("Jane Candidate")
                .email("candidate@dotfield.com")
                .build();
        Profile savedProfile = profileRepository.saveAndFlush(profile);

        Application application = Application.builder()
                .profile(savedProfile)
                .job(savedJob)
                .status(com.dotfield.entity.ApplicationStatus.SAVED)
                .build();
        Application savedApp = applicationRepository.saveAndFlush(application);
        assertNotNull(savedApp.getId());

        // Run ingestion with zero listings to expire the job
        JobDiscoveryRequest discoveryRequest = JobDiscoveryRequest.builder().source("REMOTIVE").maxResults(10).build();
        when(mockJobSource.discover(any())).thenReturn(List.of());
        discoveryService.discoverJobs(discoveryRequest);

        Job expiredJob = jobRepository.findById(savedJob.getId()).orElseThrow();
        assertEquals(JobStatus.EXPIRED, expiredJob.getStatus());

        Application fetchedApp = applicationRepository.findById(savedApp.getId()).orElseThrow();
        assertNotNull(fetchedApp);
        assertEquals(savedJob.getId(), fetchedApp.getJob().getId());
        assertEquals("Data Scientist", fetchedApp.getJob().getTitle());
        assertEquals(JobStatus.EXPIRED, fetchedApp.getJob().getStatus());
    }
}
