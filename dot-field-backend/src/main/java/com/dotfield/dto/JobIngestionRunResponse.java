package com.dotfield.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobIngestionRunResponse {

    private int sourcesProcessed;
    private int jobsFetched;
    private int jobsInserted;
    private int jobsUpdated;
    private int duplicates;
    private int failed;

    /**
     * Map internal {@link JobDiscoveryResponse} object to public {@link JobIngestionRunResponse} summary format.
     */
    public static JobIngestionRunResponse fromJobDiscoveryResponse(JobDiscoveryResponse response) {
        if (response == null) {
            return JobIngestionRunResponse.builder().build();
        }

        int sourcesCount = response.getSourceResults() != null ? response.getSourceResults().size() : 0;
        int totalDuplicates = response.getDuplicates() + response.getUnchangedJobs();

        return JobIngestionRunResponse.builder()
                .sourcesProcessed(sourcesCount)
                .jobsFetched(response.getDiscovered())
                .jobsInserted(response.getNewJobs())
                .jobsUpdated(response.getUpdatedJobs())
                .duplicates(totalDuplicates)
                .failed(response.getFailed())
                .build();
    }
}
