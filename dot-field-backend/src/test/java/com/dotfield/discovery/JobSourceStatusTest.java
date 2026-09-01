package com.dotfield.discovery;

import com.dotfield.discovery.source.CompanyCareerPageSource;
import com.dotfield.discovery.source.NaukriJobSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JobSourceStatusTest {

    @Test
    @DisplayName("JobSourceRegistry returns active sources accurately")
    void registryReturnsActiveSources() {
        CompanyCareerPageSource companySource = new CompanyCareerPageSource();
        NaukriJobSource naukriSource = new NaukriJobSource(null, null);

        JobSourceRegistry registry = new JobSourceRegistry(List.of(companySource, naukriSource));

        assertThat(registry.getAllSources()).hasSize(2);
        assertThat(registry.isRegistered("COMPANY_WEBSITE")).isTrue();
        assertThat(registry.isRegistered("NAUKRI")).isTrue();
        assertThat(registry.isRegistered("NON_EXISTENT")).isFalse();
    }

    @Test
    @DisplayName("NaukriJobSource without credentials logs warning and returns empty list safely")
    void naukriWithoutCredentialsReturnsEmptyList() {
        NaukriJobSource naukriSource = new NaukriJobSource(null, null);

        assertThat(naukriSource.getSourceName()).isEqualTo("NAUKRI");
        assertThat(naukriSource.discover(null)).isEmpty();
    }
}
