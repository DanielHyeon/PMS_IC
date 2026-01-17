package com.insuretech.pms.rfp.dto;

import com.insuretech.pms.rfp.entity.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementDto {
    private String id;
    private String rfpId;
    private String projectId;
    private String code;
    private String title;
    private String description;
    private RequirementCategory category;
    private Priority priority;
    private RequirementStatus status;
    private Integer progress;
    private String sourceText;
    private Integer pageNumber;
    private String assigneeId;
    private LocalDate dueDate;
    private String acceptanceCriteria;
    private Integer estimatedEffort;
    private Integer actualEffort;

    // Phase 3: Progress tracking fields
    private Integer storyPoints;
    private Integer estimatedEffortHours;
    private Integer actualEffortHours;
    private Integer remainingEffortHours;
    private Integer progressPercentage;
    private LocalDateTime lastProgressUpdate;
    private Requirement.ProgressStage progressStage;
    private Requirement.ProgressCalculationMethod progressCalcMethod;

    private Set<String> linkedTaskIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RequirementDto fromEntity(Requirement req) {
        return RequirementDto.builder()
                .id(req.getId())
                .rfpId(req.getRfp() != null ? req.getRfp().getId() : null)
                .projectId(req.getProjectId())
                .code(req.getCode())
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory())
                .priority(req.getPriority())
                .status(req.getStatus())
                .progress(req.getProgress())
                .sourceText(req.getSourceText())
                .pageNumber(req.getPageNumber())
                .assigneeId(req.getAssigneeId())
                .dueDate(req.getDueDate())
                .acceptanceCriteria(req.getAcceptanceCriteria())
                .estimatedEffort(req.getEstimatedEffort())
                .actualEffort(req.getActualEffort())
                // Phase 3: Progress tracking fields
                .storyPoints(req.getStoryPoints())
                .estimatedEffortHours(req.getEstimatedEffortHours())
                .actualEffortHours(req.getActualEffortHours())
                .remainingEffortHours(req.getRemainingEffortHours())
                .progressPercentage(req.getProgressPercentage())
                .lastProgressUpdate(req.getLastProgressUpdate())
                .progressStage(req.getProgressStage())
                .progressCalcMethod(req.getProgressCalcMethod())
                .linkedTaskIds(req.getLinkedTaskIds())
                .createdAt(req.getCreatedAt())
                .updatedAt(req.getUpdatedAt())
                .build();
    }
}
