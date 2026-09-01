package com.dotfield.discovery.india;

import com.dotfield.dto.RawJobListing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IndiaJobFilterTest {

    private IndiaJobFilter indiaJobFilter;

    @BeforeEach
    void setUp() {
        IndiaLocationNormalizer normalizer = new IndiaLocationNormalizer();
        indiaJobFilter = new IndiaJobFilter(normalizer);
    }

    @Test
    @DisplayName("Indian location job is accepted by IndiaJobFilter")
    void indianLocationAccepted() {
        RawJobListing rawJob = RawJobListing.builder()
                .title("Java Engineer")
                .company("Acme Corp")
                .location("Bengaluru, India")
                .currency("INR")
                .build();

        assertThat(indiaJobFilter.isIndiaRelevant(rawJob)).isTrue();
    }

    @Test
    @DisplayName("Explicit India remote job is accepted by IndiaJobFilter")
    void explicitIndiaRemoteAccepted() {
        RawJobListing rawJob = RawJobListing.builder()
                .title("Full Stack Developer")
                .company("Tech Corp")
                .location("Remote - India")
                .build();

        assertThat(indiaJobFilter.isIndiaRelevant(rawJob)).isTrue();
    }

    @Test
    @DisplayName("Foreign location job is rejected by IndiaJobFilter")
    void foreignLocationRejected() {
        RawJobListing rawJob = RawJobListing.builder()
                .title("Senior Backend Engineer")
                .company("US Tech")
                .location("San Francisco, USA")
                .build();

        assertThat(indiaJobFilter.isIndiaRelevant(rawJob)).isFalse();
    }

    @Test
    @DisplayName("Ambiguous Remote job without India eligibility is rejected by IndiaJobFilter")
    void ambiguousRemoteRejected() {
        RawJobListing rawJob = RawJobListing.builder()
                .title("DevOps Engineer")
                .company("Global Corp")
                .location("Remote")
                .build();

        assertThat(indiaJobFilter.isIndiaRelevant(rawJob)).isFalse();
    }

    @Test
    @DisplayName("INR currency alone without Indian location is NOT sufficient to accept job")
    void inrCurrencyAloneNotSufficient() {
        RawJobListing rawJob = RawJobListing.builder()
                .title("Cloud Architect")
                .company("Global Tech")
                .location("Remote")
                .currency("INR")
                .build();

        assertThat(indiaJobFilter.isIndiaRelevant(rawJob)).isFalse();
    }
}
