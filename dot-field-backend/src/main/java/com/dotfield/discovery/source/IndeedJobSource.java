package com.dotfield.discovery.source;

import com.dotfield.discovery.JobSource;
import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.RawJobListing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Publisher API integration adapter for Indeed India.
 * Requires authorized Indeed Publisher / Employer API credentials.
 * <p>
 * Status: {@code DISABLED — PARTNER ACCESS REQUIRED}.
 * Configured via {@code job.sources.indeed.enabled} (default {@code false}).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "job.sources.indeed.enabled", havingValue = "true", matchIfMissing = false)
public class IndeedJobSource implements JobSource {

    public static final String SOURCE_NAME = "INDEED";

    private final String publisherId;

    public IndeedJobSource(@Value("${job.sources.indeed.publisher-id:}") String publisherId) {
        this.publisherId = publisherId;
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    @Override
    public boolean supports(String source) {
        return source != null && SOURCE_NAME.equalsIgnoreCase(source.trim());
    }

    @Override
    public List<RawJobListing> discover(JobDiscoveryRequest request) {
        if (publisherId == null || publisherId.isBlank()) {
            log.warn("Indeed job discovery invoked but Publisher ID (INDEED_PUBLISHER_ID) is missing. Skipping ingestion.");
            return List.of();
        }

        log.info("Executing Indeed India Publisher API job discovery for query: {}", request != null ? request.getKeyword() : "all");
        return List.of();
    }
}
