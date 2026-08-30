package com.dotfield.dto;

import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.RemoteType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDiscoveryRequest {

    @NotBlank(message = "Source is required")
    private String source;

    private String keyword;
    private String company;
    private String location;
    private RemoteType remoteType;
    private EmploymentType employmentType;

    @Min(value = 1, message = "maxResults must be at least 1")
    @Max(value = 100, message = "maxResults cannot exceed 100")
    @Builder.Default
    private Integer maxResults = 20;

}
