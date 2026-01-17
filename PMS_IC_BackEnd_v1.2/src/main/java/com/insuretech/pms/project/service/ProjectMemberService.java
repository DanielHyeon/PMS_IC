package com.insuretech.pms.project.service;

import com.insuretech.pms.common.exception.CustomException;
import com.insuretech.pms.project.dto.*;
import com.insuretech.pms.project.entity.Project;
import com.insuretech.pms.project.entity.ProjectMember;
import com.insuretech.pms.project.repository.ProjectMemberRepository;
import com.insuretech.pms.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<ProjectMemberDto> getProjectMembers(String projectId) {
        validateProjectExists(projectId);
        return projectMemberRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(ProjectMemberDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectMemberDto getProjectMember(String projectId, String memberId) {
        ProjectMember member = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> CustomException.notFound("Project member not found: " + memberId));

        if (!member.getProject().getId().equals(projectId)) {
            throw CustomException.badRequest("Member does not belong to project: " + projectId);
        }

        return ProjectMemberDto.from(member);
    }

    @Transactional
    public ProjectMemberDto addProjectMember(String projectId, AddProjectMemberRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> CustomException.notFound("Project not found: " + projectId));

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, request.getUserId())) {
            throw CustomException.badRequest("User is already a member of this project");
        }

        ProjectMember member = ProjectMember.builder()
                .id("pm-" + UUID.randomUUID().toString().substring(0, 8))
                .project(project)
                .userId(request.getUserId())
                .userName(request.getUserName())
                .userEmail(request.getUserEmail())
                .role(ProjectMember.ProjectRole.valueOf(request.getRole()))
                .department(request.getDepartment())
                .build();

        ProjectMember saved = projectMemberRepository.save(member);
        log.info("Member added to project {}: {} with role {}", projectId, request.getUserId(), request.getRole());
        return ProjectMemberDto.from(saved);
    }

    @Transactional
    public ProjectMemberDto updateProjectMember(String projectId, String memberId, UpdateProjectMemberRequest request) {
        ProjectMember member = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> CustomException.notFound("Project member not found: " + memberId));

        if (!member.getProject().getId().equals(projectId)) {
            throw CustomException.badRequest("Member does not belong to project: " + projectId);
        }

        if (request.getRole() != null) {
            member.setRole(ProjectMember.ProjectRole.valueOf(request.getRole()));
        }
        if (request.getDepartment() != null) {
            member.setDepartment(request.getDepartment());
        }

        ProjectMember updated = projectMemberRepository.save(member);
        log.info("Project member updated: {} in project {}", memberId, projectId);
        return ProjectMemberDto.from(updated);
    }

    @Transactional
    public void removeProjectMember(String projectId, String memberId) {
        ProjectMember member = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> CustomException.notFound("Project member not found: " + memberId));

        if (!member.getProject().getId().equals(projectId)) {
            throw CustomException.badRequest("Member does not belong to project: " + projectId);
        }

        projectMemberRepository.delete(member);
        log.info("Member removed from project {}: {}", projectId, memberId);
    }

    private void validateProjectExists(String projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw CustomException.notFound("Project not found: " + projectId);
        }
    }
}
