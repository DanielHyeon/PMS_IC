package com.insuretech.pms.task.dto;

import com.insuretech.pms.task.entity.UserStory;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserStoryRequest {

    private String title;

    private String description;

    private UserStory.Priority priority;

    private Integer storyPoints;

    private UserStory.StoryStatus status;

    private String epic;

    private String assignee;

    private List<String> acceptanceCriteria;

    private String sprintId;
}
