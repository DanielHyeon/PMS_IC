package com.insuretech.pms.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {
    private Long totalProjects;
    private Long activeProjects;
    private Long totalTasks;
    private Long completedTasks;
    private Integer avgProgress;
    private Map<String, Long> projectsByStatus;
    private Map<String, Long> tasksByStatus;
}