package com.dotfield.discovery.source;

import com.dotfield.discovery.JobSource;
import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.RawJobListing;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        List<RawJobListing> sampleListings = createConfiguredListings(request);
        return sampleListings.subList(0, Math.min(max, sampleListings.size()));
    }

    private List<RawJobListing> createConfiguredListings(JobDiscoveryRequest request) {
        List<RawJobListing> listings = new ArrayList<>();

        String keyword = request.getKeyword() != null ? request.getKeyword().toLowerCase() : "";
        String locationFilter = request.getLocation() != null ? request.getLocation().toLowerCase() : "";

        RawJobListing job1 = RawJobListing.builder()
                .externalId("JOB-CW-101")
                .title("Java Backend Developer")
                .company("Acme Corp")
                .location("Bangalore")
                .description("Looking for experienced Java and Spring Boot backend developers.")
                .jobUrl("https://acme.com/careers/jobs/101")
                .source(SOURCE_NAME)
                .employmentType(request.getEmploymentType())
                .remoteType(request.getRemoteType())
                .rawData(Map.of("rawKey", "rawValue1"))
                .build();

        RawJobListing job2 = RawJobListing.builder()
                .externalId("JOB-CW-102")
                .title("Senior Software Engineer")
                .company("Acme Corp")
                .location("Hyderabad")
                .description("Lead developer position working with Java microservices and Kubernetes.")
                .jobUrl("https://acme.com/careers/jobs/102")
                .source(SOURCE_NAME)
                .employmentType(request.getEmploymentType())
                .remoteType(request.getRemoteType())
                .rawData(Map.of("rawKey", "rawValue2"))
                .build();

        if (matchesFilter(job1, keyword, locationFilter)) {
            listings.add(job1);
        }
        if (matchesFilter(job2, keyword, locationFilter)) {
            listings.add(job2);
        }

        return listings;
    }

    private boolean matchesFilter(RawJobListing job, String keyword, String locationFilter) {
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
        return true;
    }

}
