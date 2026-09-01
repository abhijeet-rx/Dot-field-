package com.dotfield.discovery.india;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Normalizes raw location strings into structured {@link NormalizedLocation} models.
 * Determines deterministic India relevance based strictly on Indian cities, states,
 * and explicit India-remote eligibility patterns.
 * <p>
 * Does NOT classify generic English preposition "in" (e.g. "Engineer in London") as India.
 */
@Component
public class IndiaLocationNormalizer {

    private static final Map<String, String> CITY_NORMALIZATION_MAP = new LinkedHashMap<>();

    static {
        // Major Indian Tech Hubs & Cities
        registerCityVariants("Bengaluru", "bengaluru", "bangalore", "blr");
        registerCityVariants("Hyderabad", "hyderabad", "hyd");
        registerCityVariants("Chennai", "chennai", "madras");
        registerCityVariants("Pune", "pune");
        registerCityVariants("Mumbai", "mumbai", "bombay");
        registerCityVariants("Delhi NCR", "delhi", "new delhi", "ncr", "delhi ncr");
        registerCityVariants("Gurugram", "gurugram", "gurgaon");
        registerCityVariants("Noida", "noida", "greater noida");
        registerCityVariants("Kolkata", "kolkata", "calcutta");
        registerCityVariants("Ahmedabad", "ahmedabad");
        registerCityVariants("Jaipur", "jaipur");
        registerCityVariants("Kochi", "kochi", "cochin", "ernakulam");
        registerCityVariants("Chandigarh", "chandigarh", "mohali", "panchkula");
        registerCityVariants("Indore", "indore");
        registerCityVariants("Mysuru", "mysuru", "mysore");
        registerCityVariants("Thiruvananthapuram", "thiruvananthapuram", "trivandrum");
        registerCityVariants("Coimbatore", "coimbatore");
        registerCityVariants("Vadodara", "vadodara", "baroda");
        registerCityVariants("Visakhapatnam", "visakhapatnam", "vizag");
        registerCityVariants("Surat", "surat");
        registerCityVariants("Nagpur", "nagpur");
        registerCityVariants("Bhopal", "bhopal");
        registerCityVariants("Bhubaneswar", "bhubaneswar");
        registerCityVariants("Ranchi", "ranchi");
        registerCityVariants("Guwahati", "guwahati");
    }

    private static void registerCityVariants(String canonical, String... variants) {
        for (String variant : variants) {
            CITY_NORMALIZATION_MAP.put(variant, canonical);
        }
    }

    // Explicit India patterns (Do NOT include generic standalone "in" which causes false positives for "in London")
    private static final Pattern INDIA_COUNTRY_PATTERN = Pattern.compile(
            "(^|\\b)(india|bharat|india-based)(\\b|$)|\\b(india\\s*-\\s*remote|remote\\s*-\\s*india|remote\\s*\\(india\\)|anywhere\\s+in\\s+india)\\b|(^|,|\\s)IN($|,|\\s)",
            Pattern.CASE_INSENSITIVE
    );

    // Foreign countries and major international cities
    private static final Pattern FOREIGN_LOCATION_PATTERN = Pattern.compile(
            "\\b(usa|united states|us|uk|united kingdom|britain|germany|canada|singapore|australia|japan|france|london|san francisco|new york|toronto|berlin|sydney|tokyo|worldwide|global remote)\\b",
            Pattern.CASE_INSENSITIVE
    );

    public NormalizedLocation normalize(String rawLocation) {
        if (rawLocation == null || rawLocation.isBlank()) {
            return NormalizedLocation.builder()
                    .rawLocation(rawLocation)
                    .isIndiaRelevant(false)
                    .isRemote(false)
                    .build();
        }

        String trimmed = rawLocation.trim();
        String lower = trimmed.toLowerCase();

        boolean isRemote = lower.contains("remote") || lower.contains("work from home") || lower.contains("wfh") || lower.contains("anywhere");

        // 1. Check for Indian city matches first
        String matchedCity = null;
        for (Map.Entry<String, String> entry : CITY_NORMALIZATION_MAP.entrySet()) {
            String variant = entry.getKey();
            if (Pattern.compile("\\b" + Pattern.quote(variant) + "\\b", Pattern.CASE_INSENSITIVE).matcher(lower).find()) {
                matchedCity = entry.getValue();
                break;
            }
        }

        boolean hasExplicitIndia = INDIA_COUNTRY_PATTERN.matcher(lower).find();
        boolean hasForeignLocation = FOREIGN_LOCATION_PATTERN.matcher(lower).find();

        // 2. Foreign location takes precedence if explicitly foreign (e.g., "London, UK", "San Francisco, USA", "Developer in New York")
        if (hasForeignLocation && !hasExplicitIndia && matchedCity == null) {
            String foreignCountry = lower.contains("usa") || lower.contains("united states") || lower.contains("san francisco") || lower.contains("new york") ? "US"
                    : lower.contains("uk") || lower.contains("united kingdom") || lower.contains("london") ? "GB" : "FOREIGN";
            return NormalizedLocation.builder()
                    .rawLocation(trimmed)
                    .normalizedCountry(foreignCountry)
                    .isRemote(isRemote)
                    .isIndiaRelevant(false)
                    .build();
        }

        // 3. Indian location evaluation
        if ((matchedCity != null || hasExplicitIndia) && !hasForeignLocation) {
            return NormalizedLocation.builder()
                    .rawLocation(trimmed)
                    .normalizedCountry("IN")
                    .normalizedCity(matchedCity)
                    .isRemote(isRemote)
                    .remoteCountry(isRemote ? "IN" : null)
                    .isIndiaRelevant(true)
                    .build();
        }

        // Conflict resolution: If explicit foreign location exists alongside Indian keyword, foreign location wins
        if (hasForeignLocation) {
            return NormalizedLocation.builder()
                    .rawLocation(trimmed)
                    .isRemote(isRemote)
                    .isIndiaRelevant(false)
                    .build();
        }

        // 4. Generic Remote without explicit India tag -> NOT India relevant
        if (isRemote) {
            return NormalizedLocation.builder()
                    .rawLocation(trimmed)
                    .isRemote(true)
                    .isIndiaRelevant(false)
                    .build();
        }

        // 5. Default fallback
        return NormalizedLocation.builder()
                .rawLocation(trimmed)
                .isIndiaRelevant(false)
                .isRemote(false)
                .build();
    }
}
