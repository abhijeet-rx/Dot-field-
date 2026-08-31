package com.dotfield.extractor;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Extractor adapter for normalizing raw job maps originating from the REMOTIVE source.
 */
@Component
public class RemotiveJobExtractor implements JobExtractor {

    public static final String SUPPORTED_SOURCE = "REMOTIVE";

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
        String location = getString(rawData, "location", "candidate_required_location");
        String description = getString(rawData, "description");
        String jobUrl = getString(rawData, "jobUrl", "url");

        String rawEmploymentType = getString(rawData, "employmentType", "job_type");
        String rawRemoteType = getString(rawData, "remoteType", "work_arrangement");

        BigDecimal salaryMin = getBigDecimal(rawData, "salaryMin", "minSalary");
        BigDecimal salaryMax = getBigDecimal(rawData, "salaryMax", "maxSalary");
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
                .currency(currency)
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
