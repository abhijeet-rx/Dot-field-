package com.dotfield.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceResponse {

    private Long id;
    private String company;
    private String role;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;

}
