package com.dotfield.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceDiscoveryResult {

    private String source;
    @Builder.Default
    private String status = "SUCCESS";
    private String errorMessage;
    private int discovered;
    private int indiaFiltered;
    private int newJobs;
    private int updatedJobs;
    private int unchangedJobs;
    private int duplicates;
    private int failed;

}
