package com.dotfield.extractor;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Extractor adapter for normalizing raw job maps originating from the JOOBLE source.
 */
@Component
public class JoobleJobExtractor implements JobExtractor {

    public static final String SUPPORTED_SOURCE = "JOOBLE";

    @Override
    public boolean supports(String source) {
        return source != null && SUPPORTED_SOURCE.equalsIgnoreCase(source.trim());
    }

    @Override
    public ExtractedJob extract(Map<String, Object> rawData, String source) {
        if (rawData == null) {
            return ExtractedJob.builder().source(source).build();
        }

        String title = getString(rawData, "title");
        String company = getString(rawData, "company");
        String location = getString(rawData, "location");
        String description = getString(rawData, "description", "snippet");
        String jobUrl = getString(rawData, "jobUrl", "link");

        String rawEmploymentType = getString(rawData, "employmentType", "type");
        String rawRemoteType = getString(rawData, "remoteType");

        BigDecimal salaryMin = getBigDecimal(rawData, "salaryMin");
        BigDecimal salaryMax = getBigDecimal(rawData, "salaryMax");
        String currency = getString(rawData, "currency");

        String rawPostedDate = getString(rawData, "postedDate", "updated");

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
