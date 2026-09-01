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
    void discover_withoutConfiguredAtsFeeds_returnsEmptyListWithoutFakeData() {
        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("COMPANY_WEBSITE")
                .maxResults(10)
                .build();

        List<RawJobListing> listings = source.discover(request);

        assertNotNull(listings);
        assertTrue(listings.isEmpty(), "Should return empty list rather than hardcoded fake jobs");
    }

    @Test
    void discover_nullRequest_returnsEmptyList() {
        List<RawJobListing> listings = source.discover(null);
        assertNotNull(listings);
        assertTrue(listings.isEmpty());
    }
}
