package com.insuretech.pms.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KanbanColumnResponse {
    private String id;
    private String name;
    private Integer orderNum;
    private Integer wipLimit;
    private String color;
    private List<TaskSummaryResponse> tasks;
}
