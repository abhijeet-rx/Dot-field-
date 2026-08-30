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
public class TailoringAnalysisResponse {

    @Builder.Default
    private List<String> emphasizedSkills = new ArrayList<>();

    @Builder.Default
    private List<String> emphasizedExperiences = new ArrayList<>();

    @Builder.Default
    private List<String> emphasizedProjects = new ArrayList<>();

    @Builder.Default
    private List<String> matchedKeywords = new ArrayList<>();

    @Builder.Default
    private List<String> unusedJobKeywords = new ArrayList<>();

    private String tailoringNotes;

}
