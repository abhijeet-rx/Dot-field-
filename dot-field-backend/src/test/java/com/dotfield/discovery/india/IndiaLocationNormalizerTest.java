package com.dotfield.discovery.india;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class IndiaLocationNormalizerTest {

    private IndiaLocationNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new IndiaLocationNormalizer();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Bangalore, India",
            "Bengaluru",
            "Hyderabad, India",
            "Chennai",
            "Pune, Maharashtra",
            "Mumbai",
            "Delhi NCR",
            "New Delhi",
            "Gurugram, HR",
            "Gurgaon",
            "Noida",
            "Kolkata",
            "Ahmedabad",
            "Jaipur",
            "Kochi",
            "Chandigarh",
            "Indore"
    })
    @DisplayName("Indian cities evaluate as India-relevant with normalized country IN")
    void indianCitiesEvaluateAsIndiaRelevant(String location) {
        NormalizedLocation norm = normalizer.normalize(location);
        assertThat(norm.isIndiaRelevant()).isTrue();
        assertThat(norm.getNormalizedCountry()).isEqualTo("IN");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Remote - India",
            "India - Remote",
            "Remote (India)",
            "Anywhere in India",
            "Remote, India",
            "Work from Anywhere in India"
    })
    @DisplayName("Explicit India remote strings evaluate as India-relevant remote roles")
    void explicitIndiaRemoteEvaluatesAsIndiaRelevant(String location) {
        NormalizedLocation norm = normalizer.normalize(location);
        assertThat(norm.isIndiaRelevant()).isTrue();
        assertThat(norm.isRemote()).isTrue();
        assertThat(norm.getRemoteCountry()).isEqualTo("IN");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "San Francisco, USA",
            "London, UK",
            "Berlin, Germany",
            "Toronto, Canada",
            "Singapore"
    })
    @DisplayName("Foreign locations evaluate as non-India relevant")
    void foreignLocationsEvaluateAsNonIndiaRelevant(String location) {
        NormalizedLocation norm = normalizer.normalize(location);
        assertThat(norm.isIndiaRelevant()).isFalse();
    }

    @Test
    @DisplayName("Generic Remote without India eligibility evaluates as non-India relevant")
    void genericRemoteEvaluatesAsNonIndiaRelevant() {
        NormalizedLocation norm = normalizer.normalize("Remote");
        assertThat(norm.isRemote()).isTrue();
        assertThat(norm.isIndiaRelevant()).isFalse();
    }

    @Test
    @DisplayName("Null or blank location returns non-India relevant (insufficient evidence)")
    void nullOrBlankLocationReturnsNonIndiaRelevant() {
        NormalizedLocation normNull = normalizer.normalize(null);
        assertThat(normNull.isIndiaRelevant()).isFalse();

        NormalizedLocation normBlank = normalizer.normalize("   ");
        assertThat(normBlank.isIndiaRelevant()).isFalse();
    }
}
