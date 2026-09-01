package com.dotfield.extractor;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Extractor adapter for normalizing raw job maps originating from the INDIANAPI source.
 */
@Component
public class IndianApiJobExtractor implements JobExtractor {

    public static final String SUPPORTED_SOURCE = "INDIANAPI";

    @Override
    public boolean supports(String source) {
        return source != null && SUPPORTED_SOURCE.equalsIgnoreCase(source.trim());
    }

    @Override
    public ExtractedJob extract(Map<String, Object> rawData, String source) {
        if (rawData == null) {
            return ExtractedJob.builder().source(source).build();
        }

        String title = getString(rawData, "title", "position_title");
        String company = getString(rawData, "company", "company_name");
        String location = getString(rawData, "location", "office_location");
        String description = getString(rawData, "description", "snippet");
        String jobUrl = getString(rawData, "jobUrl", "link", "url");

        String rawEmploymentType = getString(rawData, "employmentType", "job_type");
        String rawRemoteType = getString(rawData, "remoteType", "work_arrangement");

        BigDecimal salaryMin = getBigDecimal(rawData, "salaryMin", "salary_min");
        BigDecimal salaryMax = getBigDecimal(rawData, "salaryMax", "salary_max");
        String currency = getString(rawData, "currency");

        String rawPostedDate = getString(rawData, "postedDate", "publication_date");

        return ExtractedJob.builder()
                .title(title)
                .company(company)
                .location(location)
                .description(description)
                .jobUrl(jobUrl)
                .source(source)
                .employmentType(JobNormalizationUtil.normalizeEmploymentType(rawEmploymentType))
                .remoteType(JobNormalizationUtil.normalizeRemoteType(rawRemoteType))
                .salaryMin(salaryMin)
                .salaryMax(salaryMax)
                .currency(currency != null ? currency : "INR")
                .postedDate(JobNormalizationUtil.parseDate(rawPostedDate))
                .build();
    }

    private String getString(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key) && map.get(key) != null) {
                String val = map.get(key).toString().trim();
                if (!val.isEmpty()) {
                    return val;
                }
            }
        }
        return null;
    }

    private BigDecimal getBigDecimal(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key) && map.get(key) != null) {
                Object val = map.get(key);
                if (val instanceof Number number) {
                    return BigDecimal.valueOf(number.doubleValue());
                } else {
                    try {
                        return new BigDecimal(val.toString().trim());
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return null;
    }
}
