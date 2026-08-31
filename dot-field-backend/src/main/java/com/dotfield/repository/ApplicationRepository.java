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

    Optional<Application> findByProfileIdAndJobId(Long profileId, Long jobId);

    boolean existsByProfileIdAndJobId(Long profileId, Long jobId);

    List<Application> findAllByProfileId(Long profileId);

    @Query("SELECT a.status, COUNT(a) FROM Application a WHERE a.profile.id = :profileId GROUP BY a.status")
    List<Object[]> countApplicationsByStatusGrouped(@Param("profileId") Long profileId);
}
