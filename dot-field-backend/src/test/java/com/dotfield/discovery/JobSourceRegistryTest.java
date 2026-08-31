package com.dotfield.discovery;

import com.dotfield.discovery.source.CompanyCareerPageSource;
import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.RawJobListing;
import com.dotfield.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JobSourceRegistryTest {

    private JobSourceRegistry registry;
    private CompanyCareerPageSource careerPageSource;
    private JobSource secondSource;

    @BeforeEach
    void setUp() {
        careerPageSource = new CompanyCareerPageSource();

        secondSource = new JobSource() {
            @Override
            public String getSourceName() {
                return "LINKEDIN";
            }

            @Override
            public boolean supports(String source) {
                return "LINKEDIN".equalsIgnoreCase(source);
            }

            @Override
            public List<RawJobListing> discover(JobDiscoveryRequest request) {
                return List.of();
            }
        };

        registry = new JobSourceRegistry(List.of(careerPageSource, secondSource));
    }

    @Test
    @DisplayName("getSource — Supported source name returns source adapter")
    void getSource_supportedSource_returnsSourceAdapter() {
        Optional<JobSource> source = registry.getSource("COMPANY_WEBSITE");
        assertTrue(source.isPresent());
        assertEquals("COMPANY_WEBSITE", source.get().getSourceName());
    }

    @Test
    @DisplayName("getSource — Case insensitive match returns source adapter")
    void getSource_caseInsensitiveMatch_returnsSourceAdapter() {
        Optional<JobSource> source = registry.getSource("company_website");
        assertTrue(source.isPresent());

        Optional<JobSource> linkedin = registry.getSource("linkedin");
        assertTrue(linkedin.isPresent());
        assertEquals("LINKEDIN", linkedin.get().getSourceName());
    }

    @Test
    @DisplayName("getSource — Unsupported source returns empty")
    void getSource_unsupportedSource_returnsEmpty() {
        Optional<JobSource> source = registry.getSource("UNSUPPORTED_FEED");
        assertTrue(source.isEmpty());
    }

    @Test
    @DisplayName("getRequiredSource — Unsupported source throws BadRequestException (400)")
    void getRequiredSource_unsupportedSource_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> registry.getRequiredSource("INVALID_SOURCE"));
    }

    @Test
    @DisplayName("getAllSources — Returns all registered sources")
    void getAllSources_returnsAllRegisteredSources() {
        List<JobSource> allSources = registry.getAllSources();
        assertEquals(2, allSources.size());
    }

    @Test
    @DisplayName("getAvailableSourceNames — Returns names of registered sources")
    void getAvailableSourceNames_returnsSourceNames() {
        List<String> sourceNames = registry.getAvailableSourceNames();
        assertEquals(2, sourceNames.size());
        assertTrue(sourceNames.contains("COMPANY_WEBSITE"));
        assertTrue(sourceNames.contains("LINKEDIN"));
    }

    @Test
    @DisplayName("isRegistered — Returns true for registered sources, false otherwise")
    void isRegistered_verifiesRegistration() {
        assertTrue(registry.isRegistered("COMPANY_WEBSITE"));
        assertTrue(registry.isRegistered("LINKEDIN"));
        assertFalse(registry.isRegistered("INDEED"));
    }
}
