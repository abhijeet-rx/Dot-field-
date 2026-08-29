package com.dotfield.service;

import com.dotfield.dto.ProfileResponse;
import com.dotfield.dto.UpdateProfileRequest;
import com.dotfield.entity.Profile;
import com.dotfield.exception.ResourceNotFoundException;
import com.dotfield.mapper.ProfileMapper;
import com.dotfield.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile() {
        Profile profile = getPrimaryProfileOrThrow();
        return profileMapper.toProfileResponse(profile);
    }

    @Transactional
    public ProfileResponse updateProfile(UpdateProfileRequest request) {
        Profile profile = profileRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> Profile.builder().build());

        profile.setName(request.getName());
        profile.setEmail(request.getEmail());
        profile.setPhone(request.getPhone());
        profile.setLocation(request.getLocation());
        profile.setLinkedinUrl(request.getLinkedinUrl());
        profile.setGithubUrl(request.getGithubUrl());
        profile.setPortfolioUrl(request.getPortfolioUrl());

        Profile saved = profileRepository.save(profile);
        log.info("Saved candidate profile with ID: {}", saved.getId());

        return profileMapper.toProfileResponse(saved);
    }

    @Transactional(readOnly = true)
    public Profile getPrimaryProfileOrThrow() {
        return profileRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));
    }

}
