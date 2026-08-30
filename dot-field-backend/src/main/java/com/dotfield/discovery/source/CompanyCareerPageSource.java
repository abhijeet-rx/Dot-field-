package com.dotfield.discovery.source;

import com.dotfield.discovery.JobSource;
import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.RawJobListing;
import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.RemoteType;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * V1 simulated source adapter for job discovery.
 * <p>
 * This adapter returns a controlled/simulated set of job listings for
 * development and testing purposes. It does NOT scrape arbitrary company
 * career pages, public feeds, or any external websites.
 * <p>
 * Supported filters (applied against the simulated dataset):
 * <ul>
 *   <li>{@code keyword} — matches against title and description (case-insensitive substring)</li>
 *   <li>{@code location} — matches against listing location (case-insensitive substring)</li>
 *   <li>{@code company} — matches against listing company name (case-insensitive substring)</li>
 *   <li>{@code maxResults} — bounds the number of returned listings</li>
 * </ul>
 * <p>
 * Unsupported filters (ignored, NOT applied to listings):
 * <ul>
 *   <li>{@code employmentType} — the simulated listings have their own fixed employment types</li>
 *   <li>{@code remoteType} — the simulated listings have their own fixed remote types</li>
 * </ul>
 */
@Component
public class CompanyCareerPageSource implements JobSource {

    public static final String SOURCE_NAME = "COMPANY_WEBSITE";

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
            return List.of();
        }

        int max = request.getMaxResults() != null ? request.getMaxResults() : 20;

        List<RawJobListing> allListings = createSimulatedListings();
        List<RawJobListing> filtered = applyFilters(allListings, request);
        return filtered.subList(0, Math.min(max, filtered.size()));
    }

    /**
     * Returns the fixed simulated dataset. Each listing has its own
     * employment type and remote type — these are NOT copied from the request.
     */
    private List<RawJobListing> createSimulatedListings() {
        List<RawJobListing> listings = new ArrayList<>();

        listings.add(RawJobListing.builder()
                .externalId("JOB-CW-101")
                .title("Java Backend Developer")
                .company("Acme Corp")
                .location("Bangalore")
                .description("Looking for experienced Java and Spring Boot backend developers.")
                .jobUrl("https://acme.com/careers/jobs/101")
                .source(SOURCE_NAME)
                .employmentType(EmploymentType.FULL_TIME)
                .remoteType(RemoteType.HYBRID)
                .rawData(buildRawData("JOB-CW-101", "Java Backend Developer", "Acme Corp",
                        "Bangalore", "FULL_TIME", "HYBRID"))
                .build());

        listings.add(RawJobListing.builder()
                .externalId("JOB-CW-102")
                .title("Senior Software Engineer")
                .company("Acme Corp")
                .location("Hyderabad")
                .description("Lead developer position working with Java microservices and Kubernetes.")
                .jobUrl("https://acme.com/careers/jobs/102")
                .source(SOURCE_NAME)
                .employmentType(EmploymentType.FULL_TIME)
                .remoteType(RemoteType.ONSITE)
                .rawData(buildRawData("JOB-CW-102", "Senior Software Engineer", "Acme Corp",
                        "Hyderabad", "FULL_TIME", "ONSITE"))
                .build());

        return listings;
    }

    /**
     * Applies request filters against the simulated dataset.
     * Only keyword, location, and company filters are supported.
     * employmentType and remoteType filters are NOT supported by this adapter.
     */
    private List<RawJobListing> applyFilters(List<RawJobListing> listings, JobDiscoveryRequest request) {
        String keyword = request.getKeyword() != null ? request.getKeyword().toLowerCase() : "";
        String locationFilter = request.getLocation() != null ? request.getLocation().toLowerCase() : "";
        String companyFilter = request.getCompany() != null ? request.getCompany().toLowerCase() : "";

        List<RawJobListing> result = new ArrayList<>();
        for (RawJobListing job : listings) {
            if (matchesFilter(job, keyword, locationFilter, companyFilter)) {
                result.add(job);
            }
        }
        return result;
    }

    private boolean matchesFilter(RawJobListing job, String keyword, String locationFilter, String companyFilter) {
        if (!keyword.isBlank()) {
            boolean titleMatch = job.getTitle() != null && job.getTitle().toLowerCase().contains(keyword);
            boolean descMatch = job.getDescription() != null && job.getDescription().toLowerCase().contains(keyword);
            if (!titleMatch && !descMatch) {
                return false;
            }
        }
        if (!locationFilter.isBlank()) {
            boolean locMatch = job.getLocation() != null && job.getLocation().toLowerCase().contains(locationFilter);
            if (!locMatch) {
                return false;
            }
        }
        if (!companyFilter.isBlank()) {
            boolean companyMatch = job.getCompany() != null && job.getCompany().toLowerCase().contains(companyFilter);
            if (!companyMatch) {
                return false;
            }
        }
        return true;
    }

    /**
     * Builds a realistic raw data map representing what a real career page
     * JSON response might contain. This replaces the previous placeholder
     * data ({@code "rawKey" -> "rawValue1"}).
     */
    private Map<String, Object> buildRawData(String id, String title, String company,
                                              String location, String employmentType, String remoteType) {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("listing_id", id);
        raw.put("position_title", title);
        raw.put("company_name", company);
        raw.put("office_location", location);
        raw.put("employment_type", employmentType);
        raw.put("work_arrangement", remoteType);
        raw.put("source_adapter", "CompanyCareerPageSource_v1_simulated");
        return raw;
    }

}
