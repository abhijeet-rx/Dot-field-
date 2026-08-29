package com.dotfield.service;

import com.dotfield.dto.EducationRequest;
import com.dotfield.dto.EducationResponse;
import com.dotfield.entity.Education;
import com.dotfield.entity.Profile;
import com.dotfield.exception.BadRequestException;
import com.dotfield.exception.ResourceNotFoundException;
import com.dotfield.mapper.ProfileMapper;
import com.dotfield.repository.EducationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EducationService {

    private final ProfileService profileService;
    private final EducationRepository educationRepository;
    private final ProfileMapper profileMapper;

    @Transactional(readOnly = true)
    public List<EducationResponse> getEducation() {
        Profile profile = profileService.getPrimaryProfileOrThrow();
        return educationRepository.findByProfileId(profile.getId()).stream()
                .map(profileMapper::toEducationResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EducationResponse addEducation(EducationRequest request) {
        Profile profile = profileService.getPrimaryProfileOrThrow();
        validateDates(request);

        Education education = profileMapper.toEducationEntity(request);
        profile.addEducation(education);

        Education saved = educationRepository.save(education);
        log.info("Added education record ID: {}", saved.getId());

        return profileMapper.toEducationResponse(saved);
    }

    @Transactional
    public EducationResponse updateEducation(Long id, EducationRequest request) {
        Profile profile = profileService.getPrimaryProfileOrThrow();
        Education education = educationRepository.findByIdAndProfileId(id, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Education", "id", id));

        validateDates(request);

        education.setInstitution(request.getInstitution());
        education.setDegree(request.getDegree());
        education.setFieldOfStudy(request.getFieldOfStudy());
        education.setStartDate(request.getStartDate());
        education.setEndDate(request.getEndDate());
        education.setGrade(request.getGrade());

        Education saved = educationRepository.save(education);
        log.info("Updated education record ID: {}", saved.getId());

        return profileMapper.toEducationResponse(saved);
    }

    @Transactional
    public void deleteEducation(Long id) {
        Profile profile = profileService.getPrimaryProfileOrThrow();
        Education education = educationRepository.findByIdAndProfileId(id, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Education", "id", id));

        profile.removeEducation(education);
        educationRepository.delete(education);
        log.info("Deleted education record ID: {}", id);
    }

    private void validateDates(EducationRequest request) {
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("Start date cannot be after end date");
        }
    }

}
