package com.dotfield.extractor;

import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.RemoteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompanyWebsiteJobExtractorTest {

    private CompanyWebsiteJobExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new CompanyWebsiteJobExtractor();
    }

    @Test
    void supports_returnsTrueOnlyForCompanyWebsite() {
        assertTrue(extractor.supports("COMPANY_WEBSITE"));
        assertTrue(extractor.supports("company_website"));
        assertTrue(extractor.supports(" Company_Website "));
        assertFalse(extractor.supports("LINKEDIN"));
        assertFalse(extractor.supports("INDEED"));
        assertFalse(extractor.supports(null));
    }

    @Test
    void extract_extractsFieldsFromRawMap() {
        Map<String, Object> rawData = new HashMap<>();
        rawData.put("title", "Frontend Architect");
        rawData.put("company", "Vercel");
        rawData.put("location", "San Francisco, CA");
        rawData.put("description", "Lead Next.js platform engineering");
        rawData.put("jobUrl", "https://vercel.com/careers/123");
        rawData.put("employmentType", "Full Time");
        rawData.put("remoteType", "Remote");
        rawData.put("salaryMin", 180000);
        rawData.put("salaryMax", 240000);
        rawData.put("currency", "USD");
        rawData.put("postedDate", "2026-08-10");

        ExtractedJob job = extractor.extract(rawData, "COMPANY_WEBSITE");

        assertNotNull(job);
        assertEquals("Frontend Architect", job.getTitle());
        assertEquals("Vercel", job.getCompany());
        assertEquals("San Francisco, CA", job.getLocation());
        assertEquals("https://vercel.com/careers/123", job.getJobUrl());
        assertEquals("COMPANY_WEBSITE", job.getSource());
        assertEquals(EmploymentType.FULL_TIME, job.getEmploymentType());
        assertEquals(RemoteType.REMOTE, job.getRemoteType());
        assertEquals(new BigDecimal("180000.0"), job.getSalaryMin());
        assertEquals(new BigDecimal("240000.0"), job.getSalaryMax());
        assertEquals("USD", job.getCurrency());
        assertEquals(LocalDate.of(2026, 8, 10), job.getPostedDate());
    }

    @Test
    void extract_nullMap_returnsDefaultWithSource() {
        ExtractedJob job = extractor.extract(null, "COMPANY_WEBSITE");

        assertNotNull(job);
        assertEquals("COMPANY_WEBSITE", job.getSource());
        assertNull(job.getTitle());
        assertNull(job.getCompany());
    }
}
