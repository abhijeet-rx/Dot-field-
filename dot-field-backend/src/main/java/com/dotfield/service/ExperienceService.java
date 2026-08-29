package com.dotfield.service;

import com.dotfield.dto.ExperienceRequest;
import com.dotfield.dto.ExperienceResponse;
import com.dotfield.entity.Experience;
import com.dotfield.entity.Profile;
import com.dotfield.exception.BadRequestException;
import com.dotfield.exception.ResourceNotFoundException;
import com.dotfield.mapper.ProfileMapper;
import com.dotfield.repository.ExperienceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExperienceService {

    private final ProfileService profileService;
    private final ExperienceRepository experienceRepository;
    private final ProfileMapper profileMapper;

    @Transactional(readOnly = true)
    public List<ExperienceResponse> getExperience() {
        Profile profile = profileService.getPrimaryProfileOrThrow();
        return experienceRepository.findByProfileId(profile.getId()).stream()
                .map(profileMapper::toExperienceResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ExperienceResponse addExperience(ExperienceRequest request) {
        Profile profile = profileService.getPrimaryProfileOrThrow();
        validateDates(request);

        Experience experience = profileMapper.toExperienceEntity(request);
        profile.addExperience(experience);

        Experience saved = experienceRepository.save(experience);
        log.info("Added experience record ID: {}", saved.getId());

        return profileMapper.toExperienceResponse(saved);
    }

    @Transactional
    public ExperienceResponse updateExperience(Long id, ExperienceRequest request) {
        Profile profile = profileService.getPrimaryProfileOrThrow();
        Experience experience = experienceRepository.findByIdAndProfileId(id, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Experience", "id", id));

        validateDates(request);

        experience.setCompany(request.getCompany());
        experience.setRole(request.getRole());
        experience.setDescription(request.getDescription());
        experience.setStartDate(request.getStartDate());
        experience.setEndDate(request.getEndDate());

        Experience saved = experienceRepository.save(experience);
        log.info("Updated experience record ID: {}", saved.getId());

        return profileMapper.toExperienceResponse(saved);
    }

    @Transactional
    public void deleteExperience(Long id) {
        Profile profile = profileService.getPrimaryProfileOrThrow();
        Experience experience = experienceRepository.findByIdAndProfileId(id, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Experience", "id", id));

        profile.removeExperience(experience);
        experienceRepository.delete(experience);
        log.info("Deleted experience record ID: {}", id);
    }

    private void validateDates(ExperienceRequest request) {
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("Start date cannot be after end date");
        }
    }

}
