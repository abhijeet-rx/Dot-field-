package com.dotfield.discovery;

import com.dotfield.discovery.source.CompanyCareerPageSource;
import com.dotfield.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JobSourceRegistryTest {

    private JobSourceRegistry registry;
    private CompanyCareerPageSource careerPageSource;

    @BeforeEach
    void setUp() {
        careerPageSource = new CompanyCareerPageSource();
        registry = new JobSourceRegistry(List.of(careerPageSource));
    }

    @Test
    void getSource_supportedSource_returnsSourceAdapter() {
        Optional<JobSource> source = registry.getSource("COMPANY_WEBSITE");
        assertTrue(source.isPresent());
        assertEquals("COMPANY_WEBSITE", source.get().getSourceName());
    }

    @Test
    void getSource_caseInsensitiveMatch_returnsSourceAdapter() {
        Optional<JobSource> source = registry.getSource("company_website");
        assertTrue(source.isPresent());
    }

    @Test
    void getSource_unsupportedSource_returnsEmpty() {
        Optional<JobSource> source = registry.getSource("UNSUPPORTED_FEED");
        assertTrue(source.isEmpty());
    }

    @Test
    void getRequiredSource_unsupportedSource_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> registry.getRequiredSource("INVALID_SOURCE"));
    }

}
