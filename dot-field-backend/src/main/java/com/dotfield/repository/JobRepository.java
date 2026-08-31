package com.dotfield.repository;

import com.dotfield.entity.Job;
import com.dotfield.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    Optional<Job> findBySourceAndExternalId(String source, String externalId);

    Optional<Job> findByCanonicalUrl(String canonicalUrl);

    Optional<Job> findByDeduplicationFingerprint(String deduplicationFingerprint);

    @Query("SELECT j FROM Job j WHERE UPPER(j.source) = UPPER(:source) AND j.status != :expiredStatus AND " +
           "( (j.lastSeenAt IS NOT NULL AND j.lastSeenAt < :threshold) OR " +
           "  (j.lastSeenAt IS NULL AND j.lastDiscoveredAt IS NOT NULL AND j.lastDiscoveredAt < :threshold) OR " +
           "  (j.lastSeenAt IS NULL AND j.lastDiscoveredAt IS NULL AND j.createdAt < :threshold) )")
    List<Job> findStaleJobsForSource(@Param("source") String source,
                                     @Param("expiredStatus") JobStatus expiredStatus,
                                     @Param("threshold") LocalDateTime threshold);

}
