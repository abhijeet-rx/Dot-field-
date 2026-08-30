package com.dotfield.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TailoredExperienceResponse {

    private Long id;
    private String company;
    private String role;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean emphasized;

    @Builder.Default
    private List<String> matchingKeywords = new ArrayList<>();

}
