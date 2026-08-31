package com.dotfield.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionStatusResponse {

    private LocalDateTime lastRun;
    private Long durationMs;
    private int sourcesProcessed;
    private int jobsFetched;
    private int jobsInserted;
    private int jobsUpdated;
    private int duplicates;
    private int failures;
    private List<SourceStatusDto> sources;

}
