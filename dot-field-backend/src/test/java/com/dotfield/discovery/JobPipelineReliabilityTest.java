package com.dotfield.discovery;

import com.dotfield.entity.Job;
import com.dotfield.entity.JobStatus;
import com.dotfield.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class JobPipelineReliabilityTest {

    @Autowired
    private JobDeduplicationService deduplicationService;

    @Autowired
    private JobRepository jobRepository;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
    }

    @Test
    @DisplayName("Canonical URL stripping: removes utm_source, utm_medium, ref, fbclid parameters")
    void canonicalUrlStrippingTest() {
        String rawUrl = "https://example.com/careers/job-123?utm_source=linkedin&utm_medium=cpc&ref=campaign1&fbclid=abc123xyz&keep=me";
        String canonical = deduplicationService.canonicalizeUrl(rawUrl);

        assertEquals("https://example.com/careers/job-123?keep=me", canonical);
    }

    @Test
    @DisplayName("Deduplication Level 1: Matches by Source and External ID")
    void deduplicationLevel1Test() {
        Job existing = Job.builder()
                .source("LINKEDIN")
                .externalId("JOB-999")
                .title("DevOps Engineer")
                .company("Cloud Co")
                .status(JobStatus.SAVED)
                .build();
        jobRepository.save(existing);

        Optional<Job> match = deduplicationService.findExistingJob("linkedin", "JOB-999", null, null, null, null, null);
        assertTrue(match.isPresent());
        assertEquals("DevOps Engineer", match.get().getTitle());
    }

    @Test
    @DisplayName("Deduplication Level 2: Matches by Canonical URL")
    void deduplicationLevel2Test() {
        String canonical = "https://cloudco.com/jobs/devops";
        Job existing = Job.builder()
                .source("MANUAL")
                .canonicalUrl(canonical)
                .title("DevOps Engineer")
                .company("Cloud Co")
                .status(JobStatus.SAVED)
                .build();
        jobRepository.save(existing);

        Optional<Job> match = deduplicationService.findExistingJob("OTHER", null, "https://cloudco.com/jobs/devops?utm_source=twitter", null, null, null, null);
        assertTrue(match.isPresent());
        assertEquals("DevOps Engineer", match.get().getTitle());
    }

    @Test
    @DisplayName("Deduplication Level 3: Matches by SHA-256 Composite Fingerprint")
    void deduplicationLevel3Test() {
        String fingerprint = deduplicationService.generateFingerprint("Cloud Co", "DevOps Engineer", "Remote", "Kubernetes Docker AWS");
        Job existing = Job.builder()
                .source("MANUAL")
                .deduplicationFingerprint(fingerprint)
                .title("DevOps Engineer")
                .company("Cloud Co")
                .location("Remote")
                .description("Kubernetes Docker AWS")
                .status(JobStatus.SAVED)
                .build();
        jobRepository.save(existing);

        Optional<Job> match = deduplicationService.findExistingJob("DIFFERENT", null, null, "cloud co", "DEVOPS ENGINEER", "remote", "KUBERNETES DOCKER AWS");
        assertTrue(match.isPresent());
        assertEquals(fingerprint, match.get().getDeduplicationFingerprint());
    }

    @Test
    @DisplayName("Distinct jobs with different titles or companies generate distinct fingerprints and do NOT match")
    void distinctJobsDoNotMatchTest() {
        String fp1 = deduplicationService.generateFingerprint("Acme Corp", "Backend Engineer", "Boston, MA", "Java Spring");
        String fp2 = deduplicationService.generateFingerprint("Acme Corp", "Frontend Engineer", "Boston, MA", "React Redux");

        assertNotEquals(fp1, fp2);

        Job existing = Job.builder()
                .source("MANUAL")
                .deduplicationFingerprint(fp1)
                .title("Backend Engineer")
                .company("Acme Corp")
                .location("Boston, MA")
                .status(JobStatus.SAVED)
                .build();
        jobRepository.save(existing);

        Optional<Job> match = deduplicationService.findExistingJob("MANUAL", null, null, "Acme Corp", "Frontend Engineer", "Boston, MA", "React Redux");
        assertFalse(match.isPresent());
    }

    @Test
    @DisplayName("Fingerprint generation returns null if company, title, or location is missing")
    void missingRequiredFieldsReturnsNullFingerprint() {
        assertNull(deduplicationService.generateFingerprint(null, "Title", "Loc", "Desc"));
        assertNull(deduplicationService.generateFingerprint("Company", null, "Loc", "Desc"));
        assertNull(deduplicationService.generateFingerprint("Company", "Title", null, "Desc"));
    }
}
