package com.insuretech.pms.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {
    private String columnId;
    private String phaseId;
    private String title;
    private String description;
    private String assigneeId;
    private String priority;
    private String status;
    private LocalDate dueDate;
    private Integer orderNum;
    private List<String> tags;
    private String trackType;
}
