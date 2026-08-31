package com.dotfield.discovery;

import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.RawJobListing;

import java.util.List;

/**
 * Abstraction for fetching raw jobs from external job sources.
 */
public interface JobSource {

    /**
     * Unique identifier for this job source (e.g., "COMPANY_WEBSITE", "LINKEDIN").
     */
    String getSourceName();

    /**
     * Checks if this adapter supports the specified source name.
     */
    boolean supports(String source);

    /**
     * Primary method for fetching/discovering raw job listings from the source.
     */
    List<RawJobListing> discover(JobDiscoveryRequest request);

    /**
     * Convenience/alias method for fetching raw job listings from the source.
     */
    default List<RawJobListing> fetchJobs(JobDiscoveryRequest request) {
        return discover(request);
    }
}
