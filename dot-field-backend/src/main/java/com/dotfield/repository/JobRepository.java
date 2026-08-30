package com.dotfield.repository;

import com.dotfield.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    Optional<Job> findBySourceAndExternalId(String source, String externalId);

    Optional<Job> findByCanonicalUrl(String canonicalUrl);

    Optional<Job> findByDeduplicationFingerprint(String deduplicationFingerprint);

}
