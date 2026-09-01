package com.dotfield.discovery.source;

import com.dotfield.discovery.JobSource;
import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.RawJob;
import com.dotfield.dto.RawJobListing;
import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.RemoteType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Adapter for discovering jobs from Jooble REST API.
 * Endpoint: {@code https://jooble.org/api/{api_key}}
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "job.sources.jooble.enabled", havingValue = "true", matchIfMissing = true)
public class JoobleJobSource implements JobSource {

    public static final String SOURCE_NAME = "JOOBLE";

    private final String baseUrl;
    private final String apiKey;
    private final RestClient restClient;

    @org.springframework.beans.factory.annotation.Autowired
    public JoobleJobSource(
            @Value("${job.sources.jooble.base-url:https://jooble.org/api/}") String baseUrl,
            @Value("${job.sources.jooble.api-key:}") String apiKey,
            @Value("${job.sources.jooble.connect-timeout:5000}") int connectTimeout,
            @Value("${job.sources.jooble.read-timeout:10000}") int readTimeout) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public JoobleJobSource(String baseUrl, String apiKey, RestClient restClient) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
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
        if (apiKey == null || apiKey.isBlank()) {
            log.info("JoobleJobSource: DISABLED — CREDENTIALS NOT CONFIGURED (JOOBLE_API_KEY is empty)");
            return List.of();
        }

        int max = (request != null && request.getMaxResults() != null) ? request.getMaxResults() : 50;
        List<RawJobListing> rawJobs = new ArrayList<>();
        int page = 1;

        log.info("Fetching job opportunities from Jooble API...");

        while (rawJobs.size() < max) {
            String targetUrl = buildEndpointUrl();
            JoobleApiRequest reqBody = buildRequestBody(request, page);

            try {
                JoobleApiResponse response = restClient.post()
                        .uri(targetUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(reqBody)
                        .retrieve()
                        .body(JoobleApiResponse.class);

                if (response == null || response.getJobs() == null || response.getJobs().isEmpty()) {
                    break;
                }

                int fetchedThisPage = 0;
                for (JoobleJobDto dto : response.getJobs()) {
                    if (rawJobs.size() >= max) {
                        break;
                    }
                    RawJobListing listing = mapToRawJob(dto);
                    if (listing != null) {
                        rawJobs.add(listing);
                        fetchedThisPage++;
                    }
                }

                if (fetchedThisPage == 0 || response.getJobs().size() < 20) {
                    break;
                }
                page++;
            } catch (HttpClientErrorException.TooManyRequests e) {
                log.warn("Jooble API rate limited (429). Stopping pagination safely.");
                break;
            } catch (Exception e) {
                log.error("Failed to fetch jobs from Jooble API: {}", e.getClass().getSimpleName() + " - " + e.getMessage());
                break;
            }
        }

        log.info("Successfully fetched and mapped {} jobs from Jooble API", rawJobs.size());
        return rawJobs;
    }

    private String buildEndpointUrl() {
        String base = baseUrl.trim();
        if (!base.endsWith("/")) {
            base += "/";
        }
        return base + apiKey.trim();
    }

    private JoobleApiRequest buildRequestBody(JobDiscoveryRequest request, int page) {
        String keywords = (request != null && request.getKeyword() != null && !request.getKeyword().isBlank())
                ? request.getKeyword().trim() : "software engineer";

        String location = (request != null && request.getLocation() != null && !request.getLocation().isBlank())
                ? request.getLocation().trim() : "India";

        return JoobleApiRequest.builder()
                .keywords(keywords)
                .location(location)
                .page(page)
                .build();
    }

    private RawJobListing mapToRawJob(JoobleJobDto dto) {
        if (dto == null) {
            return null;
        }

        String externalId = dto.getId() != null ? String.valueOf(dto.getId()).trim() : null;
        String title = dto.getTitle() != null ? dto.getTitle().trim() : null;
        String company = (dto.getCompany() != null && !dto.getCompany().isBlank()) ? dto.getCompany().trim() : "Unknown";
        
        // Missing location must remain null, NOT defaulted to "India"
        String location = (dto.getLocation() != null && !dto.getLocation().isBlank()) ? dto.getLocation().trim() : null;
        
        String jobUrl = dto.getLink() != null ? dto.getLink().trim() : null;
        String description = dto.getSnippet() != null ? dto.getSnippet().trim() : "";

        if ((externalId == null || externalId.isBlank()) && jobUrl != null) {
            externalId = "JBL-" + Math.abs(jobUrl.hashCode());
        }

        if (title == null || title.isBlank()) {
            return null;
        }

        LocalDate postedDate = parseDate(dto.getUpdated());
        EmploymentType employmentType = parseEmploymentType(dto.getType());
        RemoteType remoteType = parseRemoteType(location, title, description);
        String currency = parseCurrency(dto.getSalary());

        Map<String, Object> rawData = new LinkedHashMap<>();
        if (externalId != null) rawData.put("id", externalId);
        if (title != null) rawData.put("title", title);
        if (company != null) rawData.put("company", company);
        if (location != null) rawData.put("location", location);
        if (dto.getSalary() != null) rawData.put("salary", dto.getSalary());

        return RawJob.builder()
                .externalId(externalId)
                .title(title)
                .company(company)
                .location(location)
                .description(description)
                .jobUrl(jobUrl)
                .source(SOURCE_NAME)
                .employmentType(employmentType)
                .remoteType(remoteType)
                .currency(currency)
                .postedDate(postedDate)
                .rawData(rawData)
                .build();
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME).toLocalDate();
        } catch (Exception e) {
            try {
                return LocalDate.parse(dateStr.substring(0, 10));
            } catch (Exception ex) {
                return LocalDate.now();
            }
        }
    }

    private EmploymentType parseEmploymentType(String typeStr) {
        if (typeStr == null || typeStr.isBlank()) {
            return null; // Do NOT default missing type to FULL_TIME
        }
        String lower = typeStr.toLowerCase().trim();
        if (lower.contains("part")) return EmploymentType.PART_TIME;
        if (lower.contains("contract") || lower.contains("freelance")) return EmploymentType.CONTRACT;
        if (lower.contains("temp")) return EmploymentType.TEMPORARY;
        if (lower.contains("intern")) return EmploymentType.INTERNSHIP;
        if (lower.contains("full") || lower.contains("perm")) return EmploymentType.FULL_TIME;
        return null;
    }

    private RemoteType parseRemoteType(String location, String title, String description) {
        String combined = ((location != null ? location : "") + " " + (title != null ? title : "")).toLowerCase();
        if (combined.isBlank()) {
            return null;
        }
        if (combined.contains("remote")) return RemoteType.REMOTE;
        if (combined.contains("hybrid")) return RemoteType.HYBRID;
        if (combined.contains("onsite") || combined.contains("on-site") || combined.contains("office")) return RemoteType.ONSITE;
        return null; // Do NOT guess HYBRID
    }

    private String parseCurrency(String salaryStr) {
        if (salaryStr == null || salaryStr.isBlank()) {
            return null;
        }
        if (salaryStr.contains("₹") || salaryStr.toUpperCase().contains("INR")) {
            return "INR";
        }
        if (salaryStr.contains("$") || salaryStr.toUpperCase().contains("USD")) {
            return "USD";
        }
        if (salaryStr.contains("€") || salaryStr.toUpperCase().contains("EUR")) {
            return "EUR";
        }
        if (salaryStr.contains("£") || salaryStr.toUpperCase().contains("GBP")) {
            return "GBP";
        }
        return null;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoobleApiRequest {
        private String keywords;
        private String location;
        private Integer page;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JoobleApiResponse {
        private Integer totalCount;
        private List<JoobleJobDto> jobs;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JoobleJobDto {
        private Object id;
        private String title;
        private String location;
        private String snippet;
        private String company;
        private String updated;
        private String type;
        private String salary;
        private String link;
    }
}
