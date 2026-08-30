package com.dotfield.dto;

import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.JobStatus;
import com.dotfield.entity.RemoteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {

    private Long id;
    private String title;
    private String company;
    private String location;
    private String description;
    private String jobUrl;
    private String source;
    private EmploymentType employmentType;
    private RemoteType remoteType;
    private JobStatus status;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String currency;
    private LocalDate postedDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
