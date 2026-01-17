package com.insuretech.pms.rfp.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateRequirementRequest {
    private String title;
    private String description;
    private String category;
    private String priority;
    private String status;
    private String acceptanceCriteria;
    private String assigneeId;
    private LocalDate dueDate;
    private Integer estimatedEffort;
    private Integer actualEffort;
    private Integer progress;
}
