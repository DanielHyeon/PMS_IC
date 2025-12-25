package com.insuretech.pms.task.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.insuretech.pms.task.entity.UserStory;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserStoryResponse {

    private String id;

    private String projectId;

    private String sprintId;

    private String title;

    private String description;

    private String acceptanceCriteria;

    private UserStory.Priority priority;

    private Integer storyPoints;

    private UserStory.StoryStatus status;

    private String assigneeId;

    private String epic;

    private List<String> acceptanceCriteriaList;

    private Integer priorityOrder;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public static UserStoryResponse fromEntity(UserStory story, List<String> acceptanceCriteriaList) {
        return UserStoryResponse.builder()
                .id(story.getId())
                .projectId(story.getProjectId())
                .sprintId(story.getSprint() != null ? story.getSprint().getId() : null)
                .title(story.getTitle())
                .description(story.getDescription())
                .acceptanceCriteria(story.getAcceptanceCriteria())
                .priority(story.getPriority())
                .storyPoints(story.getStoryPoints())
                .status(story.getStatus())
                .assigneeId(story.getAssigneeId())
                .epic(story.getEpic())
                .acceptanceCriteriaList(acceptanceCriteriaList)
                .priorityOrder(story.getPriorityOrder())
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .build();
    }
}
