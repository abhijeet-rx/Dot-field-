package com.dotfield.discovery;

import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.JobDiscoveryResponse;
import com.dotfield.dto.RawJobListing;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SpringBootTest
class RealApiDiscoveryTest {

    static {
        try {
            Path envPath = Paths.get(".env");
            if (Files.exists(envPath)) {
                Files.readAllLines(envPath).forEach(line -> {
                    String trimmed = line.trim();
                    if (trimmed.contains("=") && !trimmed.startsWith("#")) {
                        String[] parts = trimmed.split("=", 2);
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        if (!key.isEmpty()) {
                            System.setProperty(key, value);
                        }
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Could not load .env file for test: " + e.getMessage());
        }
    }

    @Autowired
    private JobDiscoveryService discoveryService;

    @Autowired
    private JobSourceRegistry sourceRegistry;

    @Test
    @DisplayName("Execute live discovery test for IndianAPI, Jooble, and Adzuna")
    void executeRealApiDiscoveryTest() {
        System.out.println("=================================================");
        System.out.println("  STARTING REAL API DISCOVERY VERIFICATION TEST  ");
        System.out.println("=================================================");

        // 1. Test IndianAPI
        if (sourceRegistry.isRegistered("INDIANAPI")) {
            System.out.println("\n--- Testing Source: INDIANAPI ---");
            JobDiscoveryRequest req = JobDiscoveryRequest.builder().source("INDIANAPI").keyword("Software Engineer").maxResults(5).build();
            JobDiscoveryResponse res = discoveryService.discoverJobs(req);
            System.out.printf("Discovered: %d, India Filtered: %d, New: %d, Duplicates: %d%n",
                    res.getDiscovered(), res.getIndiaFiltered(), res.getNewJobs(), res.getDuplicates());
        }

        // 2. Test Jooble
        if (sourceRegistry.isRegistered("JOOBLE")) {
            System.out.println("\n--- Testing Source: JOOBLE ---");
            JobDiscoveryRequest req = JobDiscoveryRequest.builder().source("JOOBLE").keyword("Java Developer").maxResults(5).build();
            JobDiscoveryResponse res = discoveryService.discoverJobs(req);
            System.out.printf("Discovered: %d, India Filtered: %d, New: %d, Duplicates: %d%n",
                    res.getDiscovered(), res.getIndiaFiltered(), res.getNewJobs(), res.getDuplicates());
        }

        // 3. Test Adzuna
        if (sourceRegistry.isRegistered("ADZUNA")) {
            System.out.println("\n--- Testing Source: ADZUNA ---");
            JobDiscoveryRequest req = JobDiscoveryRequest.builder().source("ADZUNA").keyword("Developer").maxResults(5).build();
            JobDiscoveryResponse res = discoveryService.discoverJobs(req);
            System.out.printf("Discovered: %d, India Filtered: %d, New: %d, Duplicates: %d%n",
                    res.getDiscovered(), res.getIndiaFiltered(), res.getNewJobs(), res.getDuplicates());
        }

        // 4. Test Multi-Source Ingestion Pipeline
        System.out.println("\n--- Testing ALL Sources Ingestion Run ---");
        JobDiscoveryRequest reqAll = JobDiscoveryRequest.builder().source("ALL").maxResults(10).build();
        JobDiscoveryResponse resAll = discoveryService.discoverJobs(reqAll);
        System.out.printf("ALL Sources Discovered: %d, India Filtered: %d, New: %d, Duplicates: %d%n",
                resAll.getDiscovered(), resAll.getIndiaFiltered(), resAll.getNewJobs(), resAll.getDuplicates());

        System.out.println("=================================================");
        System.out.println("  REAL API DISCOVERY VERIFICATION COMPLETED     ");
        System.out.println("=================================================");
    }
}
