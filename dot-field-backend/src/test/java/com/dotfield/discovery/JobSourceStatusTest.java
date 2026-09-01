package com.dotfield.discovery;

import com.dotfield.discovery.source.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JobSourceStatusTest {

    @Test
    @DisplayName("JobSourceRegistry returns active sources accurately")
    void registryReturnsActiveSources() {
        CompanyCareerPageSource companySource = new CompanyCareerPageSource();
        NaukriJobSource naukriSource = new NaukriJobSource(null, null);
        IndianApiJobSource indianApiSource = new IndianApiJobSource("https://indianapi.in/jobs", "key", RestClient.builder().build());
        JoobleJobSource joobleSource = new JoobleJobSource("https://jooble.org/api/", "key", RestClient.builder().build());
        AdzunaJobSource adzunaSource = new AdzunaJobSource("https://api.adzuna.com", "id", "key", RestClient.builder().build());

        JobSourceRegistry registry = new JobSourceRegistry(List.of(companySource, naukriSource, indianApiSource, joobleSource, adzunaSource));

        assertThat(registry.getAllSources()).hasSize(5);
        assertThat(registry.isRegistered("COMPANY_WEBSITE")).isTrue();
        assertThat(registry.isRegistered("NAUKRI")).isTrue();
        assertThat(registry.isRegistered("INDIANAPI")).isTrue();
        assertThat(registry.isRegistered("JOOBLE")).isTrue();
        assertThat(registry.isRegistered("ADZUNA")).isTrue();
        assertThat(registry.isRegistered("NON_EXISTENT")).isFalse();
    }

    @Test
    @DisplayName("API sources without credentials log notice and return empty list safely without crashing")
    void apiSourcesWithoutCredentialsReturnEmptyList() {
        IndianApiJobSource indianApiSource = new IndianApiJobSource("https://indianapi.in/jobs", "", RestClient.builder().build());
        JoobleJobSource joobleSource = new JoobleJobSource("https://jooble.org/api/", "", RestClient.builder().build());
        AdzunaJobSource adzunaSource = new AdzunaJobSource("https://api.adzuna.com", "", "", RestClient.builder().build());

        assertThat(indianApiSource.discover(null)).isEmpty();
        assertThat(joobleSource.discover(null)).isEmpty();
        assertThat(adzunaSource.discover(null)).isEmpty();
    }
}
