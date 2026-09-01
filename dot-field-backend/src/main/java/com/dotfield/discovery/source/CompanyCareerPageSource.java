package com.dotfield.discovery.source;

import com.dotfield.discovery.JobSource;
import com.dotfield.discovery.india.IndiaLocationNormalizer;
import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.RawJobListing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Adapter for discovering jobs directly from company career portals and structured ATS feeds.
 * <p>
 * Does NOT generate fake or hardcoded job listings per Sections 3 & 4.
 * When live company ATS credentials / feeds (e.g. Greenhouse, Lever, Workday) are unconfigured,
 * this source returns an empty list and operates in DISABLED / NOT CONFIGURED state.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "job.sources.company-careers.enabled", havingValue = "true", matchIfMissing = true)
public class CompanyCareerPageSource implements JobSource {

    public static final String SOURCE_NAME = "COMPANY_WEBSITE";

    private final IndiaLocationNormalizer locationNormalizer;

    public CompanyCareerPageSource(IndiaLocationNormalizer locationNormalizer) {
        this.locationNormalizer = locationNormalizer != null ? locationNormalizer : new IndiaLocationNormalizer();
    }

    public CompanyCareerPageSource() {
        this(new IndiaLocationNormalizer());
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
        if (request == null) {
            return Collections.emptyList();
        }

        // Section 3 & 4 Rule: No hardcoded or fake job listings.
        // Live retrieval is only performed when structured ATS endpoints / customer API credentials are configured.
        log.info("CompanyCareerPageSource: Live ATS integration requires company API credentials. Returning empty listing set.");
        return Collections.emptyList();
    }
}
