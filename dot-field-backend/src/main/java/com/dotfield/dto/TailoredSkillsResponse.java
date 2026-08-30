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
public class TailoredSkillsResponse {

    @Builder.Default
    private List<String> primary = new ArrayList<>();

    @Builder.Default
    private List<String> secondary = new ArrayList<>();

}
