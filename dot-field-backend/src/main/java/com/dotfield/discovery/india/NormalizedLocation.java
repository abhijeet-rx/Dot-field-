package com.dotfield.discovery.india;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured location model produced by {@link IndiaLocationNormalizer}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NormalizedLocation {

    private String rawLocation;
    private String normalizedCountry; // e.g. "IN", "US", "GB"
    private String normalizedCity;    // e.g. "Bengaluru", "Hyderabad", "Mumbai"
    private boolean isRemote;
    private String remoteCountry;     // e.g. "IN" for "Remote - India"
    private boolean isIndiaRelevant;
}
