package com.dotfield.discovery.source;

import com.dotfield.discovery.JobSource;
import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.RawJob;
import com.dotfield.dto.RawJobListing;
import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.RemoteType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Adapter for fetching public remote job listings from the official Remotive API.
 * Endpoint: {@code https://remotive.com/api/remote-jobs}
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "job.sources.remotive.enabled", havingValue = "true", matchIfMissing = false)
public class RemotiveJobSource implements JobSource {

    public static final String SOURCE_NAME = "REMOTIVE";

    private final String baseUrl;
    private final RestClient restClient;

    @org.springframework.beans.factory.annotation.Autowired
    public RemotiveJobSource(
            @Value("${job.sources.remotive.base-url:https://remotive.com/api/remote-jobs}") String baseUrl,
            @Value("${job.sources.remotive.connect-timeout:5000}") int connectTimeout,
            @Value("${job.sources.remotive.read-timeout:10000}") int readTimeout) {
        this.baseUrl = baseUrl;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Constructor for testing with custom RestClient or MockRestServiceServer.
     */
    public RemotiveJobSource(String baseUrl, RestClient restClient) {
        this.baseUrl = baseUrl;
        this.restClient = restClient;
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
        String url = buildFetchUrl(request);
        log.info("Fetching external jobs from Remotive API: {}", url);

        RemotiveApiResponse response;
        try {
            response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(RemotiveApiResponse.class);
        } catch (Exception e) {
            log.error("Failed to fetch jobs from Remotive API ({}) : {}", url, e.getMessage());
            throw new RuntimeException("Remotive API request failed: " + e.getMessage(), e);
        }

        if (response == null || response.getJobs() == null || response.getJobs().isEmpty()) {
            log.info("Remotive API returned zero jobs");
            return List.of();
        }

        List<RawJobListing> rawJobs = new ArrayList<>();
        int max = (request != null && request.getMaxResults() != null) ? request.getMaxResults() : 50;

        for (RemotiveJobDto jobDto : response.getJobs()) {
            if (rawJobs.size() >= max) {
                break;
            }
            try {
                RawJobListing rawJob = mapToRawJob(jobDto);
                if (rawJob != null) {
                    rawJobs.add(rawJob);
                }
            } catch (Exception e) {
                log.warn("Skipping malformed Remotive job record (id={}): {}", jobDto != null ? jobDto.getId() : "null", e.getMessage());
            }
        }

        log.info("Successfully fetched and mapped {} raw jobs from Remotive API", rawJobs.size());
        return rawJobs;
    }

    private String buildFetchUrl(JobDiscoveryRequest request) {
        if (request == null) {
            return baseUrl;
        }
        StringBuilder sb = new StringBuilder(baseUrl);
        List<String> queryParams = new ArrayList<>();

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            queryParams.add("search=" + encode(request.getKeyword().trim()));
        }
        if (request.getMaxResults() != null) {
            queryParams.add("limit=" + request.getMaxResults());
        }

        if (!queryParams.isEmpty()) {
            sb.append(baseUrl.contains("?") ? "&" : "?").append(String.join("&", queryParams));
        }
        return sb.toString();
    }

    private String encode(String val) {
        try {
            return java.net.URLEncoder.encode(val, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return val;
        }
    }

    private RawJobListing mapToRawJob(RemotiveJobDto dto) {
        if (dto == null) {
            return null;
        }

        String externalId = dto.getId() != null ? String.valueOf(dto.getId()).trim() : null;
        String title = dto.getTitle() != null ? dto.getTitle().trim() : null;
        String jobUrl = dto.getUrl() != null ? dto.getUrl().trim() : null;

        if ((externalId == null || externalId.isBlank()) && (title == null || title.isBlank())) {
            log.warn("Skipping Remotive job record missing both externalId and title");
            return null;
        }

        String company = (dto.getCompanyName() != null && !dto.getCompanyName().isBlank())
                ? dto.getCompanyName().trim() : "Unknown";
        String location = (dto.getCandidateRequiredLocation() != null && !dto.getCandidateRequiredLocation().isBlank())
                ? dto.getCandidateRequiredLocation().trim() : "Remote";
        String description = dto.getDescription() != null ? dto.getDescription().trim() : "";

        LocalDate postedDate = parseDate(dto.getPublicationDate());
        EmploymentType employmentType = parseEmploymentType(dto.getJobType());

        Map<String, Object> rawData = new LinkedHashMap<>();
        if (dto.getId() != null) rawData.put("id", dto.getId());
        if (dto.getCategory() != null) rawData.put("category", dto.getCategory());
        if (dto.getJobType() != null) rawData.put("job_type", dto.getJobType());
        if (dto.getSalary() != null) rawData.put("salary", dto.getSalary());
        if (dto.getTags() != null) rawData.put("tags", dto.getTags());

        return RawJob.builder()
                .externalId(externalId)
                .title(title)
                .company(company)
                .location(location)
                .description(description)
                .jobUrl(jobUrl)
                .source(SOURCE_NAME)
                .employmentType(employmentType)
                .remoteType(RemoteType.REMOTE)
                .postedDate(postedDate)
                .rawData(rawData)
                .build();
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME).toLocalDate();
        } catch (Exception e) {
            try {
                return LocalDate.parse(dateStr.substring(0, 10));
            } catch (Exception ex) {
                log.debug("Could not parse publication date '{}': {}", dateStr, ex.getMessage());
                return null;
            }
        }
    }

    private EmploymentType parseEmploymentType(String typeStr) {
        if (typeStr == null || typeStr.isBlank()) {
            return EmploymentType.FULL_TIME;
        }
        String normalized = typeStr.toLowerCase().trim();
        if (normalized.contains("contract") || normalized.contains("freelance")) {
            return EmploymentType.CONTRACT;
        }
        if (normalized.contains("part")) {
            return EmploymentType.PART_TIME;
        }
        if (normalized.contains("intern")) {
            return EmploymentType.INTERNSHIP;
        }
        return EmploymentType.FULL_TIME;
    }

    // --- DTOs representing Remotive API JSON response structure ---

    @Data
    public static class RemotiveApiResponse {
        @com.fasterxml.jackson.annotation.JsonProperty("0-legal-notice")
        private String legalNotice;

        @com.fasterxml.jackson.annotation.JsonProperty("job-count")
        private Integer jobCount;

        private List<RemotiveJobDto> jobs;
    }

    @Data
    public static class RemotiveJobDto {
        private Object id;
        private String url;
        private String title;

        @com.fasterxml.jackson.annotation.JsonProperty("company_name")
        private String companyName;

        private String category;
        private List<String> tags;

        @com.fasterxml.jackson.annotation.JsonProperty("job_type")
        private String jobType;

        @com.fasterxml.jackson.annotation.JsonProperty("publication_date")
        private String publicationDate;

        @com.fasterxml.jackson.annotation.JsonProperty("candidate_required_location")
        private String candidateRequiredLocation;

        private String salary;
        private String description;
    }
}
