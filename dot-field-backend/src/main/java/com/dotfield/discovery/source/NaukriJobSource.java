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
 * Enterprise partner integration adapter for Naukri.com.
 * Requires authorized Naukri Enterprise Recruiter / Partner API credentials.
 * <p>
 * Status: {@code DISABLED — PARTNER ACCESS REQUIRED}.
 * Configured via {@code job.sources.naukri.enabled} (default {@code false}).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "job.sources.naukri.enabled", havingValue = "true", matchIfMissing = false)
public class NaukriJobSource implements JobSource {

    public static final String SOURCE_NAME = "NAUKRI";

    private final String clientId;
    private final String clientSecret;

    public NaukriJobSource(
            @Value("${job.sources.naukri.client-id:}") String clientId,
            @Value("${job.sources.naukri.client-secret:}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
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
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            log.warn("Naukri job discovery invoked but partner API credentials (NAUKRI_CLIENT_ID / NAUKRI_CLIENT_SECRET) are missing. Skipping ingestion.");
            return List.of();
        }

        log.info("Executing Naukri Enterprise API job discovery for query: {}", request != null ? request.getKeyword() : "all");
        // Real partner API execution path would invoke authenticated OAuth 2.0 endpoint here.
        return List.of();
    }
}
