package com.dotfield.matching;

import java.util.*;
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

}
