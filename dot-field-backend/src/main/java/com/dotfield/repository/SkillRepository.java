package com.dotfield.repository;

import com.dotfield.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findByProfileId(Long profileId);

    Optional<Skill> findByIdAndProfileId(Long id, Long profileId);

    boolean existsByProfileIdAndNameIgnoreCase(Long profileId, String name);

}
