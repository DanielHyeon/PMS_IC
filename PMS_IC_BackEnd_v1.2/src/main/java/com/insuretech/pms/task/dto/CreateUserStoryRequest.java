package com.insuretech.pms.task.dto;

import com.insuretech.pms.task.entity.UserStory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateUserStoryRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Priority is required")
    private UserStory.Priority priority;

    private Integer storyPoints;

    @NotBlank(message = "Epic is required")
    private String epic;

    private String assignee;

    private List<String> acceptanceCriteria;

    @NotBlank(message = "Project ID is required")
    private String projectId;
}
