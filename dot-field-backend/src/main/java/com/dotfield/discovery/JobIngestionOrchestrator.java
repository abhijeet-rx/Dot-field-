package com.dotfield.discovery;

import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.JobDiscoveryResponse;

/**
 * Service orchestrator responsible for executing the end-to-end job ingestion pipeline:
 * <pre>
 *   JobSource (fetch)
 *     ↓
 *   RawJob (raw external listing)
 *     ↓
 *   JobExtractionPipeline (extract & normalize)
 *     ↓
 *   JobDeduplicationService (canonicalize & fingerprint)
 *     ↓
 *   JobDiscoveryPersistenceHelper (REQUIRES_NEW transaction per listing)
 *     ↓
 *   JobRepository (database persistence)
 * </pre>
 */
public interface JobIngestionOrchestrator {

    /**
     * Primary method for ingesting/discovering jobs from a single specified source.
     */
    JobDiscoveryResponse discoverJobs(JobDiscoveryRequest request);

    /**
     * Primary method for ingesting/discovering jobs from ALL registered sources with error isolation.
     */
    JobDiscoveryResponse discoverFromAllSources(JobDiscoveryRequest request);

    /**
     * Alias method for ingesting jobs from a single specified source.
     */
    default JobDiscoveryResponse ingestFromSource(JobDiscoveryRequest request) {
        return discoverJobs(request);
    }

    /**
     * Alias method for ingesting jobs from ALL registered sources with error isolation.
     */
    default JobDiscoveryResponse ingestFromAllSources(JobDiscoveryRequest request) {
        return discoverFromAllSources(request);
    }
}
