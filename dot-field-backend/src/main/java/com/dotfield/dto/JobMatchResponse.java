package com.dotfield.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchResponse {

    private Long jobId;
    private Long profileId;
    private int overallScore;
    private String matchCategory;

    private Integer skillScore;
    private Integer experienceScore;
    private Integer educationScore;
    private Integer locationScore;

    private Set<String> matchedRequiredSkills;
    private Set<String> missingRequiredSkills;
    private Set<String> matchedPreferredSkills;
    private Set<String> missingPreferredSkills;

    private String experienceAnalysis;
    private String educationAnalysis;
    private String locationAnalysis;

    private List<String> strengths;
    private List<String> gaps;

}
