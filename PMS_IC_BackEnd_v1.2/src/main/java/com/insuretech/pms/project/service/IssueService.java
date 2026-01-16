package com.insuretech.pms.project.service;

import com.insuretech.pms.common.exception.CustomException;
import com.insuretech.pms.project.dto.IssueDto;
import com.insuretech.pms.project.entity.Issue;
import com.insuretech.pms.project.entity.Project;
import com.insuretech.pms.project.repository.IssueRepository;
import com.insuretech.pms.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<IssueDto> getIssuesByProject(String projectId) {
        ensureProjectExists(projectId);
        return issueRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(IssueDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IssueDto getIssueById(String issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> CustomException.notFound("이슈를 찾을 수 없습니다: " + issueId));
        return IssueDto.from(issue);
    }

    @Transactional
    public IssueDto createIssue(String projectId, IssueDto dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> CustomException.notFound("프로젝트를 찾을 수 없습니다: " + projectId));

        Issue issue = Issue.builder()
                .project(project)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .issueType(parseIssueType(dto.getIssueType()))
                .priority(parseIssuePriority(dto.getPriority()))
                .status(Issue.IssueStatus.OPEN)
                .assignee(dto.getAssignee())
                .reporter(dto.getReporter())
                .reviewer(dto.getReviewer())
                .dueDate(dto.getDueDate())
                .resolution(dto.getResolution())
                .comments(dto.getComments())
                .build();

        Issue saved = issueRepository.save(issue);
        log.info("Issue created: {}", saved.getId());
        return IssueDto.from(saved);
    }

    @Transactional
    public IssueDto updateIssue(String projectId, String issueId, IssueDto dto) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> CustomException.notFound("이슈를 찾을 수 없습니다: " + issueId));

        if (!issue.getProject().getId().equals(projectId)) {
            throw CustomException.badRequest("해당 프로젝트와 이슈 ID가 일치하지 않습니다.");
        }

        issue.setTitle(dto.getTitle());
        issue.setDescription(dto.getDescription());
        issue.setIssueType(parseIssueType(dto.getIssueType()));
        issue.setPriority(parseIssuePriority(dto.getPriority()));
        issue.setStatus(parseIssueStatus(dto.getStatus()));
        issue.setAssignee(dto.getAssignee());
        issue.setReporter(dto.getReporter());
        issue.setReviewer(dto.getReviewer());
        issue.setDueDate(dto.getDueDate());
        issue.setResolution(dto.getResolution());
        issue.setComments(dto.getComments());

        if (dto.getStatus() != null &&
            (dto.getStatus().equals("RESOLVED") || dto.getStatus().equals("CLOSED")) &&
            issue.getResolvedAt() == null) {
            issue.setResolvedAt(LocalDateTime.now());
        }

        Issue saved = issueRepository.save(issue);
        log.info("Issue updated: {}", saved.getId());
        return IssueDto.from(saved);
    }

    @Transactional
    public IssueDto updateIssueStatus(String projectId, String issueId, String status) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> CustomException.notFound("이슈를 찾을 수 없습니다: " + issueId));

        if (!issue.getProject().getId().equals(projectId)) {
            throw CustomException.badRequest("해당 프로젝트와 이슈 ID가 일치하지 않습니다.");
        }

        issue.setStatus(parseIssueStatus(status));

        if ((status.equals("RESOLVED") || status.equals("CLOSED")) && issue.getResolvedAt() == null) {
            issue.setResolvedAt(LocalDateTime.now());
        }

        Issue saved = issueRepository.save(issue);
        log.info("Issue status updated: {} -> {}", saved.getId(), status);
        return IssueDto.from(saved);
    }

    @Transactional
    public void deleteIssue(String projectId, String issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> CustomException.notFound("이슈를 찾을 수 없습니다: " + issueId));

        if (!issue.getProject().getId().equals(projectId)) {
            throw CustomException.badRequest("해당 프로젝트와 이슈 ID가 일치하지 않습니다.");
        }

        issueRepository.delete(issue);
        log.info("Issue deleted: {}", issueId);
    }

    private void ensureProjectExists(String projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw CustomException.notFound("프로젝트를 찾을 수 없습니다: " + projectId);
        }
    }

    private Issue.IssueType parseIssueType(String type) {
        if (type == null || type.isBlank()) {
            return Issue.IssueType.OTHER;
        }
        return Issue.IssueType.valueOf(type);
    }

    private Issue.IssuePriority parseIssuePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return Issue.IssuePriority.MEDIUM;
        }
        return Issue.IssuePriority.valueOf(priority);
    }

    private Issue.IssueStatus parseIssueStatus(String status) {
        if (status == null || status.isBlank()) {
            return Issue.IssueStatus.OPEN;
        }
        return Issue.IssueStatus.valueOf(status);
    }
}
