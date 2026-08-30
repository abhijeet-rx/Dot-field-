package com.dotfield.discovery.source;

import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.RawJobListing;
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

}
