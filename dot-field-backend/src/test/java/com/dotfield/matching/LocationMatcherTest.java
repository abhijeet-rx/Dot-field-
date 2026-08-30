package com.dotfield.matching;

import com.dotfield.entity.Profile;
import com.dotfield.entity.RemoteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocationMatcherTest {

    private LocationMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new LocationMatcher();
    }

    @Test
    void match_remoteJob_returns100Score() {
        Profile profile = Profile.builder().location("Delhi, India").build();
        JobRequirements reqs = JobRequirements.builder().remoteType(RemoteType.REMOTE).build();

        LocationMatcher.LocationResult result = matcher.match(profile, reqs);

        assertEquals(100, result.score());
        assertEquals("REMOTE_COMPATIBLE", result.status());
    }

    @Test
    void match_onsiteMatchingLocation_returns100Score() {
        Profile profile = Profile.builder().location("Bangalore, India").build();
        JobRequirements reqs = JobRequirements.builder()
                .remoteType(RemoteType.ONSITE)
                .location("Bangalore, India")
                .build();

        LocationMatcher.LocationResult result = matcher.match(profile, reqs);

        assertEquals(100, result.score());
        assertEquals("LOCATION_MATCHED", result.status());
    }

    @Test
    void match_onsiteMismatchingLocation_returns0Score() {
        Profile profile = Profile.builder().location("Delhi, India").build();
        JobRequirements reqs = JobRequirements.builder()
                .remoteType(RemoteType.ONSITE)
                .location("Bangalore, India")
                .build();

        LocationMatcher.LocationResult result = matcher.match(profile, reqs);

        assertEquals(0, result.score());
        assertEquals("LOCATION_MISMATCH", result.status());
    }

    @Test
    void match_missingLocationDetails_returnsNullScore() {
        Profile profile = Profile.builder().build();
        JobRequirements reqs = JobRequirements.builder()
                .remoteType(RemoteType.ONSITE)
                .location("Bangalore, India")
                .build();

        LocationMatcher.LocationResult result = matcher.match(profile, reqs);

        assertNull(result.score());
        assertEquals("UNKNOWN", result.status());
    }
}
