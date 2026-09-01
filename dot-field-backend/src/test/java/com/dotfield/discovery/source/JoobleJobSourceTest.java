package com.dotfield.discovery.source;

import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.RawJobListing;
import com.dotfield.entity.EmploymentType;
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

class JoobleJobSourceTest {

    private JoobleJobSource source;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        source = new JoobleJobSource("https://jooble.org/api/", "test-jooble-key", builder.build());
    }

    @Test
    void supports_validSource_returnsTrue() {
        assertTrue(source.supports("JOOBLE"));
        assertTrue(source.supports("jooble"));
        assertFalse(source.supports("LINKEDIN"));
    }

    @Test
    void discover_missingApiKey_returnsEmptyList() {
        JoobleJobSource unconfiguredSource = new JoobleJobSource("https://jooble.org/api/", "", RestClient.builder().build());
        List<RawJobListing> listings = unconfiguredSource.discover(JobDiscoveryRequest.builder().source("JOOBLE").build());
        assertNotNull(listings);
        assertTrue(listings.isEmpty());
    }

    @Test
    void discover_successfulApiResponse_mapsListingsCorrectly() {
        String jsonResponse = """
                {
                  "totalCount": 1,
                  "jobs": [
                    {
                      "id": "123456",
                      "title": "Backend Engineer",
                      "company": "Swiggy",
                      "location": "Remote - India",
                      "snippet": "React and Node developer",
                      "type": "Full-time",
                      "salary": "₹15,000 - ₹25,000",
                      "link": "https://jooble.org/desc/123456"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://jooble.org/api/test-jooble-key"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("JOOBLE")
                .keyword("Backend")
                .maxResults(10)
                .build();

        List<RawJobListing> listings = source.discover(request);

        assertNotNull(listings);
        assertEquals(1, listings.size());
        RawJobListing job = listings.get(0);
        assertEquals("123456", job.getExternalId());
        assertEquals("Backend Engineer", job.getTitle());
        assertEquals("Swiggy", job.getCompany());
        assertEquals("Remote - India", job.getLocation());
        assertEquals("JOOBLE", job.getSource());
        assertEquals(EmploymentType.FULL_TIME, job.getEmploymentType());
        assertEquals(RemoteType.REMOTE, job.getRemoteType());
        assertEquals("INR", job.getCurrency());
    }

    @Test
    void discover_missingLocation_leavesLocationNull() {
        String jsonResponse = """
                {
                  "totalCount": 1,
                  "jobs": [
                    {
                      "id": "123457",
                      "title": "Frontend Engineer",
                      "company": "Zomato",
                      "snippet": "Vue developer",
                      "link": "https://jooble.org/desc/123457"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://jooble.org/api/test-jooble-key"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        JobDiscoveryRequest request = JobDiscoveryRequest.builder().source("JOOBLE").build();
        List<RawJobListing> listings = source.discover(request);

        assertNotNull(listings);
        assertEquals(1, listings.size());
        assertNull(listings.get(0).getLocation()); // Location must remain null, NOT "India"
        assertNull(listings.get(0).getRemoteType()); // RemoteType must remain null, NOT HYBRID
        assertNull(listings.get(0).getCurrency()); // Currency must remain null if absent
    }

    @Test
    void discover_httpError_returnsEmptyListWithoutCrashing() {
        mockServer.expect(requestTo("https://jooble.org/api/test-jooble-key"))
                .andRespond(withServerError());

        JobDiscoveryRequest request = JobDiscoveryRequest.builder().source("JOOBLE").build();
        List<RawJobListing> listings = source.discover(request);

        assertNotNull(listings);
        assertTrue(listings.isEmpty());
    }
}
