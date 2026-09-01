package com.dotfield.extractor;

import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.RemoteType;
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
public class ExtractedJob {

    private String title;
    private String company;
    private String location;
    private String normalizedCountry;
    private String normalizedCity;
    private String remoteCountry;
    private Boolean isIndiaRelevant;
    private String description;
    private String jobUrl;
    private String source;
    private EmploymentType employmentType;
    private RemoteType remoteType;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String currency;
    private LocalDate postedDate;

}
