package com.insuretech.pms.project.dto;

import com.insuretech.pms.project.entity.Backlog;

import java.time.LocalDateTime;

public class BacklogDto {
    private String id;
    private String projectId;
    private String name;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    public BacklogDto() {
    }

    public BacklogDto(String id, String projectId, String name, String description, String status,
                     LocalDateTime createdAt, LocalDateTime updatedAt, String createdBy, String updatedBy) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public static BacklogDto fromEntity(Backlog backlog) {
        BacklogDto dto = new BacklogDto();
        dto.setId(backlog.getId());
        dto.setProjectId(backlog.getProjectId());
        dto.setName(backlog.getName());
        dto.setDescription(backlog.getDescription());
        dto.setStatus(backlog.getStatus().name());
        dto.setCreatedAt(backlog.getCreatedAt());
        dto.setUpdatedAt(backlog.getUpdatedAt());
        dto.setCreatedBy(backlog.getCreatedBy());
        dto.setUpdatedBy(backlog.getUpdatedBy());
        return dto;
    }
}
