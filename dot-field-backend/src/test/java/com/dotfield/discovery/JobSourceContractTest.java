package com.dotfield.discovery;

import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.RawJob;
import com.dotfield.dto.RawJobListing;
import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.RemoteType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JobSourceContractTest {

    @Test
    @DisplayName("JobSource Interface Contract — default fetchJobs() delegates to discover()")
    void jobSourceContract_defaultFetchJobsDelegatesToDiscover() {
        JobSource mockSource = new JobSource() {
            @Override
            public String getSourceName() {
                return "TEST_SOURCE";
            }

            @Override
            public boolean supports(String source) {
                return "TEST_SOURCE".equalsIgnoreCase(source);
            }

            @Override
            public List<RawJobListing> discover(JobDiscoveryRequest request) {
                return List.of(RawJobListing.builder()
                        .externalId("JOB-1")
                        .title("Software Engineer")
                        .company("Test Co")
                        .source("TEST_SOURCE")
                        .build());
            }
        };

        assertEquals("TEST_SOURCE", mockSource.getSourceName());
        assertTrue(mockSource.supports("TEST_SOURCE"));
        assertTrue(mockSource.supports("test_source"));
        assertFalse(mockSource.supports("OTHER_SOURCE"));

        JobDiscoveryRequest request = JobDiscoveryRequest.builder().source("TEST_SOURCE").build();
        List<RawJobListing> discovered = mockSource.discover(request);
        List<RawJobListing> fetched = mockSource.fetchJobs(request);

        assertEquals(1, discovered.size());
        assertEquals(discovered, fetched);
        assertEquals("JOB-1", fetched.get(0).getExternalId());
    }

    @Test
    @DisplayName("RawJob & RawJobListing Representation — encapsulates all external job fields without JPA entity dependency")
    void rawJobRepresentation_encapsulatesFieldsCorrectly() {
        LocalDate now = LocalDate.now();

        RawJobListing rawListing = RawJobListing.builder()
                .externalId("EXT-101")
                .title("Senior Backend Developer")
                .company("Acme Inc")
                .location("Remote")
                .description("Build microservices in Java.")
                .jobUrl("https://acme.com/jobs/101")
                .source("COMPANY_WEBSITE")
                .employmentType(EmploymentType.FULL_TIME)
                .remoteType(RemoteType.REMOTE)
                .salaryMin(BigDecimal.valueOf(120000))
                .salaryMax(BigDecimal.valueOf(150000))
                .currency("USD")
                .postedDate(now)
                .rawData(Map.of("rawKey", "rawValue"))
                .build();

        assertEquals("EXT-101", rawListing.getExternalId());
        assertEquals("Senior Backend Developer", rawListing.getTitle());
        assertEquals("Acme Inc", rawListing.getCompany());
        assertEquals("Remote", rawListing.getLocation());
        assertEquals("Build microservices in Java.", rawListing.getDescription());
        assertEquals("https://acme.com/jobs/101", rawListing.getJobUrl());
        assertEquals("COMPANY_WEBSITE", rawListing.getSource());
        assertEquals(EmploymentType.FULL_TIME, rawListing.getEmploymentType());
        assertEquals(RemoteType.REMOTE, rawListing.getRemoteType());
        assertEquals(BigDecimal.valueOf(120000), rawListing.getSalaryMin());
        assertEquals(BigDecimal.valueOf(150000), rawListing.getSalaryMax());
        assertEquals("USD", rawListing.getCurrency());
        assertEquals(now, rawListing.getPostedDate());
        assertEquals(now, rawListing.getPostedAt());
        assertEquals("rawValue", rawListing.getRawData().get("rawKey"));

        // Verify RawJob subclass behavior
        RawJob rawJob = RawJob.builder()
                .externalId("EXT-102")
                .title("Frontend Developer")
                .company("Acme Inc")
                .source("LINKEDIN")
                .build();

        assertEquals("EXT-102", rawJob.getExternalId());
        assertEquals("Frontend Developer", rawJob.getTitle());
        assertEquals("LINKEDIN", rawJob.getSource());
    }
}
