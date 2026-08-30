package com.dotfield.matching;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SkillNormalizationUtil {

    private static final Map<String, String> ALIAS_MAP = new HashMap<>();

    static {
        ALIAS_MAP.put("js", "javascript");
        ALIAS_MAP.put("javascript", "javascript");
        ALIAS_MAP.put("ts", "typescript");
        ALIAS_MAP.put("typescript", "typescript");
        ALIAS_MAP.put("reactjs", "react");
        ALIAS_MAP.put("react", "react");
        ALIAS_MAP.put("postgres", "postgresql");
        ALIAS_MAP.put("postgresql", "postgresql");
        ALIAS_MAP.put("k8s", "kubernetes");
        ALIAS_MAP.put("kubernetes", "kubernetes");
        ALIAS_MAP.put("node", "nodejs");
        ALIAS_MAP.put("node.js", "nodejs");
        ALIAS_MAP.put("nodejs", "nodejs");
        ALIAS_MAP.put("springboot", "spring boot");
        ALIAS_MAP.put("spring boot", "spring boot");
    }

    public static String normalize(String skillName) {
        if (skillName == null || skillName.isBlank()) {
            return null;
        }

        String cleaned = skillName.trim().toLowerCase(Locale.ROOT);
        return ALIAS_MAP.getOrDefault(cleaned, cleaned);
    }

    public static Set<String> normalizeSet(Collection<String> rawSkills) {
        if (rawSkills == null || rawSkills.isEmpty()) {
            return Collections.emptySet();
        }

        return rawSkills.stream()
                .map(SkillNormalizationUtil::normalize)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Safely checks if free text contains a specific keyword using word boundaries
     * and collision guards (e.g. Java != JavaScript, React != React Native, C != C++).
     */
    public static boolean containsKeyword(String text, String keyword) {
        if (text == null || text.isBlank() || keyword == null || keyword.isBlank()) {
            return false;
        }

        String normKeyword = normalize(keyword);
        if (normKeyword == null || normKeyword.isBlank()) {
            return false;
        }

        String patternStr = "(?<=^|[^a-zA-Z0-9#+.-])" + Pattern.quote(normKeyword) + "(?=$|[^a-zA-Z0-9#+.-])";
        Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            int end = matcher.end();

            // Collision guard: 'react' vs 'react native'
            if (normKeyword.equalsIgnoreCase("react")) {
                String remainder = text.substring(end).toLowerCase(Locale.ROOT);
                if (remainder.startsWith(" native") || remainder.startsWith("-native")) {
                    continue;
                }
            }

            // Collision guard: 'c' vs 'c++' or 'c#'
            if (normKeyword.equalsIgnoreCase("c")) {
                String remainder = text.substring(end).toLowerCase(Locale.ROOT);
                if (remainder.startsWith("++") || remainder.startsWith("#")) {
                    continue;
                }
            }

            return true;
        }

        return false;
    }

}
