package com.dotfield.discovery.india;

import com.dotfield.dto.RawJobListing;
import com.dotfield.extractor.ExtractedJob;
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
    @DisplayName("Title location conflict — Title says Bangalore but Location says London, UK must be rejected")
    void titleLocationConflict_explicitForeignLocationWins() {
        RawJobListing rawJob = RawJobListing.builder()
                .title("Senior Engineer - Bangalore")
                .company("Global Corp")
                .location("London, UK")
                .build();

        assertThat(indiaJobFilter.isIndiaRelevant(rawJob)).isFalse();

        ExtractedJob extractedJob = ExtractedJob.builder()
                .title("Senior Engineer - Bangalore")
                .company("Global Corp")
                .location("London, UK")
                .build();

        assertThat(indiaJobFilter.isIndiaRelevant(extractedJob)).isFalse();
    }

    @Test
    @DisplayName("Regression Test — 'Software Engineer in London' must be rejected")
    void softwareEngineerInLondon_rejected() {
        RawJobListing rawJob = RawJobListing.builder()
                .title("Software Engineer")
                .company("Global Corp")
                .location("Software Engineer in London")
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

    @Test
    @DisplayName("Indian company name alone with generic Remote location is NOT sufficient to accept job")
    void indianCompanyNameAloneNotSufficient() {
        RawJobListing rawJob = RawJobListing.builder()
                .title("Product Manager")
                .company("Tata Consultancy Services")
                .location("Remote")
                .build();

        assertThat(indiaJobFilter.isIndiaRelevant(rawJob)).isFalse();
    }
}
