package com.dotfield.dto;

import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.RemoteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Source-independent representation of a raw job listing fetched from an external source.
 * This class is decoupled from the JPA Job entity.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RawJobListing {

    private String externalId;
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
    private Map<String, Object> rawData;

    /**
     * Alias getter for postedDate.
     */
    public LocalDate getPostedAt() {
        return postedDate;
    }

    /**
     * Alias setter for postedDate.
     */
    public void setPostedAt(LocalDate postedAt) {
        this.postedDate = postedAt;
    }
}
