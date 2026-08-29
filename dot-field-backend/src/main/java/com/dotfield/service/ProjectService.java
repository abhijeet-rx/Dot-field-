package com.dotfield.service;

import com.dotfield.dto.ProjectRequest;
import com.dotfield.dto.ProjectResponse;
import com.dotfield.entity.Profile;
import com.dotfield.entity.Project;
import com.dotfield.exception.ResourceNotFoundException;
import com.dotfield.mapper.ProfileMapper;
import com.dotfield.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProfileService profileService;
    private final ProjectRepository projectRepository;
    private final ProfileMapper profileMapper;

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjects() {
        Profile profile = profileService.getPrimaryProfileOrThrow();
        return projectRepository.findByProfileId(profile.getId()).stream()
                .map(profileMapper::toProjectResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectResponse addProject(ProjectRequest request) {
        Profile profile = profileService.getPrimaryProfileOrThrow();

        Project project = profileMapper.toProjectEntity(request);
        profile.addProject(project);

        Project saved = projectRepository.save(project);
        log.info("Added project record ID: {}", saved.getId());

        return profileMapper.toProjectResponse(saved);
    }

    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        Profile profile = profileService.getPrimaryProfileOrThrow();
        Project project = projectRepository.findByIdAndProfileId(id, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setGithubUrl(request.getGithubUrl());
        project.setLiveUrl(request.getLiveUrl());
        project.setTechnologies(request.getTechnologies() != null
                ? new ArrayList<>(request.getTechnologies())
                : new ArrayList<>());

        Project saved = projectRepository.save(project);
        log.info("Updated project record ID: {}", saved.getId());

        return profileMapper.toProjectResponse(saved);
    }

    @Transactional
    public void deleteProject(Long id) {
        Profile profile = profileService.getPrimaryProfileOrThrow();
        Project project = projectRepository.findByIdAndProfileId(id, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));

        profile.removeProject(project);
        projectRepository.delete(project);
        log.info("Deleted project record ID: {}", id);
    }

}
