package com.dotfield.matching;

import com.dotfield.entity.Profile;
import com.dotfield.entity.RemoteType;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class LocationMatcher {

    public record LocationResult(
            Integer score, // Null if UNKNOWN
            String status, // REMOTE_COMPATIBLE, LOCATION_MATCHED, LOCATION_MISMATCH, UNKNOWN
            String analysis
    ) {}

    public LocationResult match(Profile profile, JobRequirements requirements) {
        RemoteType remoteType = requirements.getRemoteType();

        if (remoteType == RemoteType.REMOTE) {
            return new LocationResult(
                    100,
                    "REMOTE_COMPATIBLE",
                    "Job is fully remote and compatible regardless of candidate physical location."
            );
        }

        String jobLoc = requirements.getLocation();
        String candLoc = (profile != null && profile.getLocation() != null) ? profile.getLocation() : null;

        if (jobLoc == null || jobLoc.isBlank() || candLoc == null || candLoc.isBlank()) {
            return new LocationResult(
                    null,
                    "UNKNOWN",
                    "Location compatibility cannot be determined because location details are incomplete."
            );
        }

        String jobLocLower = jobLoc.trim().toLowerCase(Locale.ROOT);
        String candLocLower = candLoc.trim().toLowerCase(Locale.ROOT);

        if (jobLocLower.contains(candLocLower) || candLocLower.contains(jobLocLower)) {
            return new LocationResult(
                    100,
                    "LOCATION_MATCHED",
                    String.format("Candidate location '%s' matches job location '%s'.", candLoc, jobLoc)
            );
        } else {
            return new LocationResult(
                    0,
                    "LOCATION_MISMATCH",
                    String.format("Candidate location '%s' differs from job location '%s'.", candLoc, jobLoc)
            );
        }
    }

}
