package com.dotfield.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TailoredResumeResponse {

    private Long jobId;
    private Long profileId;
    private String summary;

    private TailoredSkillsResponse skills;

    @Builder.Default
    private List<TailoredExperienceResponse> experience = new ArrayList<>();

    @Builder.Default
    private List<TailoredEducationResponse> education = new ArrayList<>();

    @Builder.Default
    private List<TailoredProjectResponse> projects = new ArrayList<>();

    @Builder.Default
    private List<TailoredLinkResponse> links = new ArrayList<>();

    private TailoringAnalysisResponse tailoringAnalysis;

}
