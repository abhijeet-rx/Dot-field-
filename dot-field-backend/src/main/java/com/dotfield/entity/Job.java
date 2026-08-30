package com.dotfield.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_jobs_source_external_id", columnNames = {"source", "externalId"}),
                @UniqueConstraint(name = "uk_jobs_canonical_url", columnNames = {"canonicalUrl"})
        },
        indexes = {
                @Index(name = "idx_jobs_fingerprint", columnList = "deduplicationFingerprint")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200)
    private String externalId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 200)
    private String company;

    @Column(length = 200)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 2048)
    private String jobUrl;

    @Column(length = 2048)
    private String canonicalUrl;

    @Column(length = 64)
    private String deduplicationFingerprint;

    @Column(nullable = false, length = 100)
    private String source;

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    private RemoteType remoteType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(precision = 15, scale = 2)
    private BigDecimal salaryMin;

    @Column(precision = 15, scale = 2)
    private BigDecimal salaryMax;

    @Column(length = 10)
    private String currency;

    private LocalDate postedDate;

    private LocalDateTime lastDiscoveredAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = JobStatus.SAVED;
        }
        if (this.source == null || this.source.trim().isEmpty()) {
            this.source = "MANUAL";
        } else {
            this.source = this.source.trim().toUpperCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.source != null && !this.source.trim().isEmpty()) {
            this.source = this.source.trim().toUpperCase();
        }
    }
}
