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

    // ──────────────────────────────────────────────
    // Canonical URL normalization tests
    // ──────────────────────────────────────────────

    @Test
    void canonicalizeUrl_stripsTrackingParams_andPreservesFunctionalParams() {
        String inputUrl = "https://example.com/careers/job/101/?utm_source=linkedin&utm_medium=cpc&location=bangalore#section";
        String canonical = deduplicationService.canonicalizeUrl(inputUrl);

        assertEquals("https://example.com/careers/job/101?location=bangalore", canonical);
    }

    @Test
    void canonicalizeUrl_httpAndHttpsNormalizedToHttpsWhenCanonicalizeSchemeIsTrue() {
        deduplicationService.setCanonicalizeScheme(true);
        String httpUrl = deduplicationService.canonicalizeUrl("http://example.com/job/101");
        String httpsUrl = deduplicationService.canonicalizeUrl("https://example.com/job/101");

        assertEquals(httpUrl, httpsUrl);
        assertEquals("https://example.com/job/101", httpUrl);
    }

    @Test
    void canonicalizeUrl_httpAndHttpsRemainDistinctWhenCanonicalizeSchemeIsFalse() {
        deduplicationService.setCanonicalizeScheme(false);
        String httpUrl = deduplicationService.canonicalizeUrl("http://example.com/job/101");
        String httpsUrl = deduplicationService.canonicalizeUrl("https://example.com/job/101");

        assertNotEquals(httpUrl, httpsUrl);
        assertEquals("http://example.com/job/101", httpUrl);
        assertEquals("https://example.com/job/101", httpsUrl);
    }

    @Test
    void canonicalizeUrl_removesDefaultPort80ForHttp() {
        deduplicationService.setCanonicalizeScheme(false);
        String canonical = deduplicationService.canonicalizeUrl("http://example.com:80/job/101");
        assertEquals("http://example.com/job/101", canonical);
    }

    @Test
    void canonicalizeUrl_removesDefaultPort443ForHttps() {
        String canonical = deduplicationService.canonicalizeUrl("https://example.com:443/job/101");
        assertEquals("https://example.com/job/101", canonical);
    }

    @Test
    void canonicalizeUrl_preservesNonDefaultPort() {
        String canonical = deduplicationService.canonicalizeUrl("https://example.com:8443/job/101");
        assertEquals("https://example.com:8443/job/101", canonical);
    }

    @Test
    void canonicalizeUrl_lowercasesSchemeAndHost() {
        String canonical = deduplicationService.canonicalizeUrl("HTTPS://EXAMPLE.COM/Job/101");
        assertEquals("https://example.com/Job/101", canonical);
    }

    @Test
    void canonicalizeUrl_nullReturnsNull() {
        assertNull(deduplicationService.canonicalizeUrl(null));
    }

    @Test
    void canonicalizeUrl_blankReturnsNull() {
        assertNull(deduplicationService.canonicalizeUrl("   "));
    }

    @Test
    void canonicalizeUrl_removesTrailingSlash() {
        String canonical = deduplicationService.canonicalizeUrl("https://example.com/job/101/");
        assertEquals("https://example.com/job/101", canonical);
    }

    @Test
    void canonicalizeUrl_preservesRootSlash() {
        String canonical = deduplicationService.canonicalizeUrl("https://example.com/");
        assertEquals("https://example.com/", canonical);
    }

    @Test
    void canonicalizeUrl_stripsAllKnownTrackingParams() {
        String inputUrl = "https://example.com/job?utm_source=x&utm_medium=y&utm_campaign=z&utm_term=a&utm_content=b&ref=c&fbclid=d&id=123";
        String canonical = deduplicationService.canonicalizeUrl(inputUrl);
        assertEquals("https://example.com/job?id=123", canonical);
    }

    // ──────────────────────────────────────────────
    // Fingerprint tests
    // ──────────────────────────────────────────────

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
    void generateFingerprint_differentDescription_producesDifferentHash() {
        String fp1 = deduplicationService.generateFingerprint("Acme Corp", "Java Engineer", "Bangalore", "Senior role");
        String fp2 = deduplicationService.generateFingerprint("Acme Corp", "Java Engineer", "Bangalore", "Junior role");

        assertNotEquals(fp1, fp2);
    }

    @Test
    void generateFingerprint_missingDescription_returnsFallbackHashWithoutCrashing() {
        String fpWithDescNull = deduplicationService.generateFingerprint("Acme Corp", "Java Engineer", "Bangalore", null);
        String fpWithDescBlank = deduplicationService.generateFingerprint("Acme Corp", "Java Engineer", "Bangalore", "   ");

        assertNotNull(fpWithDescNull);
        assertEquals(fpWithDescNull, fpWithDescBlank);
    }

    @Test
    void generateFingerprint_missingCompany_returnsNull() {
        assertNull(deduplicationService.generateFingerprint(null, "Java Engineer", "Bangalore", "desc"));
        assertNull(deduplicationService.generateFingerprint("", "Java Engineer", "Bangalore", "desc"));
        assertNull(deduplicationService.generateFingerprint("   ", "Java Engineer", "Bangalore", "desc"));
    }

    @Test
    void generateFingerprint_missingTitle_returnsNull() {
        assertNull(deduplicationService.generateFingerprint("Acme", null, "Bangalore", "desc"));
        assertNull(deduplicationService.generateFingerprint("Acme", "", "Bangalore", "desc"));
        assertNull(deduplicationService.generateFingerprint("Acme", "   ", "Bangalore", "desc"));
    }

    @Test
    void generateFingerprint_missingLocation_returnsNull() {
        assertNull(deduplicationService.generateFingerprint("Acme", "Java Engineer", null, "desc"));
        assertNull(deduplicationService.generateFingerprint("Acme", "Java Engineer", "", "desc"));
        assertNull(deduplicationService.generateFingerprint("Acme", "Java Engineer", "   ", "desc"));
    }

    @Test
    void generateFingerprint_is64CharHex() {
        String fp = deduplicationService.generateFingerprint("Acme", "Dev", "BLR", "desc");
        assertNotNull(fp);
        assertEquals(64, fp.length());
        assertTrue(fp.matches("[0-9a-f]+"));
    }

    // ──────────────────────────────────────────────
    // Deduplication precedence tests
    // ──────────────────────────────────────────────

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
        // Level 2 and 3 should NOT be called when Level 1 matches
        verify(jobRepository, never()).findByCanonicalUrl(anyString());
        verify(jobRepository, never()).findByDeduplicationFingerprint(anyString());
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
        // Level 3 should NOT be called when Level 2 matches
        verify(jobRepository, never()).findByDeduplicationFingerprint(anyString());
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

    @Test
    void findExistingJob_noMatchReturnsEmpty() {
        when(jobRepository.findBySourceAndExternalId(anyString(), anyString())).thenReturn(Optional.empty());
        when(jobRepository.findByCanonicalUrl(anyString())).thenReturn(Optional.empty());
        when(jobRepository.findByDeduplicationFingerprint(anyString())).thenReturn(Optional.empty());

        Optional<Job> match = deduplicationService.findExistingJob(
                "COMPANY_WEBSITE", "EXT-100", "https://example.com/job/100", "Acme", "Dev", "Bangalore", "Desc"
        );

        assertTrue(match.isEmpty());
    }

    @Test
    void findExistingJob_nullExternalId_skipsLevel1() {
        // When externalId is null, Level 1 should be skipped entirely
        Job existingJob = Job.builder().id(5L).canonicalUrl("https://example.com/job/100").build();
        when(jobRepository.findByCanonicalUrl("https://example.com/job/100")).thenReturn(Optional.of(existingJob));

        Optional<Job> match = deduplicationService.findExistingJob(
                "COMPANY_WEBSITE", null, "https://example.com/job/100", "Acme", "Dev", "Bangalore", "Desc"
        );

        assertTrue(match.isPresent());
        assertEquals(5L, match.get().getId());
        verify(jobRepository, never()).findBySourceAndExternalId(anyString(), anyString());
    }

    @Test
    void findExistingJob_nullUrl_skipsLevel2() {
        when(jobRepository.findBySourceAndExternalId(anyString(), anyString())).thenReturn(Optional.empty());

        Job existingJob = Job.builder().id(6L).build();
        when(jobRepository.findByDeduplicationFingerprint(anyString())).thenReturn(Optional.of(existingJob));

        Optional<Job> match = deduplicationService.findExistingJob(
                "COMPANY_WEBSITE", "EXT-100", null, "Acme", "Dev", "Bangalore", "Desc"
        );

        assertTrue(match.isPresent());
        assertEquals(6L, match.get().getId());
        verify(jobRepository, never()).findByCanonicalUrl(anyString());
    }

    @Test
    void findExistingJob_sameSourceExternalId_differentUrl_usesLevel1() {
        // Identity conflict: same source+externalId but different URL
        // Level 1 is authoritative — should match on Level 1 regardless of URL
        Job existingJob = Job.builder().id(7L).source("COMPANY_WEBSITE").externalId("EXT-200")
                .canonicalUrl("https://old-domain.com/job/200").build();
        when(jobRepository.findBySourceAndExternalId("COMPANY_WEBSITE", "EXT-200")).thenReturn(Optional.of(existingJob));

        Optional<Job> match = deduplicationService.findExistingJob(
                "COMPANY_WEBSITE", "EXT-200", "https://new-domain.com/job/200", "Acme", "Dev", "Bangalore", "Desc"
        );

        assertTrue(match.isPresent());
        assertEquals(7L, match.get().getId());
    }

}
