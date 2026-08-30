package com.dotfield.discovery.source;

import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.RawJobListing;
import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.RemoteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompanyCareerPageSourceTest {

    private CompanyCareerPageSource source;

    @BeforeEach
    void setUp() {
        source = new CompanyCareerPageSource();
    }

    @Test
    void supports_validSource_returnsTrue() {
        assertTrue(source.supports("COMPANY_WEBSITE"));
        assertTrue(source.supports("company_website"));
        assertFalse(source.supports("LINKEDIN"));
    }

    @Test
    void discover_withoutFilter_returnsListings() {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("COMPANY_WEBSITE")
                .maxResults(10)
                .build();

        List<RawJobListing> listings = source.discover(request);

        assertNotNull(listings);
        assertFalse(listings.isEmpty());
        assertTrue(listings.size() <= 10);
        assertEquals("COMPANY_WEBSITE", listings.get(0).getSource());
    }

    @Test
    void discover_withKeywordFilter_filtersMatchingListings() {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("COMPANY_WEBSITE")
                .keyword("Java")
                .maxResults(10)
                .build();

        List<RawJobListing> listings = source.discover(request);

        assertNotNull(listings);
        for (RawJobListing job : listings) {
            boolean titleHasJava = job.getTitle() != null && job.getTitle().toLowerCase().contains("java");
            boolean descHasJava = job.getDescription() != null && job.getDescription().toLowerCase().contains("java");
            assertTrue(titleHasJava || descHasJava);
        }
    }

    @Test
    void discover_boundedMaxResults_respectsLimit() {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("COMPANY_WEBSITE")
                .maxResults(1)
                .build();

        List<RawJobListing> listings = source.discover(request);

        assertEquals(1, listings.size());
    }

    @Test
    void discover_withLocationFilter_filtersCorrectly() {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("COMPANY_WEBSITE")
                .location("Hyderabad")
                .maxResults(10)
                .build();

        List<RawJobListing> listings = source.discover(request);

        assertNotNull(listings);
        for (RawJobListing job : listings) {
            assertTrue(job.getLocation().toLowerCase().contains("hyderabad"));
        }
    }

    @Test
    void discover_withCompanyFilter_filtersCorrectly() {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("COMPANY_WEBSITE")
                .company("Acme")
                .maxResults(10)
                .build();

        List<RawJobListing> listings = source.discover(request);

        assertFalse(listings.isEmpty());
        for (RawJobListing job : listings) {
            assertTrue(job.getCompany().toLowerCase().contains("acme"));
        }
    }

    @Test
    void discover_nonMatchingCompanyFilter_returnsEmpty() {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("COMPANY_WEBSITE")
                .company("NonExistent Corp")
                .maxResults(10)
                .build();

        List<RawJobListing> listings = source.discover(request);
        assertTrue(listings.isEmpty());
    }

    @Test
    void discover_listingsHaveOwnEmploymentType_notCopiedFromRequest() {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("COMPANY_WEBSITE")
                .remoteType(RemoteType.REMOTE)
                .employmentType(EmploymentType.CONTRACT)
                .maxResults(10)
                .build();

        List<RawJobListing> listings = source.discover(request);

        // Listings should have their own fixed types, NOT the request's types
        for (RawJobListing job : listings) {
            assertNotNull(job.getEmploymentType(), "Each listing should have its own employment type");
            assertNotNull(job.getRemoteType(), "Each listing should have its own remote type");
            // The simulated listings are FULL_TIME, not CONTRACT
            assertEquals(EmploymentType.FULL_TIME, job.getEmploymentType());
        }
    }

    @Test
    void discover_rawDataIsRealistic_notPlaceholder() {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("COMPANY_WEBSITE")
                .maxResults(10)
                .build();

        List<RawJobListing> listings = source.discover(request);

        for (RawJobListing job : listings) {
            assertNotNull(job.getRawData());
            // Should contain realistic fields, not "rawKey"
            assertTrue(job.getRawData().containsKey("listing_id"));
            assertTrue(job.getRawData().containsKey("position_title"));
            assertTrue(job.getRawData().containsKey("source_adapter"));
            assertFalse(job.getRawData().containsKey("rawKey"),
                    "rawData should not contain placeholder keys");
        }
    }

    @Test
    void discover_nullRequest_returnsEmptyList() {
        List<RawJobListing> listings = source.discover(null);
        assertTrue(listings.isEmpty());
    }

}
