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
public class TailoredProjectResponse {

    private Long id;
    private String name;
    private String description;
    private String githubUrl;
    private String liveUrl;

    @Builder.Default
    private List<String> technologies = new ArrayList<>();

    private boolean emphasized;
    private int projectScore;

    @Builder.Default
    private List<String> matchingKeywords = new ArrayList<>();

}
