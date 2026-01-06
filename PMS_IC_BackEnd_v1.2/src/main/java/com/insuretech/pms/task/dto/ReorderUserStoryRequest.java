package com.insuretech.pms.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReorderUserStoryRequest {

    @NotBlank(message = "Story ID is required")
    private String storyId;

    @NotNull(message = "New priority order is required")
    private Integer newPriorityOrder;
}
