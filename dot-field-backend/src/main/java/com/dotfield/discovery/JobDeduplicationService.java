package com.dotfield.discovery;

import com.dotfield.entity.Job;
import com.dotfield.extractor.JobNormalizationUtil;
import com.dotfield.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobDeduplicationService {

    private static final Set<String> TRACKING_PARAMS = Set.of(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "ref", "fbclid"
    );

    private final JobRepository jobRepository;

    @lombok.Setter
    @org.springframework.beans.factory.annotation.Value("${job.discovery.canonicalize-scheme:true}")
    private boolean canonicalizeScheme = true;

    public Optional<Job> findExistingJob(String source, String externalId, String rawUrl,
                                         String company, String title, String location, String description) {
        // Level 1 — Source + External ID
        if (source != null && !source.isBlank() && externalId != null && !externalId.isBlank()) {
            String normSource = source.trim().toUpperCase();
            String normExtId = externalId.trim();
            Optional<Job> level1Match = jobRepository.findBySourceAndExternalId(normSource, normExtId);
            if (level1Match.isPresent()) {
                log.debug("Level 1 deduplication match found for source: {}, externalId: {}", normSource, normExtId);
                return level1Match;
            }
        }

        // Level 2 — Canonical URL
        String canonicalUrl = canonicalizeUrl(rawUrl);
        if (canonicalUrl != null && !canonicalUrl.isBlank()) {
            Optional<Job> level2Match = jobRepository.findByCanonicalUrl(canonicalUrl);
            if (level2Match.isPresent()) {
                log.debug("Level 2 deduplication match found for canonicalUrl: {}", canonicalUrl);
                return level2Match;
            }
        }

        // Level 3 — Composite Fingerprint
        String fingerprint = generateFingerprint(company, title, location, description);
        if (fingerprint != null && !fingerprint.isBlank()) {
            Optional<Job> level3Match = jobRepository.findByDeduplicationFingerprint(fingerprint);
            if (level3Match.isPresent()) {
                log.debug("Level 3 deduplication match found for fingerprint: {}", fingerprint);
                return level3Match;
            }
        }

        return Optional.empty();
    }

    public String canonicalizeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        try {
            String trimmed = rawUrl.trim();
            URI uri = new URI(trimmed);

            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase() : null;
            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : null;
            if (scheme == null || host == null) {
                return trimmed;
            }

            int port = uri.getPort();
            if (("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) {
                port = -1;
            }

            if (canonicalizeScheme && ("http".equals(scheme) || "https".equals(scheme))) {
                scheme = "https";
            }

            String path = uri.getPath();
            if (path != null && path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            String query = uri.getRawQuery();
            String cleanQuery = null;
            if (query != null && !query.isBlank()) {
                cleanQuery = filterTrackingParams(query);
            }

            StringBuilder sb = new StringBuilder();
            sb.append(scheme).append("://").append(host);
            if (port != -1) {
                sb.append(":").append(port);
            }
            if (path != null) {
                sb.append(path);
            }
            if (cleanQuery != null && !cleanQuery.isBlank()) {
                sb.append("?").append(cleanQuery);
            }

            return sb.toString();
        } catch (Exception e) {
            return rawUrl.trim();
        }
    }

    /**
     * Generates a deterministic SHA-256 fingerprint for deduplication.
     * <p>
     * The fingerprint is computed from: {@code SHA-256(norm(company) + "|" + norm(title)
     * + "|" + norm(location) [+ "|" + norm(description)])}.
     * <p>
     * <strong>Null/blank behavior:</strong>
     * <ul>
     *   <li>If {@code company}, {@code title}, or {@code location} is null or blank
     *       after normalization, returns {@code null} (no fingerprint generated).
     *       This prevents overly aggressive deduplication when source data is insufficient.</li>
     *   <li>If {@code description} is null or blank after normalization, the fingerprint
     *       is still generated using only company + title + location.</li>
     * </ul>
     * <p>
     * No fuzzy matching or AI similarity is used. The hash is purely deterministic.
     *
     * @param company     the company name (required — returns null if missing)
     * @param title       the job title (required — returns null if missing)
     * @param location    the job location (required — returns null if missing)
     * @param description the job description (optional — included when present)
     * @return the SHA-256 hex string, or {@code null} if minimum fields are absent
     */
    public String generateFingerprint(String company, String title, String location, String description) {
        String normCompany = JobNormalizationUtil.normalizeText(company);
        String normTitle = JobNormalizationUtil.normalizeText(title);
        String normLocation = JobNormalizationUtil.normalizeText(location);

        if (normCompany == null || normTitle == null || normLocation == null) {
            return null;
        }

        String normDesc = JobNormalizationUtil.normalizeText(description);

        StringBuilder plain = new StringBuilder();
        plain.append(normCompany.toLowerCase())
                .append("|")
                .append(normTitle.toLowerCase())
                .append("|")
                .append(normLocation.toLowerCase());

        if (normDesc != null && !normDesc.isBlank()) {
            plain.append("|").append(normDesc.toLowerCase());
        }

        return sha256Hex(plain.toString());
    }

    private String filterTrackingParams(String rawQuery) {
        String[] pairs = rawQuery.split("&");
        List<String> preservedPairs = new ArrayList<>();
        for (String pair : pairs) {
            if (pair.isBlank()) continue;
            int idx = pair.indexOf("=");
            String key = idx > 0 ? pair.substring(0, idx) : pair;
            String keyLower = URLDecoder.decode(key, StandardCharsets.UTF_8).toLowerCase();
            if (!TRACKING_PARAMS.contains(keyLower)) {
                preservedPairs.add(pair);
            }
        }
        if (preservedPairs.isEmpty()) {
            return null;
        }
        return String.join("&", preservedPairs);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

}
