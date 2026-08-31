package com.dotfield.dto;

import com.dotfield.entity.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationAnalyticsResponse {

    private long totalApplications;
    private Map<ApplicationStatus, Long> statusCounts;
    private double responseRate;
    private double interviewRate;
    private double offerRate;
    private double averageFitScore;
}
