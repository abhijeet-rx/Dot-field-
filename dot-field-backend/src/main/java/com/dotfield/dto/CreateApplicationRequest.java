package com.dotfield.dto;

import com.dotfield.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApplicationRequest {

    @NotNull(message = "Job ID is required")
    private Long jobId;

    private ApplicationStatus status;

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;
}
