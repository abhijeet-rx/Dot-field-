package com.dotfield.extractor;

import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.RemoteType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JobNormalizationUtil {

    private static final Pattern RANGE_PATTERN = Pattern.compile("([\\$₹€£]?[\\d,]+(?:\\.\\d+)?)\\s*[-–—to]+\\s*([\\$₹€£]?[\\d,]+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SINGLE_NUMERIC_PATTERN = Pattern.compile("([\\$₹€£]?[\\d,]+(?:\\.\\d+)?)");

    public static String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String normalizeSource(String source) {
        if (source == null || source.trim().isEmpty()) {
            return "OTHER";
        }
        return source.trim().toUpperCase();
    }

    public static EmploymentType normalizeEmploymentType(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String cleaned = raw.trim().toLowerCase().replaceAll("[\\s_-]+", "");
        return switch (cleaned) {
            case "fulltime", "fulltimejob", "permanent" -> EmploymentType.FULL_TIME;
            case "parttime", "parttimejob" -> EmploymentType.PART_TIME;
            case "contract", "contractor", "freelance" -> EmploymentType.CONTRACT;
            case "internship", "intern" -> EmploymentType.INTERNSHIP;
            case "temporary", "temp" -> EmploymentType.TEMPORARY;
            case "other" -> EmploymentType.OTHER;
            default -> EmploymentType.OTHER;
        };
    }

    public static RemoteType normalizeRemoteType(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String cleaned = raw.trim().toLowerCase().replaceAll("[\\s_-]+", "");
        return switch (cleaned) {
            case "remote", "workfromhome", "wfh", "telecommute" -> RemoteType.REMOTE;
            case "hybrid", "flexible" -> RemoteType.HYBRID;
            case "onsite", "inoffice" -> RemoteType.ONSITE;
            case "other" -> RemoteType.OTHER;
            default -> RemoteType.OTHER;
        };
    }

    public static String detectCurrency(String text, String defaultCurrency) {
        if (defaultCurrency != null && !defaultCurrency.trim().isEmpty()) {
            return defaultCurrency.trim().toUpperCase();
        }
        if (text == null) {
            return null;
        }
        String upper = text.toUpperCase();
        if (upper.contains("$") || upper.contains("USD")) {
            return "USD";
        } else if (upper.contains("₹") || upper.contains("INR")) {
            return "INR";
        } else if (upper.contains("€") || upper.contains("EUR")) {
            return "EUR";
        } else if (upper.contains("£") || upper.contains("GBP")) {
            return "GBP";
        }
        return null;
    }

    public static ParsedSalary parseSalary(String rawSalary, BigDecimal rawMin, BigDecimal rawMax, String rawCurrency) {
        String currency = detectCurrency(rawSalary, rawCurrency);
        BigDecimal min = rawMin;
        BigDecimal max = rawMax;

        if ((min == null || max == null) && rawSalary != null && !rawSalary.trim().isEmpty()) {
            String text = rawSalary.trim();

            Matcher rangeMatcher = RANGE_PATTERN.matcher(text);
            if (rangeMatcher.find()) {
                BigDecimal pMin = parseNumber(rangeMatcher.group(1));
                BigDecimal pMax = parseNumber(rangeMatcher.group(2));
                if (pMin != null && pMax != null) {
                    min = (min == null) ? pMin : min;
                    max = (max == null) ? pMax : max;
                }
            } else {
                Matcher singleMatcher = SINGLE_NUMERIC_PATTERN.matcher(text);
                if (singleMatcher.find()) {
                    BigDecimal pMin = parseNumber(singleMatcher.group(1));
                    if (pMin != null && min == null) {
                        min = pMin;
                    }
                }
            }
        }

        return new ParsedSalary(min, max, currency);
    }

    public static LocalDate parseDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(rawDate.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static BigDecimal parseNumber(String str) {
        if (str == null) return null;
        String cleanStr = str.replaceAll("[^\\d.]", "");
        if (cleanStr.isEmpty()) return null;
        try {
            return new BigDecimal(cleanStr);
        } catch (Exception ignored) {
            return null;
        }
    }

    public record ParsedSalary(BigDecimal salaryMin, BigDecimal salaryMax, String currency) {}
}
