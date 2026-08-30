package com.dotfield.extractor;

import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.RemoteType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class JobNormalizationUtilTest {

    @Test
    void normalizeText_trimsWhitespaceAndHandlesNull() {
        assertEquals("Backend Developer", JobNormalizationUtil.normalizeText("  Backend Developer  "));
        assertNull(JobNormalizationUtil.normalizeText("   "));
        assertNull(JobNormalizationUtil.normalizeText(null));
    }

    @Test
    void normalizeSource_uppercasesAndDefaultsToOther() {
        assertEquals("COMPANY_WEBSITE", JobNormalizationUtil.normalizeSource("company_website"));
        assertEquals("LINKEDIN", JobNormalizationUtil.normalizeSource(" LinkedIn "));
        assertEquals("OTHER", JobNormalizationUtil.normalizeSource(""));
        assertEquals("OTHER", JobNormalizationUtil.normalizeSource(null));
    }

    @Test
    void normalizeEmploymentType_mapsVariationsAndUnknown() {
        assertEquals(EmploymentType.FULL_TIME, JobNormalizationUtil.normalizeEmploymentType("full time"));
        assertEquals(EmploymentType.FULL_TIME, JobNormalizationUtil.normalizeEmploymentType("full-time"));
        assertEquals(EmploymentType.PART_TIME, JobNormalizationUtil.normalizeEmploymentType("part-time"));
        assertEquals(EmploymentType.CONTRACT, JobNormalizationUtil.normalizeEmploymentType("contractor"));
        assertEquals(EmploymentType.INTERNSHIP, JobNormalizationUtil.normalizeEmploymentType("intern"));
        assertEquals(EmploymentType.TEMPORARY, JobNormalizationUtil.normalizeEmploymentType("temp"));
        assertEquals(EmploymentType.OTHER, JobNormalizationUtil.normalizeEmploymentType("unknown_type"));
        assertNull(JobNormalizationUtil.normalizeEmploymentType(null));
    }

    @Test
    void normalizeRemoteType_mapsVariationsAndUnknown() {
        assertEquals(RemoteType.REMOTE, JobNormalizationUtil.normalizeRemoteType("remote"));
        assertEquals(RemoteType.REMOTE, JobNormalizationUtil.normalizeRemoteType("work from home"));
        assertEquals(RemoteType.REMOTE, JobNormalizationUtil.normalizeRemoteType("wfh"));
        assertEquals(RemoteType.HYBRID, JobNormalizationUtil.normalizeRemoteType("hybrid"));
        assertEquals(RemoteType.ONSITE, JobNormalizationUtil.normalizeRemoteType("on-site"));
        assertEquals(RemoteType.OTHER, JobNormalizationUtil.normalizeRemoteType("custom_location"));
        assertNull(JobNormalizationUtil.normalizeRemoteType(null));
    }

    @Test
    void parseSalary_parsesFormattedRangesAndDetectsCurrency() {
        JobNormalizationUtil.ParsedSalary s1 = JobNormalizationUtil.parseSalary("$120,000 - $150,000", null, null, null);
        assertEquals(new BigDecimal("120000"), s1.salaryMin());
        assertEquals(new BigDecimal("150000"), s1.salaryMax());
        assertEquals("USD", s1.currency());

        JobNormalizationUtil.ParsedSalary s2 = JobNormalizationUtil.parseSalary("₹10,00,000 - ₹15,00,000", null, null, null);
        assertEquals(new BigDecimal("1000000"), s2.salaryMin());
        assertEquals(new BigDecimal("1500000"), s2.salaryMax());
        assertEquals("INR", s2.currency());
    }

    @Test
    void parseSalary_unparseableString_returnsNullSalaries() {
        JobNormalizationUtil.ParsedSalary s = JobNormalizationUtil.parseSalary("Competitive salary based on experience", null, null, null);
        assertNull(s.salaryMin());
        assertNull(s.salaryMax());
        assertNull(s.currency());
    }

    @Test
    void parseDate_parsesIsoDateAndHandlesRelativeDates() {
        assertEquals(LocalDate.of(2026, 8, 1), JobNormalizationUtil.parseDate("2026-08-01"));
        assertNull(JobNormalizationUtil.parseDate("Posted 2 days ago"));
        assertNull(JobNormalizationUtil.parseDate(null));
    }
}
