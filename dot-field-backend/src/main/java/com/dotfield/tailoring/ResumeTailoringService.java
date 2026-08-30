package com.dotfield.tailoring;

import com.dotfield.dto.TailoredResumeResponse;
import com.dotfield.entity.Job;
import com.dotfield.entity.Profile;
import com.dotfield.exception.ResourceNotFoundException;
import com.dotfield.matching.JobRequirementExtractor;
import com.dotfield.matching.JobRequirements;
import com.dotfield.repository.JobRepository;
import com.dotfield.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeTailoringService {

    private final ProfileRepository profileRepository;
    private final JobRepository jobRepository;
    private final JobRequirementExtractor requirementExtractor;
    private final ResumeTailoringEngine tailoringEngine;

    @Transactional(readOnly = true)
    public TailoredResumeResponse tailorResume(Long jobId) {
        Profile profile = profileRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        JobRequirements requirements = requirementExtractor.extract(job);

        TailoredResumeResponse response = tailoringEngine.tailor(profile, job, requirements);

        log.info("Tailored resume generated successfully for Job ID: {} and Profile ID: {}", jobId, profile.getId());

        return response;
    }

}
