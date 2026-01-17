package com.insuretech.pms.project.dto;

import com.insuretech.pms.project.entity.BacklogItem;

import java.time.LocalDateTime;

public class BacklogItemDto {
    private String id;
    private String backlogId;
    private String requirementId;
    private String originType;
    private String epicId;
    private String epicIdRef;
    private Integer priorityOrder;
    private String status;
    private Integer storyPoints;
    private Integer estimatedEffortHours;
    private String acceptanceCriteria;
    private String sprintId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    public BacklogItemDto() {
    }

    public BacklogItemDto(String id, String backlogId, String requirementId, String originType,
                         String epicId, String epicIdRef, Integer priorityOrder, String status,
                         Integer storyPoints, Integer estimatedEffortHours, String acceptanceCriteria,
                         String sprintId, LocalDateTime createdAt, LocalDateTime updatedAt, String createdBy, String updatedBy) {
        this.id = id;
        this.backlogId = backlogId;
        this.requirementId = requirementId;
        this.originType = originType;
        this.epicId = epicId;
        this.epicIdRef = epicIdRef;
        this.priorityOrder = priorityOrder;
        this.status = status;
        this.storyPoints = storyPoints;
        this.estimatedEffortHours = estimatedEffortHours;
        this.acceptanceCriteria = acceptanceCriteria;
        this.sprintId = sprintId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBacklogId() { return backlogId; }
    public void setBacklogId(String backlogId) { this.backlogId = backlogId; }

    public String getRequirementId() { return requirementId; }
    public void setRequirementId(String requirementId) { this.requirementId = requirementId; }

    public String getOriginType() { return originType; }
    public void setOriginType(String originType) { this.originType = originType; }

    public String getEpicId() { return epicId; }
    public void setEpicId(String epicId) { this.epicId = epicId; }

    public String getEpicIdRef() { return epicIdRef; }
    public void setEpicIdRef(String epicIdRef) { this.epicIdRef = epicIdRef; }

    public Integer getPriorityOrder() { return priorityOrder; }
    public void setPriorityOrder(Integer priorityOrder) { this.priorityOrder = priorityOrder; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getStoryPoints() { return storyPoints; }
    public void setStoryPoints(Integer storyPoints) { this.storyPoints = storyPoints; }

    public Integer getEstimatedEffortHours() { return estimatedEffortHours; }
    public void setEstimatedEffortHours(Integer estimatedEffortHours) { this.estimatedEffortHours = estimatedEffortHours; }

    public String getAcceptanceCriteria() { return acceptanceCriteria; }
    public void setAcceptanceCriteria(String acceptanceCriteria) { this.acceptanceCriteria = acceptanceCriteria; }

    public String getSprintId() { return sprintId; }
    public void setSprintId(String sprintId) { this.sprintId = sprintId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public static BacklogItemDto fromEntity(BacklogItem item) {
        BacklogItemDto dto = new BacklogItemDto();
        dto.setId(item.getId());
        dto.setBacklogId(item.getBacklog().getId());
        dto.setRequirementId(item.getRequirement() != null ? item.getRequirement().getId() : null);
        dto.setOriginType(item.getOriginType().name());
        dto.setEpicId(item.getEpicId());
        dto.setEpicIdRef(item.getEpic() != null ? item.getEpic().getId() : null);
        dto.setPriorityOrder(item.getPriorityOrder());
        dto.setStatus(item.getStatus().name());
        dto.setStoryPoints(item.getStoryPoints());
        dto.setEstimatedEffortHours(item.getEstimatedEffortHours());
        dto.setAcceptanceCriteria(item.getAcceptanceCriteria());
        dto.setSprintId(item.getSprintId());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        dto.setCreatedBy(item.getCreatedBy());
        dto.setUpdatedBy(item.getUpdatedBy());
        return dto;
    }
}
