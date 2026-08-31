package com.dotfield.discovery.source;

import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.RawJobListing;
import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.RemoteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RemotiveJobSourceTest {

    private static final String BASE_URL = "https://remotive.com/api/remote-jobs";
    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer server;
    private RemotiveJobSource jobSource;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        jobSource = new RemotiveJobSource(BASE_URL, restClientBuilder.build());
    }

    @Test
    @DisplayName("getSourceName and supports — Identifies as REMOTIVE source")
    void sourceNameAndSupports() {
        assertEquals("REMOTIVE", jobSource.getSourceName());
        assertTrue(jobSource.supports("REMOTIVE"));
        assertTrue(jobSource.supports("remotive"));
        assertFalse(jobSource.supports("OTHER"));
        assertFalse(jobSource.supports(null));
    }

    @Test
    @DisplayName("discover — Successful response mapped into RawJobListings with externalId, URL, postedDate")
    void discover_successfulApiResponse_mapsFieldsCorrectly() {
        String jsonResponse = """
                {
                  "0-legal-notice": "https://remotive.com/api/remote-jobs",
                  "job-count": 2,
                  "jobs": [
                    {
                      "id": 189201,
                      "url": "https://remotive.com/remote-jobs/software-dev/senior-java-developer-189201",
                      "title": "Senior Java Developer",
                      "company_name": "Acme Tech",
                      "category": "Software Development",
                      "job_type": "full_time",
                      "publication_date": "2026-08-30T10:00:00",
                      "candidate_required_location": "Worldwide",
                      "salary": "$130k - $160k",
                      "description": "<p>We are seeking a Senior Java Engineer...</p>"
                    },
                    {
                      "id": 189202,
                      "url": "https://remotive.com/remote-jobs/software-dev/devops-contractor-189202",
                      "title": "DevOps Contractor",
                      "company_name": "Cloud Operations",
                      "category": "DevOps",
                      "job_type": "contract",
                      "publication_date": "2026-08-29T15:30:00",
                      "candidate_required_location": "US / Canada",
                      "salary": "$70/hr",
                      "description": "Kubernetes and Terraform specialist."
                    }
                  ]
                }
                """;

        server.expect(requestTo("https://remotive.com/api/remote-jobs?limit=10"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        JobDiscoveryRequest request = JobDiscoveryRequest.builder().source("REMOTIVE").maxResults(10).build();
        List<RawJobListing> results = jobSource.discover(request);

        assertNotNull(results);
        assertEquals(2, results.size());

        RawJobListing job1 = results.get(0);
        assertEquals("189201", job1.getExternalId());
        assertEquals("Senior Java Developer", job1.getTitle());
        assertEquals("Acme Tech", job1.getCompany());
        assertEquals("Worldwide", job1.getLocation());
        assertEquals("<p>We are seeking a Senior Java Engineer...</p>", job1.getDescription());
        assertEquals("https://remotive.com/remote-jobs/software-dev/senior-java-developer-189201", job1.getJobUrl());
        assertEquals("REMOTIVE", job1.getSource());
        assertEquals(EmploymentType.FULL_TIME, job1.getEmploymentType());
        assertEquals(RemoteType.REMOTE, job1.getRemoteType());
        assertEquals(LocalDate.of(2026, 8, 30), job1.getPostedDate());
        assertEquals("Software Development", job1.getRawData().get("category"));

        RawJobListing job2 = results.get(1);
        assertEquals("189202", job2.getExternalId());
        assertEquals("DevOps Contractor", job2.getTitle());
        assertEquals(EmploymentType.CONTRACT, job2.getEmploymentType());

        server.verify();
    }

    @Test
    @DisplayName("discover — Keyword search parameter appends query string")
    void discover_withKeyword_appendsQueryParam() {
        server.expect(requestTo("https://remotive.com/api/remote-jobs?search=Java&limit=5"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"jobs\": []}", MediaType.APPLICATION_JSON));

        JobDiscoveryRequest request = JobDiscoveryRequest.builder()
                .source("REMOTIVE")
                .keyword("Java")
                .maxResults(5)
                .build();

        List<RawJobListing> results = jobSource.discover(request);
        assertNotNull(results);
        assertTrue(results.isEmpty());

        server.verify();
    }

    @Test
    @DisplayName("discover — Empty response returns empty list cleanly")
    void discover_emptyResponse_returnsEmptyList() {
        server.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"jobs\": []}", MediaType.APPLICATION_JSON));

        List<RawJobListing> results = jobSource.discover(null);
        assertNotNull(results);
        assertTrue(results.isEmpty());

        server.verify();
    }

    @Test
    @DisplayName("discover — Malformed record missing both externalId and title is skipped")
    void discover_malformedRecord_skipped() {
        String jsonResponse = """
                {
                  "jobs": [
                    {
                      "id": null,
                      "title": null,
                      "company_name": "No ID or Title Co"
                    },
                    {
                      "id": 999,
                      "title": "Valid Developer",
                      "company_name": "Valid Co"
                    }
                  ]
                }
                """;

        server.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<RawJobListing> results = jobSource.discover(null);
        assertEquals(1, results.size());
        assertEquals("999", results.get(0).getExternalId());
        assertEquals("Valid Developer", results.get(0).getTitle());

        server.verify();
    }

    @Test
    @DisplayName("discover — Missing optional fields handled safely with defaults")
    void discover_missingOptionalFields_handledSafely() {
        String jsonResponse = """
                {
                  "jobs": [
                    {
                      "id": 555,
                      "title": "Minimal Job"
                    }
                  ]
                }
                """;

        server.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<RawJobListing> results = jobSource.discover(null);
        assertEquals(1, results.size());
        RawJobListing job = results.get(0);
        assertEquals("555", job.getExternalId());
        assertEquals("Minimal Job", job.getTitle());
        assertEquals("Unknown", job.getCompany());
        assertEquals("Remote", job.getLocation());
        assertNull(job.getPostedDate());
        assertEquals(EmploymentType.FULL_TIME, job.getEmploymentType());

        server.verify();
    }

    @Test
    @DisplayName("discover — HTTP 500 server error throws RuntimeException cleanly")
    void discover_httpServerError_throwsException() {
        server.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThrows(RuntimeException.class, () -> jobSource.discover(null));
        server.verify();
    }

    @Test
    @DisplayName("discover — Malformed JSON response throws RuntimeException cleanly")
    void discover_malformedJson_throwsException() {
        server.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("NOT_VALID_JSON{", MediaType.APPLICATION_JSON));

        assertThrows(RuntimeException.class, () -> jobSource.discover(null));
        server.verify();
    }
}
