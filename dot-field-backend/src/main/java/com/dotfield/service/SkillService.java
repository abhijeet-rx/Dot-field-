package com.dotfield.service;

import com.dotfield.dto.SkillRequest;
import com.dotfield.dto.SkillResponse;
import com.dotfield.entity.Profile;
import com.dotfield.entity.Skill;
import com.dotfield.exception.BadRequestException;
import com.dotfield.exception.ResourceNotFoundException;
import com.dotfield.mapper.ProfileMapper;
import com.dotfield.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillService {

    private final ProfileService profileService;
    private final SkillRepository skillRepository;
    private final ProfileMapper profileMapper;

    @Transactional(readOnly = true)
    public List<SkillResponse> getSkills() {
        Profile profile = profileService.getPrimaryProfileOrThrow();
        return skillRepository.findByProfileId(profile.getId()).stream()
                .map(profileMapper::toSkillResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SkillResponse addSkill(SkillRequest request) {
        Profile profile = profileService.getPrimaryProfileOrThrow();

        if (skillRepository.existsByProfileIdAndNameIgnoreCase(profile.getId(), request.getName().trim())) {
            throw new BadRequestException("Skill '" + request.getName().trim() + "' already exists");
        }

        Skill skill = profileMapper.toSkillEntity(request);
        profile.addSkill(skill);

        Skill saved = skillRepository.save(skill);
        log.info("Added skill '{}' with ID: {}", saved.getName(), saved.getId());

        return profileMapper.toSkillResponse(saved);
    }

    @Transactional
    public void deleteSkill(Long id) {
        Profile profile = profileService.getPrimaryProfileOrThrow();
        Skill skill = skillRepository.findByIdAndProfileId(id, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill", "id", id));

        profile.removeSkill(skill);
        skillRepository.delete(skill);
        log.info("Deleted skill ID: {}", id);
    }

}
