package com.dotfield.dto;

import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.JobStatus;
import com.dotfield.entity.RemoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateJobRequest {

    @NotBlank(message = "Job title is required")
    @Size(max = 200, message = "Job title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Company name is required")
    @Size(max = 200, message = "Company name must not exceed 200 characters")
    private String company;

    @Size(max = 200, message = "Location must not exceed 200 characters")
    private String location;

    private String description;

    @Size(max = 2048, message = "Job URL must not exceed 2048 characters")
    private String jobUrl;

    @Size(max = 100, message = "Source must not exceed 100 characters")
    private String source;

    private EmploymentType employmentType;

    private RemoteType remoteType;

    private JobStatus status;

    @PositiveOrZero(message = "Minimum salary cannot be negative")
    private BigDecimal salaryMin;

    @PositiveOrZero(message = "Maximum salary cannot be negative")
    private BigDecimal salaryMax;

    @Size(max = 10, message = "Currency code must not exceed 10 characters")
    private String currency;

    private LocalDate postedDate;

}
