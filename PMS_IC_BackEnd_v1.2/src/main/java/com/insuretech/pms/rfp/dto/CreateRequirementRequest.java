package com.insuretech.pms.rfp.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateRequirementRequest {
    private String rfpId;
    private String title;
    private String description;
    private String category;
    private String priority;
    private String status;
    private String acceptanceCriteria;
    private String assigneeId;
    private LocalDate dueDate;
    private Integer estimatedEffort;
}
