package com.dotfield.dto;

import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.RemoteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawJobListing {

    private String externalId;
    private String title;
    private String company;
    private String location;
    private String description;
    private String jobUrl;
    private String source;
    private EmploymentType employmentType;
    private RemoteType remoteType;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String currency;
    private LocalDate postedDate;
    private Map<String, Object> rawData;

}
