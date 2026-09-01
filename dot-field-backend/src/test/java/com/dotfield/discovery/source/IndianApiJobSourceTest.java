package com.dotfield.discovery.source;

import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.RawJobListing;
import com.dotfield.entity.RemoteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class IndianApiJobSourceTest {

    private IndianApiJobSource source;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        source = new IndianApiJobSource("https://jobs.indianapi.in/jobs", "test-api-key", builder.build());
    }

    @Test
    void supports_validSource_returnsTrue() {
        assertTrue(source.supports("INDIANAPI"));
        assertTrue(source.supports("indianapi"));
        assertFalse(source.supports("LINKEDIN"));
    }

    @Test
    void discover_missingApiKey_returnsEmptyList() {
        IndianApiJobSource unconfiguredSource = new IndianApiJobSource("https://jobs.indianapi.in/jobs", "", RestClient.builder().build());
        List<RawJobListing> listings = unconfiguredSource.discover(JobDiscoveryRequest.builder().source("INDIANAPI").build());
        assertNotNull(listings);
        assertTrue(listings.isEmpty());
    }

    @Test
    void discover_successfulApiResponse_mapsListingsCorrectly() {
        String jsonResponse = """
                {
                  "jobs": [
                    {
                      "id": "IND-101",
                      "job_title": "Java Developer",
                      "company": "TCS",
                      "location": "Bengaluru, India",
                      "job_description": "Building microservices",
                      "apply_link": "https://jobs.indianapi.in/job/101"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("https://jobs.indianapi.in/jobs")))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("INDIANAPI")
                .keyword("Java")
                .maxResults(10)
                .build();

        List<RawJobListing> listings = source.discover(request);

        assertNotNull(listings);
        assertEquals(1, listings.size());
        RawJobListing job = listings.get(0);
        assertEquals("IND-101", job.getExternalId());
        assertEquals("Java Developer", job.getTitle());
        assertEquals("TCS", job.getCompany());
        assertEquals("Bengaluru, India", job.getLocation());
        assertEquals("INDIANAPI", job.getSource());
        assertNull(job.getCurrency()); // Currency must be null if absent
        assertNull(job.getEmploymentType()); // Employment type must be null if absent
    }

    @Test
    void discover_missingLocation_leavesLocationNull() {
        String jsonResponse = """
                {
                  "jobs": [
                    {
                      "id": "IND-102",
                      "job_title": "Python Developer",
                      "company": "Infosys",
                      "job_description": "Django backend"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("https://jobs.indianapi.in/jobs")))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        JobDiscoveryRequest request = JobDiscoveryRequest.builder().source("INDIANAPI").build();
        List<RawJobListing> listings = source.discover(request);

        assertNotNull(listings);
        assertEquals(1, listings.size());
        assertNull(listings.get(0).getLocation()); // Location must remain null, NOT "India"
        assertNull(listings.get(0).getRemoteType()); // RemoteType must remain null, NOT HYBRID
    }

    @Test
    void discover_httpServerError_returnsEmptyListWithoutCrashing() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("https://jobs.indianapi.in/jobs")))
                .andRespond(withServerError());

        JobDiscoveryRequest request = JobDiscoveryRequest.builder().source("INDIANAPI").build();
        List<RawJobListing> listings = source.discover(request);

        assertNotNull(listings);
        assertTrue(listings.isEmpty());
    }
}
