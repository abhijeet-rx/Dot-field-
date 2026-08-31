package com.dotfield.repository;

import com.dotfield.entity.Application;
import com.dotfield.entity.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByIdAndProfileId(Long id, Long profileId);

    Page<Application> findAllByProfileId(Long profileId, Pageable pageable);

    Page<Application> findAllByProfileIdAndStatus(Long profileId, ApplicationStatus status, Pageable pageable);

    @Query("SELECT a FROM Application a WHERE a.profile.id = :profileId " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:search IS NULL OR LOWER(a.job.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.job.company) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Application> searchApplications(@Param("profileId") Long profileId,
                                         @Param("status") ApplicationStatus status,
                                         @Param("search") String search,
                                         Pageable pageable);

    Optional<Application> findByProfileIdAndJobId(Long profileId, Long jobId);

    boolean existsByProfileIdAndJobId(Long profileId, Long jobId);

    List<Application> findAllByProfileId(Long profileId);

    // --- Aggregate queries for analytics (Issue 4) ---

    long countByProfileId(Long profileId);

    @Query("SELECT a.status, COUNT(a) FROM Application a WHERE a.profile.id = :profileId GROUP BY a.status")
    List<Object[]> countByProfileIdGroupByStatus(@Param("profileId") Long profileId);

    long countByProfileIdAndAppliedAtIsNotNull(Long profileId);

    @Query("SELECT AVG(a.fitScore) FROM Application a WHERE a.profile.id = :profileId AND a.fitScore IS NOT NULL")
    Double averageFitScoreByProfileId(@Param("profileId") Long profileId);
}
