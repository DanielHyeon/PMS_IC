package com.insuretech.pms.project.dto;

import com.insuretech.pms.project.entity.Issue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueDto {
    private String id;
    private String projectId;
    private String title;
    private String description;
    private String issueType;
    private String priority;
    private String status;
    private String assignee;
    private String reporter;
    private String reviewer;
    private LocalDate dueDate;
    private LocalDateTime resolvedAt;
    private String resolution;
    private String comments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static IssueDto from(Issue issue) {
        return IssueDto.builder()
                .id(issue.getId())
                .projectId(issue.getProject() != null ? issue.getProject().getId() : null)
                .title(issue.getTitle())
                .description(issue.getDescription())
                .issueType(issue.getIssueType() != null ? issue.getIssueType().name() : null)
                .priority(issue.getPriority() != null ? issue.getPriority().name() : null)
                .status(issue.getStatus() != null ? issue.getStatus().name() : null)
                .assignee(issue.getAssignee())
                .reporter(issue.getReporter())
                .reviewer(issue.getReviewer())
                .dueDate(issue.getDueDate())
                .resolvedAt(issue.getResolvedAt())
                .resolution(issue.getResolution())
                .comments(issue.getComments())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .build();
    }
}
