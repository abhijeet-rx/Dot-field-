package com.dotfield.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractJobRequest {

    @NotBlank(message = "Source is required")
    private String source;

    @NotNull(message = "Raw data is required")
    private Map<String, Object> rawData;

}
