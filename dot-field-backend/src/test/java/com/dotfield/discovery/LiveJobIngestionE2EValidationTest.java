package com.dotfield.discovery;

import com.dotfield.discovery.source.RemotiveJobSource;
import com.dotfield.dto.CreateApplicationRequest;
import com.dotfield.dto.IngestionStatusResponse;
import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.JobIngestionRunResponse;
import com.dotfield.dto.JobMatchResponse;
import com.dotfield.entity.*;
import com.dotfield.repository.*;
import com.dotfield.service.ApplicationService;
import com.dotfield.service.JobMatchingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Tag("live-integration")
@Disabled("Manual E2E validation against live external third-party API — enabled on-demand for live verification")
public class LiveJobIngestionE2EValidationTest {

    @Autowired
    private RemotiveJobSource remotiveJobSource;

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
    private JobMatchingService jobMatchingService;

    @Autowired
    private ApplicationService applicationService;

    @BeforeEach
    void setUp() {
        applicationRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Test
    @DisplayName("E2E Live Validation — Fetch real jobs from Remotive API, normalize, deduplicate, and persist")
    void validateLiveRemotiveApiIngestionAndDeduplication() {
        // Step 1: Direct live fetch from Remotive API
        JobDiscoveryRequest fetchRequest = JobDiscoveryRequest.builder()
                .source("REMOTIVE")
                .maxResults(15)
                .build();

        var rawListings = remotiveJobSource.discover(fetchRequest);
        assertNotNull(rawListings, "Raw listings from live Remotive API should not be null");
        assertFalse(rawListings.isEmpty(), "Live Remotive API should return at least 1 real job listing");

        System.out.println(">>> LIVE FETCH: Discovered " + rawListings.size() + " real jobs from Remotive API");
        rawListings.stream().limit(3).forEach(job -> {
            System.out.println("    - [Real Job] Title: " + job.getTitle() + " | Company: " + job.getCompany() + " | URL: " + job.getJobUrl());
        });

        // Step 2: Ingest into database via JobDiscoveryService
        JobIngestionRunResponse firstRun = discoveryService.runManualIngestion(fetchRequest);
        assertNotNull(firstRun);
        assertTrue(firstRun.getJobsFetched() > 0, "Jobs fetched should be > 0");
        assertTrue(firstRun.getJobsInserted() > 0, "Jobs inserted should be > 0 on first run");

        long storedJobCount = jobRepository.count();
        assertEquals(firstRun.getJobsInserted(), storedJobCount, "Stored DB count must match inserted jobs count");

        // Step 3: Verify important fields on stored entities
        List<Job> storedJobs = jobRepository.findAll();
        Job sampleJob = storedJobs.get(0);
        assertNotNull(sampleJob.getTitle(), "Stored job title must not be null");
        assertNotNull(sampleJob.getCompany(), "Stored job company must not be null");
        assertNotNull(sampleJob.getJobUrl(), "Stored job URL must not be null");
        assertEquals("REMOTIVE", sampleJob.getSource(), "Stored job source must be REMOTIVE");
        assertEquals(JobStatus.ACTIVE, sampleJob.getStatus(), "Freshly ingested job status must be ACTIVE");

        // Step 4: Idempotency check — execute second ingestion pass with identical data
        JobIngestionRunResponse secondRun = discoveryService.runManualIngestion(fetchRequest);
        assertNotNull(secondRun);
        assertEquals(0, secondRun.getJobsInserted(), "Second pass must insert ZERO new jobs (idempotence)");
        assertTrue(secondRun.getDuplicates() > 0, "Second pass must record duplicate listings");
        assertEquals(storedJobCount, jobRepository.count(), "DB count must remain identical after second pass");

        // Step 5: Verify monitoring status
        IngestionStatusResponse monitoringStatus = discoveryService.getIngestionStatus();
        assertNotNull(monitoringStatus.getLastRun());
        assertTrue(monitoringStatus.getSourcesProcessed() >= 1);

        System.out.println(">>> LIVE E2E SUCCESS: 1st Run Inserted: " + firstRun.getJobsInserted() + " | 2nd Run Inserted: " + secondRun.getJobsInserted() + " (Duplicates: " + secondRun.getDuplicates() + ")");
    }

    @Test
    @DisplayName("E2E Live Validation — Serves live jobs via API, calculates fit score, and tracks application")
    void validateLiveIngestionJobApiAndMatchingFlow() {
        // Step 1: Run ingestion
        JobIngestionRunResponse runSummary = discoveryService.runManualIngestion(
                JobDiscoveryRequest.builder().source("REMOTIVE").maxResults(10).build()
        );
        assertTrue(runSummary.getJobsInserted() > 0);

        List<Job> liveJobs = jobRepository.findAll();
        Job testJob = liveJobs.get(0);

        // Step 2: Create a candidate user profile for matching & application tracking
        User testUser = User.builder()
                .email("live_candidate@dotfield.dev")
                .passwordHash("hashed_password_123")
                .role(Role.USER)
                .build();
        userRepository.save(testUser);

        Profile profile = Profile.builder()
                .user(testUser)
                .name("Live Candidate")
                .email("live_candidate@dotfield.dev")
                .build();
        profileRepository.save(profile);

        // Authenticate as testUser in Spring Security Context
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        testUser.getId(),
                        null,
                        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
                );
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        // Step 3: Analyze fit score for live job record
        JobMatchResponse match = jobMatchingService.analyzeJob(testJob.getId());
        assertNotNull(match);
        assertNotNull(match.getMatchCategory());
        assertTrue(match.getOverallScore() >= 0 && match.getOverallScore() <= 100);

        // Step 4: Track application on live job record
        CreateApplicationRequest appReq = CreateApplicationRequest.builder()
                .jobId(testJob.getId())
                .status(ApplicationStatus.SAVED)
                .notes("Tracked from live E2E test")
                .build();

        var appResponse = applicationService.createApplication(testUser.getId(), appReq);
        assertNotNull(appResponse);
        assertEquals(testJob.getId(), appResponse.getJob().getId());
        assertNotNull(appResponse.getFitScore(), "Application fit score snapshot should be recorded");

        System.out.println(">>> LIVE MATCH & TRACKING SUCCESS: Job ID: " + testJob.getId() + " | Title: " + testJob.getTitle() + " | Score: " + match.getOverallScore() + "% (" + match.getMatchCategory() + ") | App ID: " + appResponse.getId());

        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
}
