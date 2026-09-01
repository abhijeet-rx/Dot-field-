package com.dotfield.discovery.source;

import com.dotfield.discovery.JobSource;
import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.RawJob;
import com.dotfield.dto.RawJobListing;
import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.RemoteType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Adapter for discovering jobs from official IndianAPI.
 * Endpoint: {@code https://jobs.indianapi.in/jobs}
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "job.sources.indianapi.enabled", havingValue = "true", matchIfMissing = true)
public class IndianApiJobSource implements JobSource {

    public static final String SOURCE_NAME = "INDIANAPI";

    private final String baseUrl;
    private final String apiKey;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @org.springframework.beans.factory.annotation.Autowired
    public IndianApiJobSource(
            @Value("${job.sources.indianapi.base-url:https://jobs.indianapi.in/jobs}") String baseUrl,
            @Value("${job.sources.indianapi.api-key:}") String apiKey,
            @Value("${job.sources.indianapi.connect-timeout:5000}") int connectTimeout,
            @Value("${job.sources.indianapi.read-timeout:10000}") int readTimeout) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public IndianApiJobSource(String baseUrl, String apiKey, RestClient restClient) {
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
            log.info("IndianApiJobSource: DISABLED — CREDENTIALS NOT CONFIGURED (INDIANAPI_KEY is empty)");
            return List.of();
        }

        String searchUrl = buildSearchUrl(request);
        log.info("Fetching job opportunities from IndianAPI...");

        try {
            String rawJson = restClient.get()
                    .uri(searchUrl)
                    .header("X-Api-Key", apiKey)
                    .header("x-api-key", apiKey)
                    .retrieve()
                    .body(String.class);

            if (rawJson == null || rawJson.isBlank()) {
                log.info("IndianAPI returned empty body");
                return List.of();
            }

            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode jobsNode = null;

            if (root.isArray()) {
                jobsNode = root;
            } else if (root.has("jobs") && root.get("jobs").isArray()) {
                jobsNode = root.get("jobs");
            } else if (root.has("data") && root.get("data").isArray()) {
                jobsNode = root.get("data");
            } else if (root.has("results") && root.get("results").isArray()) {
                jobsNode = root.get("results");
            }

            if (jobsNode == null || jobsNode.isEmpty()) {
                log.info("IndianAPI returned zero job items");
                return List.of();
            }

            int max = (request != null && request.getMaxResults() != null) ? request.getMaxResults() : 50;
            List<RawJobListing> rawJobs = new ArrayList<>();

            for (JsonNode item : jobsNode) {
                if (rawJobs.size() >= max) {
                    break;
                }
                RawJobListing listing = mapToRawJob(item);
                if (listing != null) {
                    rawJobs.add(listing);
                }
            }

            log.info("Successfully fetched and mapped {} jobs from IndianAPI", rawJobs.size());
            return rawJobs;
        } catch (Exception e) {
            log.error("Failed to fetch jobs from IndianAPI: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildSearchUrl(JobDiscoveryRequest request) {
        StringBuilder sb = new StringBuilder(baseUrl);
        List<String> params = new ArrayList<>();

        int limit = (request != null && request.getMaxResults() != null) ? request.getMaxResults() : 20;
        params.add("limit=" + limit);

        if (request != null && request.getKeyword() != null && !request.getKeyword().isBlank()) {
            params.add("title=" + encode(request.getKeyword().trim()));
        }

        if (request != null && request.getLocation() != null && !request.getLocation().isBlank()) {
            params.add("location=" + encode(request.getLocation().trim()));
        }

        sb.append(baseUrl.contains("?") ? "&" : "?").append(String.join("&", params));
        return sb.toString();
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    private RawJobListing mapToRawJob(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }

        String externalId = getText(node, "id", "job_id");
        String title = getText(node, "job_title", "title");
        String company = getText(node, "company", "company_name");
        String location = getText(node, "location");
        if (location == null || location.isBlank()) {
            location = "India";
        }
        String jobUrl = getText(node, "apply_link", "job_url", "link");
        String description = getText(node, "job_description", "description", "role_and_responsibility");

        if ((externalId == null || externalId.isBlank()) && jobUrl != null) {
            externalId = "IND-" + Math.abs(jobUrl.hashCode());
        }

        if (title == null || title.isBlank()) {
            return null;
        }

        String postedDateStr = getText(node, "posted_date", "publication_date");
        String jobTypeStr = getText(node, "job_type");

        LocalDate postedDate = parseDate(postedDateStr);
        EmploymentType employmentType = parseEmploymentType(jobTypeStr);

        Map<String, Object> rawData = new LinkedHashMap<>();
        if (externalId != null) rawData.put("id", externalId);
        if (title != null) rawData.put("title", title);
        if (company != null) rawData.put("company", company);
        if (location != null) rawData.put("location", location);

        return RawJob.builder()
                .externalId(externalId)
                .title(title)
                .company(company != null ? company : "Unknown")
                .location(location)
                .description(description != null ? description : "")
                .jobUrl(jobUrl)
                .source(SOURCE_NAME)
                .employmentType(employmentType)
                .remoteType(location.toLowerCase().contains("remote") ? RemoteType.REMOTE : RemoteType.HYBRID)
                .postedDate(postedDate)
                .rawData(rawData)
                .build();
    }

    private String getText(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.hasNonNull(key)) {
                String text = node.get(key).asText().trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return null;
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
            return EmploymentType.FULL_TIME;
        }
        String lower = typeStr.toLowerCase();
        if (lower.contains("part")) return EmploymentType.PART_TIME;
        if (lower.contains("contract") || lower.contains("freelance")) return EmploymentType.CONTRACT;
        if (lower.contains("intern")) return EmploymentType.INTERNSHIP;
        return EmploymentType.FULL_TIME;
    }
}
