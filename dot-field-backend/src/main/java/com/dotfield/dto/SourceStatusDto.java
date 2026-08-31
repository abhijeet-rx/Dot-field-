package com.dotfield.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceStatusDto {

    private String source;
    private String status; // SUCCESS, FAILED, PARTIAL_SUCCESS, NO_JOBS, IDLE
    private LocalDateTime lastSuccessfulRun;
    private LocalDateTime lastFailure;
    private int jobsFetched;
    private String errorMessage;

}
