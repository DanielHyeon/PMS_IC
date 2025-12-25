package com.insuretech.pms.project.service;

import com.insuretech.pms.common.exception.CustomException;
import com.insuretech.pms.project.dto.ProjectDto;
import com.insuretech.pms.project.entity.Project;
import com.insuretech.pms.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Cacheable(value = "projects", key = "'all'")
    @Transactional(readOnly = true)
    public List<ProjectDto> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(ProjectDto::from)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "projects", key = "#id")
    @Transactional(readOnly = true)
    public ProjectDto getProjectById(String id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("프로젝트를 찾을 수 없습니다: " + id));
        return ProjectDto.from(project);
    }

    @CacheEvict(value = "projects", allEntries = true)
    @Transactional
    public ProjectDto createProject(ProjectDto dto) {
        Project project = Project.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .status(Project.ProjectStatus.valueOf(dto.getStatus()))
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .budget(dto.getBudget())
                .progress(0)
                .build();

        Project saved = projectRepository.save(project);
        log.info("Project created: {}", saved.getId());
        return ProjectDto.from(saved);
    }

    @CacheEvict(value = "projects", allEntries = true)
    @Transactional
    public ProjectDto updateProject(String id, ProjectDto dto) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("프로젝트를 찾을 수 없습니다: " + id));

        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setStatus(Project.ProjectStatus.valueOf(dto.getStatus()));
        project.setStartDate(dto.getStartDate());
        project.setEndDate(dto.getEndDate());
        project.setBudget(dto.getBudget());
        project.setProgress(dto.getProgress());

        Project updated = projectRepository.save(project);
        log.info("Project updated: {}", updated.getId());
        return ProjectDto.from(updated);
    }

    @CacheEvict(value = "projects", allEntries = true)
    @Transactional
    public void deleteProject(String id) {
        if (!projectRepository.existsById(id)) {
            throw CustomException.notFound("프로젝트를 찾을 수 없습니다: " + id);
        }
        projectRepository.deleteById(id);
        log.info("Project deleted: {}", id);
    }
}