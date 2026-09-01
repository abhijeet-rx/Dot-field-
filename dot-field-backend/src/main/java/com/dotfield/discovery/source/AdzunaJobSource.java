package com.dotfield.discovery.source;

import com.dotfield.discovery.JobSource;
import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.RawJob;
import com.dotfield.dto.RawJobListing;
import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.RemoteType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Adapter for discovering jobs from Adzuna API (India Market).
 * Endpoint: {@code https://api.adzuna.com/v1/api/jobs/in/search/1}
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "job.sources.adzuna.enabled", havingValue = "true", matchIfMissing = true)
public class AdzunaJobSource implements JobSource {

    public static final String SOURCE_NAME = "ADZUNA";

    private final String baseUrl;
    private final String appId;
    private final String appKey;
    private final RestClient restClient;

    @org.springframework.beans.factory.annotation.Autowired
    public AdzunaJobSource(
            @Value("${job.sources.adzuna.base-url:https://api.adzuna.com/v1/api/jobs/in/search}") String baseUrl,
            @Value("${job.sources.adzuna.app-id:}") String appId,
            @Value("${job.sources.adzuna.app-key:}") String appKey,
            @Value("${job.sources.adzuna.connect-timeout:5000}") int connectTimeout,
            @Value("${job.sources.adzuna.read-timeout:10000}") int readTimeout) {
        this.baseUrl = baseUrl;
        this.appId = appId;
        this.appKey = appKey;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public AdzunaJobSource(String baseUrl, String appId, String appKey, RestClient restClient) {
        this.baseUrl = baseUrl;
        this.appId = appId;
        this.appKey = appKey;
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
        if (appId == null || appId.isBlank() || appKey == null || appKey.isBlank()) {
            log.info("AdzunaJobSource: DISABLED — CREDENTIALS NOT CONFIGURED (ADZUNA_APP_ID or ADZUNA_APP_KEY empty)");
            return List.of();
        }

        int max = (request != null && request.getMaxResults() != null) ? request.getMaxResults() : 50;
        List<RawJobListing> rawJobs = new ArrayList<>();
        int page = 1;

        log.info("Fetching job opportunities from Adzuna API (India market)...");

        while (rawJobs.size() < max) {
            String searchUrl = buildSearchUrl(request, page);
            try {
                AdzunaApiResponse response = restClient.get()
                        .uri(searchUrl)
                        .retrieve()
                        .body(AdzunaApiResponse.class);

                if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                    break;
                }

                int fetchedThisPage = 0;
                for (AdzunaJobDto dto : response.getResults()) {
                    if (rawJobs.size() >= max) {
                        break;
                    }
                    RawJobListing listing = mapToRawJob(dto);
                    if (listing != null) {
                        rawJobs.add(listing);
                        fetchedThisPage++;
                    }
                }

                if (fetchedThisPage == 0 || response.getResults().size() < 20) {
                    break; // No more results available
                }
                page++;
            } catch (HttpClientErrorException.TooManyRequests e) {
                log.warn("Adzuna API rate limited (429). Stopping pagination safely.");
                break;
            } catch (Exception e) {
                log.error("Failed to fetch jobs from Adzuna API: {}", e.getClass().getSimpleName() + " - " + e.getMessage());
                break;
            }
        }

        log.info("Successfully fetched and mapped {} jobs from Adzuna API", rawJobs.size());
        return rawJobs;
    }

    private String buildSearchUrl(JobDiscoveryRequest request, int page) {
        String base = baseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        
        // Remove trailing page index if present
        int lastSlash = base.lastIndexOf('/');
        if (lastSlash != -1 && base.substring(lastSlash + 1).matches("\\d+")) {
            base = base.substring(0, lastSlash);
        }

        base = base + "/" + page;

        StringBuilder sb = new StringBuilder(base);
        List<String> params = new ArrayList<>();

        params.add("app_id=" + encode(appId.trim()));
        params.add("app_key=" + encode(appKey.trim()));

        int resultsPerPage = (request != null && request.getMaxResults() != null) ? Math.min(request.getMaxResults(), 50) : 20;
        params.add("results_per_page=" + resultsPerPage);
        params.add("content-type=application/json");

        String keyword = (request != null && request.getKeyword() != null && !request.getKeyword().isBlank())
                ? request.getKeyword().trim() : "software engineer";
        params.add("what=" + encode(keyword));

        if (request != null && request.getLocation() != null && !request.getLocation().isBlank()) {
            params.add("where=" + encode(request.getLocation().trim()));
        }

        sb.append("?").append(String.join("&", params));
        return sb.toString();
    }

    private String encode(String val) {
        try {
            return URLEncoder.encode(val, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return val;
        }
    }

    private RawJobListing mapToRawJob(AdzunaJobDto dto) {
        if (dto == null) {
            return null;
        }

        String externalId = dto.getId() != null ? String.valueOf(dto.getId()).trim() : null;
        String title = dto.getTitle() != null ? dto.getTitle().trim() : null;
        String company = (dto.getCompany() != null && dto.getCompany().getDisplayName() != null && !dto.getCompany().getDisplayName().isBlank())
                ? dto.getCompany().getDisplayName().trim() : "Unknown";

        String location = (dto.getLocation() != null && dto.getLocation().getDisplayName() != null && !dto.getLocation().getDisplayName().isBlank())
                ? dto.getLocation().getDisplayName().trim() : null; // Do NOT default to "India"

        String jobUrl = dto.getRedirectUrl() != null ? dto.getRedirectUrl().trim() : null;
        String description = dto.getDescription() != null ? dto.getDescription().trim() : "";

        if ((externalId == null || externalId.isBlank()) && jobUrl != null) {
            externalId = "ADZ-" + Math.abs(jobUrl.hashCode());
        }

        if (title == null || title.isBlank()) {
            return null;
        }

        BigDecimal salaryMin = dto.getSalaryMin() != null ? BigDecimal.valueOf(dto.getSalaryMin()) : null;
        BigDecimal salaryMax = dto.getSalaryMax() != null ? BigDecimal.valueOf(dto.getSalaryMax()) : null;
        LocalDate postedDate = parseDate(dto.getCreated());

        String rawContractType = dto.getContractType();
        String rawContractTime = dto.getContractTime();
        EmploymentType employmentType = parseEmploymentType(rawContractType, rawContractTime);
        RemoteType remoteType = parseRemoteType(location, title, description);

        Map<String, Object> rawData = new LinkedHashMap<>();
        if (externalId != null) rawData.put("id", externalId);
        if (title != null) rawData.put("title", title);
        if (company != null) rawData.put("company", company);
        if (location != null) rawData.put("location", location);
        if (salaryMin != null) rawData.put("salary_min", salaryMin);
        if (salaryMax != null) rawData.put("salary_max", salaryMax);

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
                .salaryMin(salaryMin)
                .salaryMax(salaryMax)
                .currency(null) // Do NOT hardcode "INR"
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
                if (dateStr.length() >= 10) {
                    return LocalDate.parse(dateStr.substring(0, 10));
                }
                return null;
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private EmploymentType parseEmploymentType(String contractType, String contractTime) {
        String combined = ((contractType != null ? contractType : "") + " " + (contractTime != null ? contractTime : "")).toLowerCase().trim();
        if (combined.isBlank()) {
            return null;
        }
        if (combined.contains("part")) return EmploymentType.PART_TIME;
        if (combined.contains("contract") || combined.contains("freelance")) return EmploymentType.CONTRACT;
        if (combined.contains("temp")) return EmploymentType.TEMPORARY;
        if (combined.contains("intern")) return EmploymentType.INTERNSHIP;
        if (combined.contains("full") || combined.contains("permanent")) return EmploymentType.FULL_TIME;
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

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdzunaApiResponse {
        private Integer count;
        private List<AdzunaJobDto> results;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdzunaJobDto {
        private Object id;
        private String title;
        private String description;

        @JsonProperty("redirect_url")
        private String redirectUrl;

        private AdzunaCompanyDto company;
        private AdzunaLocationDto location;

        @JsonProperty("salary_min")
        private Double salaryMin;

        @JsonProperty("salary_max")
        private Double salaryMax;

        @JsonProperty("contract_type")
        private String contractType;

        @JsonProperty("contract_time")
        private String contractTime;

        private String created;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdzunaCompanyDto {
        @JsonProperty("display_name")
        private String displayName;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdzunaLocationDto {
        @JsonProperty("display_name")
        private String displayName;
        private List<String> area;
    }
}
