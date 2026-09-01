package com.dotfield.discovery.source;

import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.RawJobListing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AdzunaJobSourceTest {

    private AdzunaJobSource source;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        source = new AdzunaJobSource("https://api.adzuna.com/1/api/jobs/in/search", "test-app-id", "test-app-key", builder.build());
    }

    @Test
    void supports_validSource_returnsTrue() {
        assertTrue(source.supports("ADZUNA"));
        assertTrue(source.supports("adzuna"));
        assertFalse(source.supports("LINKEDIN"));
    }

    @Test
    void discover_missingAppIdOrKey_returnsEmptyList() {
        AdzunaJobSource unconfiguredSource = new AdzunaJobSource("https://api.adzuna.com/1/api/jobs/in/search", "", "", RestClient.builder().build());
        List<RawJobListing> listings = unconfiguredSource.discover(JobDiscoveryRequest.builder().source("ADZUNA").build());
        assertNotNull(listings);
        assertTrue(listings.isEmpty());
    }

    @Test
    void discover_successfulApiResponse_mapsListingsCorrectly() {
        String jsonResponse = """
                {
                  "count": 1,
                  "results": [
                    {
                      "id": "4920194821",
                      "title": "Full Stack Developer",
                      "company": { "display_name": "Flipkart" },
                      "location": { "display_name": "Bengaluru, India" },
                      "description": "Kafka and Spark data pipelines",
                      "redirect_url": "https://www.adzuna.in/land/ad/4920194821"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("https://api.adzuna.com/1/api/jobs/in/search")))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("ADZUNA")
                .keyword("Developer")
                .maxResults(10)
                .build();

        List<RawJobListing> listings = source.discover(request);

        assertNotNull(listings);
        assertEquals(1, listings.size());
        RawJobListing job = listings.get(0);
        assertEquals("4920194821", job.getExternalId());
        assertEquals("Full Stack Developer", job.getTitle());
        assertEquals("Flipkart", job.getCompany());
        assertEquals("Bengaluru, India", job.getLocation());
        assertEquals("ADZUNA", job.getSource());
    }

    @Test
    void discover_httpServerError_returnsEmptyListWithoutCrashing() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("https://api.adzuna.com/1/api/jobs/in/search")))
                .andRespond(withServerError());

        JobDiscoveryRequest request = JobDiscoveryRequest.builder().source("ADZUNA").build();
        List<RawJobListing> listings = source.discover(request);

        assertNotNull(listings);
        assertTrue(listings.isEmpty());
    }
}
