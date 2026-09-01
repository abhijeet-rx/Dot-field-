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
 * Partner integration adapter for Foundit (formerly Monster India).
 * Requires authorized Foundit Partner API credentials.
 * <p>
 * Status: {@code DISABLED — PARTNER ACCESS REQUIRED}.
 * Configured via {@code job.sources.foundit.enabled} (default {@code false}).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "job.sources.foundit.enabled", havingValue = "true", matchIfMissing = false)
public class FounditJobSource implements JobSource {

    public static final String SOURCE_NAME = "FOUNDIT";

    private final String clientId;
    private final String clientSecret;

    public FounditJobSource(
            @Value("${job.sources.foundit.client-id:}") String clientId,
            @Value("${job.sources.foundit.client-secret:}") String clientSecret) {
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
            log.warn("Foundit job discovery invoked but partner API credentials (FOUNDIT_CLIENT_ID / FOUNDIT_CLIENT_SECRET) are missing. Skipping ingestion.");
            return List.of();
        }

        log.info("Executing Foundit Partner API job discovery for query: {}", request != null ? request.getKeyword() : "all");
        return List.of();
    }
}
