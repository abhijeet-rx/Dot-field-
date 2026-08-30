package com.dotfield.discovery;

import com.dotfield.entity.Job;
import com.dotfield.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobDeduplicationServiceTest {

    @Mock
    private JobRepository jobRepository;

    private JobDeduplicationService deduplicationService;

    @BeforeEach
    void setUp() {
        deduplicationService = new JobDeduplicationService(jobRepository);
    }

    @Test
    void canonicalizeUrl_stripsTrackingParams_andPreservesFunctionalParams() {
        String inputUrl = "https://example.com/careers/job/101/?utm_source=linkedin&utm_medium=cpc&location=bangalore#section";
        String canonical = deduplicationService.canonicalizeUrl(inputUrl);

        assertEquals("https://example.com/careers/job/101?location=bangalore", canonical);
    }

    @Test
    void canonicalizeUrl_httpAndHttpsRemainDistinct() {
        String httpUrl = deduplicationService.canonicalizeUrl("http://example.com/job/101");
        String httpsUrl = deduplicationService.canonicalizeUrl("https://example.com/job/101");

        assertNotEquals(httpUrl, httpsUrl);
        assertEquals("http://example.com/job/101", httpUrl);
        assertEquals("https://example.com/job/101", httpsUrl);
    }

    @Test
    void generateFingerprint_sameDetails_producesSameHash() {
        String fp1 = deduplicationService.generateFingerprint("Acme Corp", "Java Engineer", "Bangalore", "Looking for Java devs");
        String fp2 = deduplicationService.generateFingerprint("ACME CORP", "java engineer", "bangalore", "Looking for java devs");

        assertNotNull(fp1);
        assertEquals(fp1, fp2);
    }

    @Test
    void generateFingerprint_differentLocation_producesDifferentHash() {
        String fpBangalore = deduplicationService.generateFingerprint("Acme Corp", "Java Engineer", "Bangalore", "Java devs");
        String fpHyderabad = deduplicationService.generateFingerprint("Acme Corp", "Java Engineer", "Hyderabad", "Java devs");

        assertNotEquals(fpBangalore, fpHyderabad);
    }

    @Test
    void generateFingerprint_missingDescription_returnsFallbackHashWithoutCrashing() {
        String fpWithDescNull = deduplicationService.generateFingerprint("Acme Corp", "Java Engineer", "Bangalore", null);
        String fpWithDescBlank = deduplicationService.generateFingerprint("Acme Corp", "Java Engineer", "Bangalore", "   ");

        assertNotNull(fpWithDescNull);
        assertEquals(fpWithDescNull, fpWithDescBlank);
    }

    @Test
    void findExistingJob_level1ExternalIdMatch() {
        Job existingJob = Job.builder().id(1L).source("COMPANY_WEBSITE").externalId("EXT-100").build();
        when(jobRepository.findBySourceAndExternalId("COMPANY_WEBSITE", "EXT-100")).thenReturn(Optional.of(existingJob));

        Optional<Job> match = deduplicationService.findExistingJob(
                "COMPANY_WEBSITE", "EXT-100", "https://example.com/job/100", "Acme", "Dev", "Bangalore", "Desc"
        );

        assertTrue(match.isPresent());
        assertEquals(1L, match.get().getId());
        verify(jobRepository).findBySourceAndExternalId("COMPANY_WEBSITE", "EXT-100");
    }

    @Test
    void findExistingJob_level2CanonicalUrlMatch() {
        when(jobRepository.findBySourceAndExternalId(anyString(), anyString())).thenReturn(Optional.empty());
        Job existingJob = Job.builder().id(2L).canonicalUrl("https://example.com/job/100").build();
        when(jobRepository.findByCanonicalUrl("https://example.com/job/100")).thenReturn(Optional.of(existingJob));

        Optional<Job> match = deduplicationService.findExistingJob(
                "COMPANY_WEBSITE", "EXT-100", "https://example.com/job/100?utm_source=test", "Acme", "Dev", "Bangalore", "Desc"
        );

        assertTrue(match.isPresent());
        assertEquals(2L, match.get().getId());
        verify(jobRepository).findByCanonicalUrl("https://example.com/job/100");
    }

    @Test
    void findExistingJob_level3FingerprintMatch() {
        when(jobRepository.findBySourceAndExternalId(anyString(), anyString())).thenReturn(Optional.empty());
        when(jobRepository.findByCanonicalUrl(anyString())).thenReturn(Optional.empty());

        Job existingJob = Job.builder().id(3L).build();
        when(jobRepository.findByDeduplicationFingerprint(anyString())).thenReturn(Optional.of(existingJob));

        Optional<Job> match = deduplicationService.findExistingJob(
                "COMPANY_WEBSITE", "EXT-100", "https://example.com/job/100", "Acme", "Dev", "Bangalore", "Desc"
        );

        assertTrue(match.isPresent());
        assertEquals(3L, match.get().getId());
        verify(jobRepository).findByDeduplicationFingerprint(anyString());
    }

}
