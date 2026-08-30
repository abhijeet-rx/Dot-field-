package com.dotfield.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDiscoveryResponse {

    private int discovered;
    private int newJobs;
    private int updatedJobs;
    private int unchangedJobs;
    private int duplicates;
    private int failed;
    private List<SourceDiscoveryResult> sourceResults;

}
